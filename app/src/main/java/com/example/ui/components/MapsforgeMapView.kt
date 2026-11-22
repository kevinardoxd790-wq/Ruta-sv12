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
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
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
import org.mapsforge.core.model.Rotation
import org.mapsforge.core.util.Parameters
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@SuppressLint("ClickableViewAccessibility", "UnusedBoxWithConstraintsScope")
@Composable
fun MapsforgeMapView(
    mapFile: File,
    isNightMode: Boolean,
    is3dPerspective: Boolean,
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
    val density = LocalDensity.current
    val latestLongPress by rememberUpdatedState(onLongPressCoordinate)
    val latestCenterChanged by rememberUpdatedState(onMapCenterChanged)
    val latestUserPan by rememberUpdatedState(onUserPanMap)

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var tileRendererRef by remember { mutableStateOf<TileRendererLayer?>(null) }
    var tileCacheRef by remember { mutableStateOf<TileCache?>(null) }
    var mapViewSize by remember { mutableStateOf(IntSize.Zero) }
    var userRotationOffset by remember { mutableFloatStateOf(0f) }
    val overlayLayers = remember { mutableListOf<Layer>() }

    // Calibrated Waze first-person 3D perspective (smooth, no white gaps):
    val targetTilt = if (is3dPerspective) 22f else 0f
    val targetScaleX = if (is3dPerspective) 1.35f else 1.0f
    val targetScaleY = if (is3dPerspective) 1.45f else 1.0f

    val animatedTilt by animateFloatAsState(
        targetValue = targetTilt,
        animationSpec = tween(durationMillis = 600),
        label = "map_tilt_3d"
    )
    val animatedScaleX by animateFloatAsState(
        targetValue = targetScaleX,
        animationSpec = tween(durationMillis = 600),
        label = "map_scale_x"
    )
    val animatedScaleY by animateFloatAsState(
        targetValue = targetScaleY,
        animationSpec = tween(durationMillis = 600),
        label = "map_scale_y"
    )

    val markerBitmaps = remember(context) {
        MarkerBitmaps(
            destination = createPinBitmap(context, 0xFFEF4444.toInt(), 0xFFFFFFFF.toInt()),
            origin = createPinBitmap(context, 0xFF0099FF.toInt(), 0xFFFFFFFF.toInt()),
            saved = createSavedDotBitmap(context),
            vehicle = createWazeStraightArrowBitmap(context)
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

    val fallbackRouteBearing = remember(routeResult) {
        val pts = routeResult?.routePoints
        if (pts != null && pts.size >= 2) {
            computeBearingDegrees(pts[0].latitude, pts[0].longitude, pts[1].latitude, pts[1].longitude)
        } else null
    }

    // Keep Mapsforge FrameBuffer centered (0.5f) and rotate map with heading-up bearing + manual gesture rotation offset
    LaunchedEffect(
        mapViewRef,
        mapViewSize,
        is3dPerspective,
        isFollowingGps,
        cursorPosition?.bearingDegrees,
        fallbackRouteBearing,
        userRotationOffset
    ) {
        val mv = mapViewRef ?: return@LaunchedEffect
        runCatching {
            mv.model.mapViewPosition.setMapViewCenterY(0.5f)
        }
        val bearing = cursorPosition?.bearingDegrees ?: fallbackRouteBearing ?: 0f
        val totalRotation = bearing + userRotationOffset
        if (mv.width > 0 && mv.height > 0) {
            mv.rotate(Rotation(-totalRotation, mv.width / 2f, mv.height / 2f))
        }
        mv.invalidate()
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
        if (isFollowingGps) {
            mv.model.mapViewPosition.animateTo(LatLong(pos.latitude, pos.longitude))
            if (isNavigating && mv.model.mapViewPosition.zoomLevel < 16) {
                mv.model.mapViewPosition.setZoomLevel(17.toByte())
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

        // 2. Waze signature purple route polyline with crisp dark purple outline
        if (routeResult != null && routeResult.routePoints.size >= 2) {
            val outlinePaint = AndroidGraphicFactory.INSTANCE.createPaint().apply {
                color = if (isNightMode) 0xFF0F172A.toInt() else 0xFF2E1065.toInt()
                strokeWidth = 22f
                setStyle(Style.STROKE)
            }
            addOverlay(Polyline(outlinePaint, AndroidGraphicFactory.INSTANCE).apply {
                setPoints(routeResult.routePoints)
            })

            val routePaint = AndroidGraphicFactory.INSTANCE.createPaint().apply {
                color = if (isNightMode) 0xFF38BDF8.toInt() else 0xFF7C2AE8.toInt()
                strokeWidth = 14f
                setStyle(Style.STROKE)
            }
            addOverlay(Polyline(routePaint, AndroidGraphicFactory.INSTANCE).apply {
                setPoints(routeResult.routePoints)
            })

            // Off-road dashed amber tail if pin is inside a finca / unpaved terrain
            if (routeResult.offRoadTailPoints.size >= 2) {
                val tailPaint = AndroidGraphicFactory.INSTANCE.createPaint().apply {
                    color = 0xFFF59E0B.toInt()
                    strokeWidth = 9f
                    setStyle(Style.STROKE)
                    setDashPathEffect(floatArrayOf(18f, 14f))
                }
                addOverlay(Polyline(tailPaint, AndroidGraphicFactory.INSTANCE).apply {
                    setPoints(routeResult.offRoadTailPoints)
                })
            }
        }

        // 3. Moving straight-up Waze chevron arrow + soft aura
        if (cursorPosition != null) {
            val gpsLatLong = LatLong(cursorPosition.latitude, cursorPosition.longitude)
            val fillPaint = AndroidGraphicFactory.INSTANCE.createPaint().apply {
                color = 0x220099FF
                setStyle(Style.FILL)
            }
            val strokePaint = AndroidGraphicFactory.INSTANCE.createPaint().apply {
                color = 0x660099FF.toInt()
                strokeWidth = 2.5f
                setStyle(Style.STROKE)
            }
            addOverlay(
                Circle(
                    gpsLatLong,
                    cursorPosition.accuracyMeters.coerceIn(10f, 100f),
                    fillPaint,
                    strokePaint
                )
            )
            addOverlay(
                Marker(
                    gpsLatLong,
                    markerBitmaps.vehicle,
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

    BoxWithConstraints(
        modifier = modifier
            .background(if (isNightMode) Color(0xFF0F172A) else Color(0xFFF3F5F7))
            .clipToBounds()
    ) {
        val viewportHeightPx = with(density) { maxHeight.toPx() }
        val animatedTranslationY by animateFloatAsState(
            targetValue = if (is3dPerspective) viewportHeightPx * 0.12f else 0f,
            animationSpec = tween(durationMillis = 600),
            label = "map_translate_y_3d"
        )

        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { mapViewSize = it }
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                    translationY = animatedTranslationY
                    rotationX = animatedTilt
                    scaleX = animatedScaleX
                    scaleY = animatedScaleY
                    cameraDistance = 36f
                },
            factory = { ctx ->
                Parameters.SQUARE_FRAME_BUFFER = true
                val touchSlop = ViewConfiguration.get(ctx).scaledTouchSlop
                var downX = 0f
                var downY = 0f

                val rotationDetector = RotationGestureDetector { delta ->
                    userRotationOffset = (userRotationOffset + delta) % 360f
                }

                val mapView = MapView(ctx).apply {
                    isClickable = true
                    mapScaleBar.isVisible = false
                    setBuiltInZoomControls(false)
                    model.mapViewPosition.setZoomLevelMax(20.toByte())
                    model.mapViewPosition.setZoomLevelMin(7.toByte())
                    model.mapViewPosition.setMapViewCenterY(0.5f)
                    model.mapViewPosition.setCenter(
                        LatLong(ElSalvadorPlaces.DEFAULT_CENTER_LAT, ElSalvadorPlaces.DEFAULT_CENTER_LON)
                    )
                    model.mapViewPosition.setZoomLevel(10.toByte())

                    setOnTouchListener { _, event ->
                        rotationDetector.onTouchEvent(event)
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
}

private class RotationGestureDetector(private val onRotation: (Float) -> Unit) {
    private var pointerId1 = -1
    private var pointerId2 = -1
    private var lastAngle = 0f

    fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pointerId1 = event.getPointerId(0)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (pointerId1 != -1) {
                    pointerId2 = event.getPointerId(event.actionIndex)
                    lastAngle = angleBetweenPointers(event, pointerId1, pointerId2)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (pointerId1 != -1 && pointerId2 != -1) {
                    val idx1 = event.findPointerIndex(pointerId1)
                    val idx2 = event.findPointerIndex(pointerId2)
                    if (idx1 >= 0 && idx2 >= 0) {
                        val newAngle = angleBetweenPointers(event, pointerId1, pointerId2)
                        var delta = newAngle - lastAngle
                        if (delta > 180f) delta -= 360f
                        if (delta < -180f) delta += 360f
                        if (abs(delta) > 0.1f) {
                            onRotation(delta)
                            lastAngle = newAngle
                        }
                    }
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val actionId = event.getPointerId(event.actionIndex)
                if (actionId == pointerId1) {
                    pointerId1 = pointerId2
                    pointerId2 = -1
                } else if (actionId == pointerId2) {
                    pointerId2 = -1
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                pointerId1 = -1
                pointerId2 = -1
            }
        }
        return true
    }

    private fun angleBetweenPointers(event: MotionEvent, id1: Int, id2: Int): Float {
        val i1 = event.findPointerIndex(id1)
        val i2 = event.findPointerIndex(id2)
        if (i1 < 0 || i2 < 0) return 0f
        val x1 = event.getX(i1)
        val y1 = event.getY(i1)
        val x2 = event.getX(i2)
        val y2 = event.getY(i2)
        return Math.toDegrees(atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())).toFloat()
    }
}

private fun computeBearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val phi1 = Math.toRadians(lat1)
    val phi2 = Math.toRadians(lat2)
    val dLon = Math.toRadians(lon2 - lon1)
    val y = sin(dLon) * cos(phi2)
    val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLon)
    val deg = Math.toDegrees(atan2(y, x)).toFloat()
    return ((deg % 360f) + 360f) % 360f
}

private data class MarkerBitmaps(
    val destination: org.mapsforge.core.graphics.Bitmap,
    val origin: org.mapsforge.core.graphics.Bitmap,
    val saved: org.mapsforge.core.graphics.Bitmap,
    val vehicle: org.mapsforge.core.graphics.Bitmap
)

private fun createWazeStraightArrowBitmap(context: Context): org.mapsforge.core.graphics.Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (48 * density).toInt().coerceAtLeast(68)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val c = size / 2f

    // Translucent outer aura like Waze's cursor
    val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x360099FF
        style = Paint.Style.FILL
    }
    canvas.drawCircle(c, c, size * 0.45f, auraPaint)

    val r = size * 0.34f
    val arrowPath = Path().apply {
        moveTo(c, c - r * 1.04f)
        lineTo(c + r * 0.90f, c + r * 0.78f)
        lineTo(c, c + r * 0.44f)
        lineTo(c - r * 0.90f, c + r * 0.78f)
        close()
    }

    // Drop shadow
    canvas.save()
    canvas.translate(0f, 3.5f * density)
    canvas.drawPath(arrowPath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x45000000
        style = Paint.Style.FILL
    })
    canvas.restore()

    // 3D beveled lower rim
    canvas.save()
    canvas.translate(0f, 2.2f * density)
    canvas.drawPath(arrowPath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF0284C7.toInt()
        style = Paint.Style.FILL
    })
    canvas.drawPath(arrowPath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE2E8F0.toInt()
        strokeWidth = 2.6f * density
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
    })
    canvas.restore()

    // Bright Waze cyan top chevron face + crisp white rim
    canvas.drawPath(arrowPath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00B8D9.toInt()
        style = Paint.Style.FILL
    })
    canvas.drawPath(arrowPath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        strokeWidth = 2.4f * density
        strokeJoin = Paint.Join.ROUND
        style = Paint.Style.STROKE
    })

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
