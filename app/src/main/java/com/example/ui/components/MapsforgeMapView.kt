package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
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
import com.example.map.OfflineRouteResult
import com.example.ui.CursorDisplayPosition
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
import kotlin.math.abs
import kotlin.math.roundToInt

@SuppressLint("ClickableViewAccessibility")
@Composable
fun MapsforgeMapView(
    mapFile: File,
    isNightMode: Boolean,
    cursorPosition: CursorDisplayPosition?,
    isFollowingGps: Boolean,
    isNavigating: Boolean,
    manualOrigin: SelectedPoint?,
    destination: SelectedPoint?,
    routeResult: OfflineRouteResult?,
    savedPlaces: List<SavedPlaceEntity>,
    focusTarget: MapFocusTarget?,
    zoomCommandNonce: Int,
    zoomDelta: Int,
    onLongPressCoordinate: (Double, Double) -> Unit,
    onMapCenterChanged: (Double, Double) -> Unit,
    onUserPanMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val latestLongPress by rememberUpdatedState(onLongPressCoordinate)
    val latestCenterChanged by rememberUpdatedState(onMapCenterChanged)
    val latestUserPan by rememberUpdatedState(onUserPanMap)

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var tileRendererRef by remember { mutableStateOf<TileRendererLayer?>(null) }
    var tileCacheRef by remember { mutableStateOf<TileCache?>(null) }
    val overlayLayers = remember { mutableListOf<Layer>() }

    val markerBitmaps = remember(context) {
        MarkerBitmaps(
            destination = createPinBitmap(context, 0xFFEF4444.toInt(), 0xFFFFFFFF.toInt()),
            origin = createPinBitmap(context, 0xFF0099FF.toInt(), 0xFFFFFFFF.toInt()),
            saved = createSavedDotBitmap(context)
        )
    }

    val vehicleArrowCache = remember {
        LruCache<Int, org.mapsforge.core.graphics.Bitmap>(72)
    }

    fun getRotatedVehicleBitmap(bearingDeg: Float): org.mapsforge.core.graphics.Bitmap {
        val normalized = (((bearingDeg % 360f) + 360f) % 360f)
        val quantized = ((normalized / 5f).roundToInt() * 5) % 360
        return vehicleArrowCache.get(quantized) ?: createWazeArrowBitmap(
            context = context,
            bearingDegrees = quantized.toFloat()
        ).also { vehicleArrowCache.put(quantized, it) }
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

    // Night driving filter when night mode toggles
    LaunchedEffect(isNightMode, mapViewRef) {
        val mv = mapViewRef ?: return@LaunchedEffect
        if (isNightMode) {
            val nightMatrix = ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 232f,
                    0f, -1f, 0f, 0f, 238f,
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

    // Explicit center/zoom requests (e.g., destination selected, recenter, start navigation)
    LaunchedEffect(focusTarget?.nonce) {
        val target = focusTarget ?: return@LaunchedEffect
        val mv = mapViewRef ?: return@LaunchedEffect
        mv.model.mapViewPosition.setCenter(LatLong(target.latitude, target.longitude))
        target.zoom?.let { z ->
            mv.model.mapViewPosition.setZoomLevel(z)
        }
        latestCenterChanged(target.latitude, target.longitude)
    }

    // Automatically move the map camera with the moving vehicle along the route
    LaunchedEffect(cursorPosition?.latitude, cursorPosition?.longitude, isFollowingGps, isNavigating) {
        val mv = mapViewRef ?: return@LaunchedEffect
        val pos = cursorPosition ?: return@LaunchedEffect
        if (isFollowingGps || isNavigating) {
            if (isFollowingGps) {
                mv.model.mapViewPosition.animateTo(LatLong(pos.latitude, pos.longitude))
                if (isNavigating && mv.model.mapViewPosition.zoomLevel < 16) {
                    mv.model.mapViewPosition.setZoomLevel(17.toByte())
                }
            }
        }
    }

    // Zoom buttons
    LaunchedEffect(zoomCommandNonce) {
        if (zoomCommandNonce == 0) return@LaunchedEffect
        val mv = mapViewRef ?: return@LaunchedEffect
        val cur = mv.model.mapViewPosition.zoomLevel.toInt()
        val next = (cur + zoomDelta).coerceIn(6, 20).toByte()
        mv.model.mapViewPosition.setZoomLevel(next)
    }

    // Redraw overlays whenever route/markers/moving cursor change
    LaunchedEffect(
        mapViewRef,
        cursorPosition,
        manualOrigin,
        destination,
        routeResult,
        savedPlaces,
        isNightMode
    ) {
        val mv = mapViewRef ?: return@LaunchedEffect
        val layers = mv.layerManager.layers

        overlayLayers.forEach { layers.remove(it) }
        overlayLayers.clear()

        fun addOverlay(layer: Layer) {
            overlayLayers.add(layer)
            layers.add(layer)
        }

        // 1. Saved places
        for (place in savedPlaces) {
            addOverlay(
                Marker(
                    LatLong(place.latitude, place.longitude),
                    markerBitmaps.saved,
                    0,
                    0
                )
            )
        }

        // 2. Waze-style route polyline (indigo/violet with crisp dark border)
        if (routeResult != null && routeResult.routePoints.size >= 2) {
            val outlinePaint = AndroidGraphicFactory.INSTANCE.createPaint().apply {
                color = if (isNightMode) 0xFF0F172A.toInt() else 0xFF312E81.toInt()
                strokeWidth = 20f
                setStyle(Style.STROKE)
            }
            addOverlay(Polyline(outlinePaint, AndroidGraphicFactory.INSTANCE).apply {
                setPoints(routeResult.routePoints)
            })

            val routePaint = AndroidGraphicFactory.INSTANCE.createPaint().apply {
                color = if (isNightMode) 0xFF38BDF8.toInt() else 0xFF6366F1.toInt()
                strokeWidth = 12f
                setStyle(Style.STROKE)
            }
            addOverlay(Polyline(routePaint, AndroidGraphicFactory.INSTANCE).apply {
                setPoints(routeResult.routePoints)
            })

            // Off-road dashed amber tail if pin is inside a finca / unpaved terrain
            if (routeResult.offRoadTailPoints.size >= 2) {
                val tailPaint = AndroidGraphicFactory.INSTANCE.createPaint().apply {
                    color = 0xFFF59E0B.toInt()
                    strokeWidth = 8f
                    setStyle(Style.STROKE)
                    setDashPathEffect(floatArrayOf(18f, 14f))
                }
                addOverlay(Polyline(tailPaint, AndroidGraphicFactory.INSTANCE).apply {
                    setPoints(routeResult.offRoadTailPoints)
                })
            }
        }

        // 3. Moving Waze-style directional chevron + soft accuracy ring
        if (cursorPosition != null) {
            val gpsLatLong = LatLong(cursorPosition.latitude, cursorPosition.longitude)
            val fillPaint = AndroidGraphicFactory.INSTANCE.createPaint().apply {
                color = 0x260099FF
                setStyle(Style.FILL)
            }
            val strokePaint = AndroidGraphicFactory.INSTANCE.createPaint().apply {
                color = 0x800099FF.toInt()
                strokeWidth = 3f
                setStyle(Style.STROKE)
            }
            addOverlay(
                Circle(
                    gpsLatLong,
                    cursorPosition.accuracyMeters.coerceIn(10f, 120f),
                    fillPaint,
                    strokePaint
                )
            )
            addOverlay(
                Marker(
                    gpsLatLong,
                    getRotatedVehicleBitmap(cursorPosition.bearingDegrees),
                    0,
                    0
                )
            )
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
            val touchSlop = ViewConfiguration.get(ctx).scaledTouchSlop
            var downX = 0f
            var downY = 0f

            val mapView = MapView(ctx).apply {
                isClickable = true
                mapScaleBar.isVisible = false
                setBuiltInZoomControls(false)
                model.mapViewPosition.setZoomLevelMax(20.toByte())
                model.mapViewPosition.setZoomLevelMin(7.toByte())
                model.mapViewPosition.setCenter(
                    LatLong(ElSalvadorPlaces.DEFAULT_CENTER_LAT, ElSalvadorPlaces.DEFAULT_CENTER_LON)
                )
                model.mapViewPosition.setZoomLevel(10.toByte())

                setOnTouchListener { _, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            downX = event.x
                            downY = event.y
                        }
                        MotionEvent.ACTION_MOVE -> {
                            if (abs(event.x - downX) > touchSlop || abs(event.y - downY) > touchSlop) {
                                latestUserPan()
                            }
                        }
                    }
                    false
                }
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
    val saved: org.mapsforge.core.graphics.Bitmap
)

private fun createWazeArrowBitmap(
    context: Context,
    bearingDegrees: Float
): org.mapsforge.core.graphics.Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (44 * density).toInt().coerceAtLeast(64)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val c = size / 2f

    // Translucent outer aura like Waze's cursor
    val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x380099FF
        style = Paint.Style.FILL
    }
    canvas.drawCircle(c, c, size * 0.44f, auraPaint)

    canvas.save()
    canvas.rotate(bearingDegrees, c, c)

    val r = size * 0.34f
    val arrowPath = Path().apply {
        moveTo(c, c - r * 1.05f)          // sharp nose pointing North (0 deg)
        lineTo(c + r * 0.88f, c + r * 0.80f) // bottom-right wing
        lineTo(c, c + r * 0.42f)          // center notch
        lineTo(c - r * 0.88f, c + r * 0.80f) // bottom-left wing
        close()
    }

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x44000000
        style = Paint.Style.FILL
    }
    canvas.save()
    canvas.translate(0f, 2f * density)
    canvas.drawPath(arrowPath, shadowPaint)
    canvas.restore()

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00B8D9.toInt() // Waze cyan chevron
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        strokeWidth = 2.6f * density
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
    }
    canvas.drawPath(arrowPath, fillPaint)
    canvas.drawPath(arrowPath, strokePaint)
    canvas.restore()

    return AndroidGraphicFactory.convertToBitmap(BitmapDrawable(context.resources, bmp))
}

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

private fun createSavedDotBitmap(context: Context): org.mapsforge.core.graphics.Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (20 * density).toInt().coerceAtLeast(28)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val c = size / 2f
    canvas.drawCircle(c, c, size * 0.40f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() })
    canvas.drawCircle(c, c, size * 0.28f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0099FF.toInt() })
    return AndroidGraphicFactory.convertToBitmap(BitmapDrawable(context.resources, bmp))
}
