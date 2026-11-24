package com.example.map

import com.example.data.ElSalvadorPlaces
import com.example.data.local.OfflinePlaceFtsEntity
import com.graphhopper.GraphHopper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Tag
import org.mapsforge.core.model.Tile
import org.mapsforge.core.util.MercatorProjection
import org.mapsforge.map.reader.MapFile
import java.io.File

object OfflinePlaceIndexer {

    private const val INDEX_ZOOM: Byte = 14
    private const val TILE_SIZE = 256
    private const val CHUNK_TILES = 8
    private const val BATCH_SIZE = 500

    suspend fun indexFromOfflineAssets(
        mapFile: File?,
        hopper: GraphHopper?,
        onProgress: (Float, Int) -> Unit,
        onBatch: suspend (List<OfflinePlaceFtsEntity>) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val seenKeys = HashSet<String>(65_536)
        val batch = ArrayList<OfflinePlaceFtsEntity>(BATCH_SIZE)
        var totalInserted = 0

        suspend fun emitItem(
            name: String,
            category: String,
            subtitle: String,
            lat: Double,
            lon: Double,
            rankPriority: Int,
            cellScale: Double
        ) {
            val cleanName = name.trim()
            if (cleanName.length < 2) return
            val normalized = OfflinePlaceFtsEntity.normalizeText(cleanName)
            if (normalized.length < 2) return
            val latCell = (lat * cellScale).toInt()
            val lonCell = (lon * cellScale).toInt()
            val key = "$normalized|$latCell|$lonCell"
            if (!seenKeys.add(key)) return

            batch.add(
                OfflinePlaceFtsEntity(
                    name = cleanName,
                    normalizedName = normalized,
                    category = category,
                    subtitle = subtitle,
                    latitude = lat,
                    longitude = lon,
                    rankPriority = rankPriority
                )
            )
            if (batch.size >= BATCH_SIZE) {
                val copy = ArrayList(batch)
                batch.clear()
                onBatch(copy)
                totalInserted += copy.size
            }
        }

        // 1. High-priority curated El Salvador places
        for (place in ElSalvadorPlaces.all) {
            emitItem(
                name = place.name,
                category = place.type,
                subtitle = place.department,
                lat = place.latitude,
                lon = place.longitude,
                rankPriority = 5,
                cellScale = 100.0
            )
        }

        // 2. POIs & named buildings/areas from el-salvador.map
        if (mapFile != null && mapFile.exists()) {
            var mapReader: MapFile? = null
            try {
                mapReader = MapFile(mapFile)
                val bbox = mapReader.boundingBox()
                val minLat = maxOf(bbox.minLatitude, 13.05)
                val maxLat = minOf(bbox.maxLatitude, 14.50)
                val minLon = maxOf(bbox.minLongitude, -90.20)
                val maxLon = minOf(bbox.maxLongitude, -87.60)

                val minX = MercatorProjection.longitudeToTileX(minLon, INDEX_ZOOM)
                val maxX = MercatorProjection.longitudeToTileX(maxLon, INDEX_ZOOM)
                val minY = MercatorProjection.latitudeToTileY(maxLat, INDEX_ZOOM)
                val maxY = MercatorProjection.latitudeToTileY(minLat, INDEX_ZOOM)

                val totalChunksX = ((maxX - minX) / CHUNK_TILES) + 1
                val totalChunksY = ((maxY - minY) / CHUNK_TILES) + 1
                val totalChunks = (totalChunksX * totalChunksY).coerceAtLeast(1)
                var processedChunks = 0

                var x = minX
                while (x <= maxX) {
                    val xEnd = (x + CHUNK_TILES - 1).coerceAtMost(maxX)
                    var y = minY
                    while (y <= maxY) {
                        val yEnd = (y + CHUNK_TILES - 1).coerceAtMost(maxY)
                        val ul = Tile(x, y, INDEX_ZOOM, TILE_SIZE)
                        val lr = Tile(xEnd, yEnd, INDEX_ZOOM, TILE_SIZE)

                        val mapData = runCatching { mapReader.readNamedItems(ul, lr) }.getOrNull()
                        if (mapData != null) {
                            for (poi in mapData.pois) {
                                val classified = classifyTags(poi.tags, isWay = false) ?: continue
                                emitItem(
                                    name = classified.name,
                                    category = classified.category,
                                    subtitle = classified.subtitle,
                                    lat = poi.position.latitude,
                                    lon = poi.position.longitude,
                                    rankPriority = classified.rankPriority,
                                    cellScale = 150.0
                                )
                            }
                            for (way in mapData.ways) {
                                val classified = classifyTags(way.tags, isWay = true) ?: continue
                                val pos = resolveWayCenter(way.labelPosition, way.latLongs) ?: continue
                                emitItem(
                                    name = classified.name,
                                    category = classified.category,
                                    subtitle = classified.subtitle,
                                    lat = pos.latitude,
                                    lon = pos.longitude,
                                    rankPriority = classified.rankPriority,
                                    cellScale = 120.0
                                )
                            }
                        }

                        processedChunks++
                        if (processedChunks % 10 == 0) {
                            onProgress((processedChunks.toFloat() / totalChunks.toFloat()) * 0.75f, totalInserted)
                        }
                        y += CHUNK_TILES
                    }
                    x += CHUNK_TILES
                }
            } finally {
                runCatching { mapReader?.close() }
            }
        }

        // 3. Road & street names from GraphHopper routing graph
        if (hopper != null) {
            runCatching {
                val graph = hopper.graphHopperStorage.baseGraph
                val nodeAccess = graph.nodeAccess
                val iter = graph.allEdges
                while (iter.next()) {
                    val streetName = iter.name?.trim() ?: continue
                    if (streetName.length < 2) continue
                    val lat = (nodeAccess.getLat(iter.baseNode) + nodeAccess.getLat(iter.adjNode)) * 0.5
                    val lon = (nodeAccess.getLon(iter.baseNode) + nodeAccess.getLon(iter.adjNode)) * 0.5
                    emitItem(
                        name = streetName,
                        category = "Calle / Vía",
                        subtitle = "Red vial de El Salvador",
                        lat = lat,
                        lon = lon,
                        rankPriority = 1,
                        cellScale = 80.0
                    )
                }
            }
        }

        if (batch.isNotEmpty()) {
            onBatch(ArrayList(batch))
            totalInserted += batch.size
            batch.clear()
        }

        onProgress(1f, totalInserted)
        totalInserted
    }

