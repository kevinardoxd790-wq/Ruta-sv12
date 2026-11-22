package com.example.map

import android.content.Context
import com.example.location.OfflineLocationTracker
import com.graphhopper.GHRequest
import com.graphhopper.GraphHopper
import com.graphhopper.config.CHProfile
import com.graphhopper.config.Profile
import com.graphhopper.routing.util.EdgeFilter
import com.graphhopper.util.Instruction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.mapsforge.core.model.LatLong
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipInputStream

enum class RouteProfile(val ghName: String, val label: String) {
    CAR("car", "Carro / Moto"),
    FOOT("foot", "A pie")
}

data class RouteStep(
    val index: Int,
    val sign: Int,
    val instructionText: String,
    val streetName: String,
    val distanceMeters: Double,
    val timeMillis: Long,
    val lat: Double,
    val lon: Double
)

data class OfflineRouteResult(
    val profile: RouteProfile,
    val origin: LatLong,
    val destination: LatLong,
    val routePoints: List<LatLong>,
    val offRoadTailPoints: List<LatLong>,
    val offRoadTailMeters: Double,
    val totalDistanceMeters: Double,
    val totalTimeMillis: Long,
    val steps: List<RouteStep>
)

sealed class OfflineInitState {
    data class Preparing(val stepLabel: String, val progress: Float) : OfflineInitState()
    data class Ready(val mapFile: File, val mapSizeMb: Double) : OfflineInitState()
    data class Error(val message: String) : OfflineInitState()
}

class OfflineMapDataManager(private val context: Context) {

    private val mutex = Mutex()
    private var hopper: GraphHopper? = null
    private var readyMapFile: File? = null

    suspend fun initialize(onProgress: (OfflineInitState.Preparing) -> Unit): OfflineInitState =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                if (readyMapFile != null && hopper != null) {
                    val f = readyMapFile!!
                    return@withLock OfflineInitState.Ready(
                        mapFile = f,
                        mapSizeMb = f.length() / (1024.0 * 1024.0)
                    )
                }

