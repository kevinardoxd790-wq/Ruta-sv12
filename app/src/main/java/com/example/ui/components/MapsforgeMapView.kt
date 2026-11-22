package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.ElSalvadorPlaces
import com.example.data.local.SavedPlaceEntity
import com.example.location.GpsPosition
import com.example.map.OfflineRouteResult
import com.example.ui.MapFocusTarget
import com.example.ui.SelectedPoint
import org.mapsforge.core.graphics.Style
import org.mapsforge.core.model.LatLong
import org.mapsforge.core.model.Point
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.Layer
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.overlay.Circle
import org.mapsforge.map.layer.overlay.Marker
import org.mapsforge.map.layer.overlay.Polyline
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.internal.MapsforgeThemes
import java.io.File

@Composable
fun MapsforgeMapView(
    mapFile: File,
    isNightMode: Boolean,
    gpsPosition: GpsPosition?,
    manualOrigin: SelectedPoint?,
    destination: SelectedPoint?,
    routeResult: OfflineRouteResult?,
    savedPlaces: List<SavedPlaceEntity>,
    focusTarget: MapFocusTarget?,
    zoomCommandNonce: Int,
    zoomDelta: Int,
    onLongPressCoordinate: (Double, Double) -> Unit,
    onMapCenterChanged: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val latestLongPress by rememberUpdatedState(onLongPressCoordinate)
    val latestCenterChanged by rememberUpdatedState(onMapCenterChanged)

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var tileRendererRef by remember { mutableStateOf<TileRendererLayer?>(null) }
    var tileCacheRef by remember { mutableStateOf<TileCache?>(null) }
    val overlayLayers = remember { mutableListOf<Layer>() }

    val markerBitmaps = remember(context) {
        MarkerBitmaps(
            destination = createPinBitmap(context, 0xFFD97706.toInt(), 0xFFFFFFFF.toInt()),
            origin = createPinBitmap(context, 0xFF0D47A1.toInt(), 0xFFFFFFFF.toInt()),
            gps = createGpsDotBitmap(context),
            saved = createSavedDotBitmap(context)
        )
    }

    DisposableEffect(mapFile) {
        onDispose {
            runCatching {
                tileRendererRef?.onDestroy()
                tileCacheRef?.destroy()
                mapViewRef?.destroyAll()
            }
        }
    }

    // Update night driving filter when night mode toggles
    LaunchedEffect(isNightMode, mapViewRef) {
        val mv = mapViewRef ?: return@LaunchedEffect
        if (isNightMode) {
            // Invert brightness while preserving hues for comfortable night driving
            val nightMatrix = ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 235f,
                    0f, -1f, 0f, 0f, 240f,
                    0f, 0f, -1f, 0f, 250f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            val paint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(nightMatrix)
            }
            mv.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
        } else {
            mv.setLayerType(View.LAYER_TYPE_NONE, null)
        }
    }

    // Center/zoom when requested
    LaunchedEffect(focusTarget?.nonce) {
        val target = focusTarget ?: return@LaunchedEffect
        val mv = mapViewRef ?: return@LaunchedEffect
        mv.model.mapViewPosition.setCenter(LatLong(target.latitude, target.longitude))
        target.zoom?.let { z ->
            mv.model.mapViewPosition.setZoomLevel(z)
        }
        latestCenterChanged(target.latitude, target.longitude)
    }

    // Zoom buttons
    LaunchedEffect(zoomCommandNonce) {
        if (zoomCommandNonce == 0) return@LaunchedEffect
        val mv = mapViewRef ?: return@LaunchedEffect
        val cur = mv.model.mapViewPosition.zoomLevel.toInt()
        val next = (cur + zoomDelta).coerceIn(6, 20).toByte()
        mv.model.mapViewPosition.setZoomLevel(next)
    }

    // Redraw overlays whenever route/markers change
    LaunchedEffect(
        mapViewRef,
        gpsPosition,
        manualOrigin,
        destination,
        routeResult,
        savedPlaces,
        isNightMode
    ) {
        val mv = mapViewRef ?: return@LaunchedEffect
        val layers = mv.layerManager.layers

        // Remove only previous overlays, keeping base TileRendererLayer intact
        overlayLayers.forEach { layers.remove(it) }
        overlayLayers.clear()

        fun addOverlay(layer: Layer) {
            overlayLayers.add(layer)
            layers.add(layer)
        }

        // 1. Saved places (subtle teal bookmarks)
        for (place in savedPlaces) {
            val m = Marker(
                LatLong(place.latitude, place.longitude),
                markerBitmaps.saved,
                0,
                0
            )
            addOverlay(m)
        }

        // 2. Main road route Polyline
        if (routeResult != null && routeResult.routePoints.size >= 2) {
            val outlinePaint = AndroidGraphicFactory.INSTANCE.createPaint().apply {
                color = if (isNightMode) 0xFF0F172A.toInt() else 0xFFFFFFFF.toInt()
                strokeWidth = 16f
                setStyle(Style.STROKE)
            }
            val outline = Polyline(outlinePaint, AndroidGraphicFactory.INSTANCE).apply {
                setPoints(routeResult.routePoints)
            }
            addOverlay(outline)

            val routePaint = AndroidGraphicFactory.INSTANCE.createPaint().apply {
                color = if (isNightMode) 0xFF5EEAD4.toInt() else 0xFF0D47A1.toInt()
                strokeWidth = 10f
                setStyle(Style.STROKE)
            }
            val routeLine = Polyline(routePaint, AndroidGraphicFactory.INSTANCE).apply {
                setPoints(routeResult.routePoints)
            }
            addOverlay(routeLine)

            // Off-road dashed/amber connector if pin is inside a finca/rural terrain
            if (routeResult.offRoadTailPoints.size >= 2) {
                val tailPaint = AndroidGraphicFactory.INSTANCE.createPaint().apply {
                    color = 0xFFD97706.toInt()
                    strokeWidth = 8f
                    setStyle(Style.STROKE)
                    setDashPathEffect(floatArrayOf(18f, 14f))
                }
                val tailLine = Polyline(tailPaint, AndroidGraphicFactory.INSTANCE).apply {
                    setPoints(routeResult.offRoadTailPoints)
                }
                addOverlay(tailLine)
            }
        }

        // 3. GPS Position accuracy circle & dot
        if (gpsPosition != null) {
            val gpsLatLong = LatLong(gpsPosition.latitude, gpsPosition.longitude)
            val fillPaint = AndroidGraphicFactory.INSTANCE.createPaint().apply {
                color = 0x301E88E5
                setStyle(Style.FILL)
            }
            val strokePaint = AndroidGraphicFactory.INSTANCE.createPaint().apply {
                color = 0x991565C0.toInt()
                strokeWidth = 3f
                setStyle(Style.STROKE)
            }
            val circle = Circle(
                gpsLatLong,
                gpsPosition.accuracyMeters.coerceIn(12f, 150f),
                fillPaint,
                strokePaint
            )
            addOverlay(circle)
            addOverlay(Marker(gpsLatLong, markerBitmaps.gps, 0, 0))
        }

        // 4. Manual origin pin
        if (manualOrigin != null) {
            addOverlay(
                Marker(
                    LatLong(manualOrigin.latitude, manualOrigin.longitude),
                    markerBitmaps.origin,
                    0,
                    -markerBitmaps.origin.height / 2
                )
            )
        }

        // 5. Destination pin
        if (destination != null) {
            addOverlay(
                Marker(
                    LatLong(destination.latitude, destination.longitude),
                    markerBitmaps.destination,
                    0,
                    -markerBitmaps.destination.height / 2
                )
            )
        }

        mv.layerManager.redrawLayers()
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val mapView = MapView(ctx).apply {
                isClickable = true
                mapScaleBar.isVisible = true
                setBuiltInZoomControls(false)
                model.mapViewPosition.setZoomLevelMax(20.toByte())
                model.mapViewPosition.setZoomLevelMin(7.toByte())
                model.mapViewPosition.setCenter(
                    LatLong(ElSalvadorPlaces.DEFAULT_CENTER_LAT, ElSalvadorPlaces.DEFAULT_CENTER_LON)
                )
                model.mapViewPosition.setZoomLevel(9.toByte())
            }

            val tileCache = AndroidUtil.createTileCache(
                ctx,
                "sv_tile_cache",
                mapView.model.displayModel.tileSize,
                1.0f,
                mapView.model.frameBufferModel.overdrawFactor
            )

            val mapDataStore = MapFile(mapFile)
            val tileRenderer = object : TileRendererLayer(
                tileCache,
                mapDataStore,
                mapView.model.mapViewPosition,
                false,
                true,
                true,
                AndroidGraphicFactory.INSTANCE
            ) {
                override fun onLongPress(tapLatLong: LatLong?, layerXY: Point?, tapXY: Point?): Boolean {
                    if (tapLatLong != null) {
                        latestLongPress(tapLatLong.latitude, tapLatLong.longitude)
                        return true
                    }
                    return false
                }
            }.apply {
                setXmlRenderTheme(MapsforgeThemes.OSMARENDER)
            }

            mapView.model.mapViewPosition.addObserver {
                val center = mapView.model.mapViewPosition.center
                mapView.post {
                    latestCenterChanged(center.latitude, center.longitude)
                }
            }

            mapView.layerManager.layers.add(tileRenderer)
            tileCacheRef = tileCache
            tileRendererRef = tileRenderer
            mapViewRef = mapView
            mapView
        }
    )
}