    private fun resolveWayCenter(labelPos: LatLong?, latLongs: Array<Array<LatLong>>?): LatLong? {
        if (labelPos != null) return labelPos
        val ring = latLongs?.firstOrNull() ?: return null
        if (ring.isEmpty()) return null
        var sumLat = 0.0
        var sumLon = 0.0
        for (pt in ring) {
            sumLat += pt.latitude
            sumLon += pt.longitude
        }
        return LatLong(sumLat / ring.size, sumLon / ring.size)
    }

    private data class ClassifiedOsmItem(
        val name: String,
        val category: String,
        val subtitle: String,
        val rankPriority: Int
    )

    private fun classifyTags(tags: List<Tag>?, isWay: Boolean): ClassifiedOsmItem? {
        if (tags.isNullOrEmpty()) return null
        var name: String? = null
        var amenity: String? = null
        var shop: String? = null
        var building: String? = null
        var office: String? = null
        var tourism: String? = null
        var leisure: String? = null
        var place: String? = null
        var highway: String? = null
        var healthcare: String? = null
        var addrCity: String? = null
        var addrStreet: String? = null

        for (tag in tags) {
            when (tag.key) {
                "name" -> if (name == null) name = tag.value
                "name:es" -> name = tag.value
                "amenity" -> amenity = tag.value
                "shop" -> shop = tag.value
                "building" -> building = tag.value
                "office" -> office = tag.value
                "tourism" -> tourism = tag.value
                "leisure" -> leisure = tag.value
                "place" -> place = tag.value
                "highway" -> highway = tag.value
                "healthcare" -> healthcare = tag.value
                "addr:city" -> addrCity = tag.value
                "addr:street" -> addrStreet = tag.value
            }
        }

        val cleanName = name?.trim()?.takeIf { it.length >= 2 } ?: return null

        val (category, priority) = when {
            place != null -> when (place) {
                "city", "town" -> "Ciudad" to 4
                "village", "hamlet" -> "Localidad / Cantón" to 3
                "suburb", "neighbourhood", "quarter" -> "Colonia / Barrio" to 3
                else -> "Zona / Lugar" to 3
            }
            amenity != null -> when (amenity) {
                "hospital", "clinic", "doctors", "pharmacy" -> "Salud" to 4
                "fuel" -> "Gasolinera" to 3
                "restaurant", "cafe", "fast_food", "food_court" -> "Comida / Café" to 3
                "school", "university", "college" -> "Educación" to 3
                "bank", "atm" -> "Banco" to 3
                "police", "fire_station", "townhall" -> "Servicio público" to 3
                "bus_station", "parking" -> "Transporte" to 3
                "place_of_worship" -> "Iglesia" to 2
                else -> "Punto de interés" to 3
            }
            healthcare != null -> "Salud" to 4
            shop != null -> when (shop) {
                "mall", "department_store", "supermarket" -> "Centro comercial / Súper" to 4
                else -> "Comercio" to 3
            }
            tourism != null -> "Turismo / Hotel" to 4
            leisure != null -> "Parque / Recreación" to 3
            office != null -> "Edificio / Oficinas" to 4
            building != null -> "Edificio / Lugar" to 4
            highway != null -> "Calle / Vía" to if (isWay) 1 else 2
            else -> "Lugar" to 2
        }

        val subtitle = listOfNotNull(
            addrStreet?.takeIf { it.isNotBlank() },
            addrCity?.takeIf { it.isNotBlank() }
        ).joinToString(", ").ifEmpty { "Mapa offline de El Salvador" }

        return ClassifiedOsmItem(
            name = cleanName,
            category = category,
            subtitle = subtitle,
            rankPriority = priority
        )
    }
}