                try {
                    val offlineDir = File(context.filesDir, "offline_sv")
                    offlineDir.mkdirs()

                    // 1. Download or copy Mapsforge vector map of El Salvador
                    val mapFile = File(offlineDir, "el-salvador.map")
                    if (!mapFile.exists() || mapFile.length() < 100_000L) {
                        onProgress(
                            OfflineInitState.Preparing(
                                "Descargando mapa offline de El Salvador…",
                                0.10f
                            )
                        )
                        val tmpMap = File(offlineDir, "el-salvador.map.tmp")
                        try {
                            downloadFileWithProgress(
                                "https://raw.githubusercontent.com/mabeldandoval/rutasv/main/app/src/main/assets/el-salvador.map",
                                tmpMap
                            ) { pct ->
                                onProgress(
                                    OfflineInitState.Preparing(
                                        "Descargando mapa (${(pct * 100).toInt()}%)...",
                                        0.10f + pct * 0.30f
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            context.assets.open("el-salvador.map").use { input ->
                                FileOutputStream(tmpMap).use { output ->
                                    input.copyTo(output, bufferSize = 65536)
                                }
                            }
                        }
                        if (mapFile.exists()) mapFile.delete()
                        tmpMap.renameTo(mapFile)
                    }

                    // 2. Download or extract GraphHopper routing graph for El Salvador
                    val ghDir = File(offlineDir, "el-salvador-gh")
                    val markerFile = File(ghDir, ".sv_graph_ready_gh53")
                    val ghZipFile = File(offlineDir, "el-salvador-gh53.zip")
                    if (!markerFile.exists()) {
                        onProgress(
                            OfflineInitState.Preparing(
                                "Descargando red de carreteras y rutas…",
                                0.40f
                            )
                        )
                        if (!ghZipFile.exists() || ghZipFile.length() < 100_000L) {
                            try {
                                downloadFileWithProgress(
                                    "https://raw.githubusercontent.com/mabeldandoval/rutasv/main/app/src/main/assets/el-salvador-gh53.zip",
                                    ghZipFile
                                ) { pct ->
                                    onProgress(
                                        OfflineInitState.Preparing(
                                            "Descargando rutas (${(pct * 100).toInt()}%)...",
                                            0.40f + pct * 0.35f
                                        )
                                    )
                                }
                            } catch (e: Exception) {
                                context.assets.open("el-salvador-gh53.zip").use { input ->
                                    FileOutputStream(ghZipFile).use { output ->
                                        input.copyTo(output, bufferSize = 65536)
                                    }
                                }
                            }
                        }

                        onProgress(
                            OfflineInitState.Preparing(
                                "Descomprimiendo red de carreteras y caminos rurales…",
                                0.78f
                            )
                        )
                        if (ghDir.exists()) ghDir.deleteRecursively()
                        ghDir.mkdirs()

                        ZipInputStream(BufferedInputStream(ghZipFile.inputStream())).use { zis ->
                            val buf = ByteArray(65536)
                            var entry = zis.nextEntry
                            while (entry != null) {
                                val outFile = File(ghDir, entry.name)
                                // Guard against Zip Slip
                                if (!outFile.canonicalPath.startsWith(ghDir.canonicalPath + File.separator)) {
                                    throw SecurityException("Invalid zip entry: ${entry.name}")
                                }
                                if (entry.isDirectory) {
                                    outFile.mkdirs()
                                } else {
                                    outFile.parentFile?.mkdirs()
                                    FileOutputStream(outFile).use { fos ->
                                        var count = zis.read(buf)
                                        while (count != -1) {
                                            fos.write(buf, 0, count)
                                            count = zis.read(buf)
                                        }
                                    }
                                }
                                zis.closeEntry()
                                entry = zis.nextEntry
                            }
                        }
                        markerFile.createNewFile()
                    }

                    // 3. Load GraphHopper in memory-mapped / fast CH mode
                    onProgress(
                        OfflineInitState.Preparing(
                            "Cargando motor de rutas sin internet…",
                            0.80f
                        )
                    )
                    val gh = GraphHopper().apply {
                        graphHopperLocation = ghDir.absolutePath
                        setProfiles(
                            Profile("car").setVehicle("car").setWeighting("fastest"),
                            Profile("foot").setVehicle("foot").setWeighting("fastest")
                        )
                        chPreparationHandler.setCHProfiles(
                            CHProfile("car"),
                            CHProfile("foot")
                        )
                    }
                    gh.importOrLoad()
                    hopper = gh
                    readyMapFile = mapFile

                    OfflineInitState.Ready(
                        mapFile = mapFile,
                        mapSizeMb = mapFile.length() / (1024.0 * 1024.0)
                    )
                } catch (e: Exception) {
                    OfflineInitState.Error(
                        e.localizedMessage ?: "No se pudo preparar el mapa offline de El Salvador."
                    )
                }
            }
        }

    suspend fun findNearestRoadName(lat: Double, lon: Double): String? =
        withContext(Dispatchers.IO) {
            val gh = hopper ?: return@withContext null
            runCatching {
                val snap = gh.locationIndex.findClosest(lat, lon, EdgeFilter.ALL_EDGES)
                if (snap.isValid) {
                    snap.closestEdge.name?.takeIf { it.isNotBlank() }
                } else {
                    null
                }
            }.getOrNull()
        }

    suspend fun calculateRoute(
        startLat: Double,
        startLon: Double,
        endLat: Double,
        endLon: Double,
        profile: RouteProfile
    ): Result<OfflineRouteResult> = withContext(Dispatchers.IO) {
        val gh = hopper ?: return@withContext Result.failure(
            IllegalStateException("El motor de rutas todavía se está cargando.")
        )

        runCatching {
            val req = GHRequest(startLat, startLon, endLat, endLon)
                .setProfile(profile.ghName)
                .setLocale(Locale.forLanguageTag("es"))
            val rsp = gh.route(req)
            if (rsp.hasErrors()) {
                val errMsg = rsp.errors.firstOrNull()?.message ?: "No se encontró ruta entre estos puntos."
                throw IllegalStateException(
                    "No se encontró ruta transitable cerca de alguno de los puntos ($errMsg)."
                )
            }

            val best = rsp.best
            val pointList = best.points
            val routePoints = ArrayList<LatLong>(pointList.size())
            for (i in 0 until pointList.size()) {
                routePoints.add(LatLong(pointList.getLat(i), pointList.getLon(i)))
            }

            // Check if destination pin is off-road (e.g. inside a farm/rural terrain)
            val lastRouted = routePoints.lastOrNull() ?: LatLong(endLat, endLon)
            val tailMeters = OfflineLocationTracker.haversineMeters(
                lastRouted.latitude,
                lastRouted.longitude,
                endLat,
                endLon
            )
            val offRoadTail = if (tailMeters > 25.0) {
                listOf(lastRouted, LatLong(endLat, endLon))
            } else {
                emptyList()
            }

            val steps = best.instructions.mapIndexed { idx, instr ->
                val pLat = if (instr.points.size() > 0) instr.points.getLat(0) else endLat
                val pLon = if (instr.points.size() > 0) instr.points.getLon(0) else endLon
                RouteStep(
                    index = idx,
                    sign = instr.sign,
                    instructionText = formatSpanishInstruction(instr),
                    streetName = instr.name.orEmpty(),
                    distanceMeters = instr.distance,
                    timeMillis = instr.time,
                    lat = pLat,
                    lon = pLon
                )
            }

            OfflineRouteResult(
                profile = profile,
                origin = LatLong(startLat, startLon),
                destination = LatLong(endLat, endLon),
                routePoints = routePoints,
                offRoadTailPoints = offRoadTail,
                offRoadTailMeters = if (tailMeters > 25.0) tailMeters else 0.0,
                totalDistanceMeters = best.distance + if (tailMeters > 25.0) tailMeters else 0.0,
                totalTimeMillis = best.time,
                steps = steps
            )
        }
    }

    fun close() {
        runCatching { hopper?.close() }
        hopper = null
    }

    private fun formatSpanishInstruction(instr: Instruction): String {
        val street = instr.name?.takeIf { it.isNotBlank() }
        val onStreet = if (street != null) " por $street" else ""
        val ontoStreet = if (street != null) " hacia $street" else ""
        return when (instr.sign) {
            Instruction.CONTINUE_ON_STREET -> if (street != null) "Continúa por $street" else "Continúa recto"
            Instruction.TURN_SLIGHT_LEFT -> "Gira levemente a la izquierda$ontoStreet"
            Instruction.TURN_LEFT -> "Gira a la izquierda$ontoStreet"
            Instruction.TURN_SHARP_LEFT -> "Gira cerrado a la izquierda$ontoStreet"
            Instruction.TURN_SLIGHT_RIGHT -> "Gira levemente a la derecha$ontoStreet"
            Instruction.TURN_RIGHT -> "Gira a la derecha$ontoStreet"
            Instruction.TURN_SHARP_RIGHT -> "Gira cerrado a la derecha$ontoStreet"
            Instruction.KEEP_LEFT -> "Mantente a la izquierda$onStreet"
            Instruction.KEEP_RIGHT -> "Mantente a la derecha$onStreet"
            Instruction.USE_ROUNDABOUT -> "Entra en la rotonda$ontoStreet"
            Instruction.LEAVE_ROUNDABOUT -> "Sal de la rotonda$ontoStreet"
            Instruction.U_TURN_LEFT, Instruction.U_TURN_RIGHT, Instruction.U_TURN_UNKNOWN ->
                "Da vuelta en U$onStreet"
            Instruction.REACHED_VIA -> "Pasaste por el punto intermedio"
            Instruction.FINISH -> "Llegaste al punto más cercano al destino"
            else -> if (street != null) "Sigue$onStreet" else "Continúa por la vía"
        }
    }

    private fun downloadFileWithProgress(urlString: String, outputFile: File, onProgress: (Float) -> Unit) {
        val url = java.net.URL(urlString)
        val connection = url.openConnection() as java.net.HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 30000
        connection.connect()
        val length = connection.contentLength.toLong()
        BufferedInputStream(connection.inputStream).use { input ->
            FileOutputStream(outputFile).use { output ->
                val data = ByteArray(8192)
                var total: Long = 0
                var count: Int
                while (input.read(data).also { count = it } != -1) {
                    total += count.toLong()
                    output.write(data, 0, count)
                    if (length > 0) {
                        onProgress(total.toFloat() / length.toFloat())
                    }
                }
                output.flush()
            }
        }
        connection.disconnect()
    }
}