private data class MarkerBitmaps(
    val destination: org.mapsforge.core.graphics.Bitmap,
    val origin: org.mapsforge.core.graphics.Bitmap,
    val gps: org.mapsforge.core.graphics.Bitmap,
    val saved: org.mapsforge.core.graphics.Bitmap
)

private fun createPinBitmap(
    context: Context,
    fillColor: Int,
    innerColor: Int
): org.mapsforge.core.graphics.Bitmap {
    val density = context.resources.displayMetrics.density
    val width = (36 * density).toInt().coerceAtLeast(48)
    val height = (46 * density).toInt().coerceAtLeast(60)
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val cx = width / 2f
    val r = width * 0.36f
    val cy = r + 4f * density

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x44000000
    }
    canvas.drawCircle(cx, height - 3f * density, r * 0.45f, shadowPaint)

    val path = Path().apply {
        addCircle(cx, cy, r, Path.Direction.CW)
        moveTo(cx - r * 0.72f, cy + r * 0.55f)
        lineTo(cx, height - 4f * density)
        lineTo(cx + r * 0.72f, cy + r * 0.55f)
        close()
    }
    val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        strokeWidth = 2.2f * density
        style = Paint.Style.STROKE
    }
    canvas.drawPath(path, pinPaint)
    canvas.drawCircle(cx, cy, r, borderPaint)

    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = innerColor
    }
    canvas.drawCircle(cx, cy, r * 0.42f, dotPaint)

    return AndroidGraphicFactory.convertToBitmap(BitmapDrawable(context.resources, bmp))
}

private fun createGpsDotBitmap(context: Context): org.mapsforge.core.graphics.Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (24 * density).toInt().coerceAtLeast(32)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val c = size / 2f
    canvas.drawCircle(c, c, size * 0.42f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() })
    canvas.drawCircle(c, c, size * 0.30f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1565C0.toInt() })
    return AndroidGraphicFactory.convertToBitmap(BitmapDrawable(context.resources, bmp))
}

private fun createSavedDotBitmap(context: Context): org.mapsforge.core.graphics.Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (20 * density).toInt().coerceAtLeast(28)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val c = size / 2f
    canvas.drawCircle(c, c, size * 0.40f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() })
    canvas.drawCircle(c, c, size * 0.28f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0F766E.toInt() })
    return AndroidGraphicFactory.convertToBitmap(BitmapDrawable(context.resources, bmp))
}
