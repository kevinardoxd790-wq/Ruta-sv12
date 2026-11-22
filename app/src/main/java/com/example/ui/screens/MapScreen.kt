package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RoundaboutRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Straight
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.TurnSharpLeft
import androidx.compose.material.icons.filled.TurnSharpRight
import androidx.compose.material.icons.filled.TurnSlightLeft
import androidx.compose.material.icons.filled.TurnSlightRight
import androidx.compose.material.icons.filled.UTurnLeft
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ElSalvadorPlaces
import com.example.data.local.SavedPlaceEntity
import com.example.map.OfflineInitState
import com.example.map.RouteProfile
import com.example.map.RouteStep
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import com.example.ui.components.LocationInputSheet
import com.example.ui.components.MapsforgeMapView
import com.example.ui.components.SavePlaceDialog
import com.example.util.CoordinateParser
import com.graphhopper.util.Instruction
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    uiState: MainUiState,
    savedPlaces: List<SavedPlaceEntity>,
    onPermissionResult: (Boolean) -> Unit,
    onOpenSavedPlaces: () -> Unit,
    onShowSearchSheet: (Boolean) -> Unit,
    onShowMenuSheet: (Boolean) -> Unit,
    onShowStepsSheet: (Boolean) -> Unit,
    onShowSaveDialog: (Boolean) -> Unit,
    onToggleNightMode: () -> Unit,
    onToggleCrosshairMode: () -> Unit,
    onSelectDestination: (Double, Double, String?) -> Unit,
    onSelectOrigin: (Double, Double, String?) -> Unit,
    onUseGpsOrigin: () -> Unit,
    onUseSanSalvadorOrigin: () -> Unit,
    onCenterMyLocation: () -> Unit,
    onUserPanMap: () -> Unit,
    onSelectProfile: (RouteProfile) -> Unit,
    onToggleProfile: () -> Unit,
    onCalculateRoute: () -> Unit,
    onStartNavigation: () -> Unit,
    onStopNavigation: () -> Unit,
    onClearRoute: () -> Unit,
    onSavePlace: (String, String, String) -> Unit,
    onOpenSavedPlace: (SavedPlaceEntity) -> Unit,
    onFocusStep: (Double, Double) -> Unit,
    onPasteIncomingText: (String) -> Unit,
    onRetryInit: () -> Unit,
    onDismissMessage: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    var mapCenterLat by remember { mutableStateOf(ElSalvadorPlaces.DEFAULT_CENTER_LAT) }
    var mapCenterLon by remember { mutableStateOf(ElSalvadorPlaces.DEFAULT_CENTER_LON) }
    var zoomCommandNonce by remember { mutableIntStateOf(0) }
    var zoomDelta by remember { mutableIntStateOf(0) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        onPermissionResult(granted)
    }

    LaunchedEffect(uiState.pendingMessage) {
        val msg = uiState.pendingMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        onDismissMessage()
    }

    val homePlace = remember(savedPlaces) {
        savedPlaces.firstOrNull {
            it.category.equals("Casa", ignoreCase = true) ||
                it.name.equals("Casa", ignoreCase = true)
        }
    }
    val workPlace = remember(savedPlaces) {
        savedPlaces.firstOrNull {
            it.category.equals("Trabajo", ignoreCase = true) ||
                it.name.equals("Trabajo", ignoreCase = true)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (val init = uiState.initState) {
                is OfflineInitState.Ready -> {
                    MapsforgeMapView(
                        mapFile = init.mapFile,
                        isNightMode = uiState.isNightMode,
                        cursorPosition = uiState.cursorPosition,
                        isFollowingGps = uiState.isFollowingGps,
                        isNavigating = uiState.isNavigating,
                        manualOrigin = uiState.manualOrigin,
                        destination = uiState.destination,
                        routeResult = uiState.routeResult,
                        savedPlaces = savedPlaces,
                        focusTarget = uiState.mapFocusRequest,
                        zoomCommandNonce = zoomCommandNonce,
                        zoomDelta = zoomDelta,
                        onLongPressCoordinate = { lat, lon ->
                            onSelectDestination(lat, lon, null)
                        },
                        onMapCenterChanged = { lat, lon ->
                            mapCenterLat = lat
                            mapCenterLon = lon
                        },
                        onUserPanMap = onUserPanMap,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is OfflineInitState.Preparing -> {
                    PreparingOfflineOverlay(
                        stepLabel = init.stepLabel,
                        progress = init.progress
                    )
                }

                is OfflineInitState.Error -> {
                    ErrorOfflineOverlay(
                        message = init.message,
                        onRetry = onRetryInit
                    )
                }
            }

            // Crosshair reticle when picking an unmarked location on the map
            if (uiState.isCrosshairMode && uiState.initState is OfflineInitState.Ready) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        border = androidx.compose.foundation.BorderStroke(
                            2.5.dp,
                            MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.GpsFixed,
                                contentDescription = "Mira de punto",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            // TOP OVERLAY: Either Waze-style Navigation Banner OR Floating Hamburger Menu + GPS Pill
            if (uiState.isNavigating && uiState.routeResult != null) {
                val steps = uiState.routeResult.steps
                val activeStep = steps.getOrNull(uiState.activeStepIndex) ?: steps.lastOrNull()
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    WazeNavigationTopBanner(
                        step = activeStep,
                        distanceToTurnMeters = uiState.distanceToNextTurnMeters,
                        onStop = onStopNavigation
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Top-left Waze floating menu button
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 6.dp,
                        shadowElevation = 6.dp,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(48.dp)
                    ) {
                        IconButton(
                            onClick = { onShowMenuSheet(true) },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("waze_menu_button")
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menú principal",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Top-right compact GPS + Offline badge
                    Surface(
                        shape = RoundedCornerShape(50),
                        tonalElevation = 4.dp,
                        shadowElevation = 4.dp,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        modifier = Modifier
                            .clickable(enabled = !uiState.hasLocationPermission) {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when {
                                    uiState.gpsPosition != null -> Icons.Default.GpsFixed
                                    !uiState.hasLocationPermission -> Icons.Default.GpsOff
                                    else -> Icons.Default.OfflinePin
                                },
                                contentDescription = null,
                                tint = when {
                                    uiState.gpsPosition != null -> MaterialTheme.colorScheme.primary
                                    !uiState.hasLocationPermission -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.secondary
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = when {
                                    !uiState.hasLocationPermission -> "Activar GPS"
                                    uiState.manualOrigin != null -> "Inicio manual"
                                    uiState.gpsPosition != null -> "Offline • GPS ±${uiState.gpsPosition.accuracyMeters.roundToInt()} m"
                                    else -> "Offline • Buscando GPS"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // RIGHT COMPACT ZOOM & DAY/NIGHT CONTROLS (when map is ready)
            if (uiState.initState is OfflineInitState.Ready) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        tonalElevation = 6.dp,
                        shadowElevation = 5.dp,
                        color = if (uiState.isNightMode) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    ) {
                        IconButton(
                            onClick = onToggleNightMode,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("night_mode_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Modo día o noche",
                                tint = if (uiState.isNightMode) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = 6.dp,
                        shadowElevation = 5.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = {
                                    zoomDelta = 1
                                    zoomCommandNonce++
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("zoom_in_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Acercar mapa")
                            }
                            HorizontalDivider(modifier = Modifier.width(30.dp))
                            IconButton(
                                onClick = {
                                    zoomDelta = -1
                                    zoomCommandNonce++
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("zoom_out_button")
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Alejar mapa")
                            }
                        }
                    }
                }
            }

            // BOTTOM SECTION: Floating Waze controls row + Bottom Sheet
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Floating Waze-style row above the bottom sheet: Left Recenter + Center Blue Vehicle Pill
                if (uiState.initState is OfflineInitState.Ready && !uiState.isCrosshairMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        // Left circular GPS/recenter button (matches Waze bottom-left button)
                        Surface(
                            shape = CircleShape,
                            tonalElevation = 8.dp,
                            shadowElevation = 6.dp,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .size(50.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (!uiState.hasLocationPermission) {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    } else {
                                        onCenterMyLocation()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("my_location_button")
                            ) {
                                Icon(
                                    imageVector = if (uiState.isFollowingGps) Icons.Default.GpsFixed else Icons.Default.MyLocation,
                                    contentDescription = "Centrar en mi ubicación",
                                    tint = if (uiState.isFollowingGps && uiState.gpsPosition != null) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }

                        // Center Waze-blue "Vehículo ▾" pill
                        Surface(
                            shape = RoundedCornerShape(50),
                            shadowElevation = 6.dp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onToggleProfile)
                                .minimumInteractiveComponentSize()
                                .testTag("waze_vehicle_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (uiState.selectedProfile == RouteProfile.CAR) {
                                        Icons.Default.DirectionsCar
                                    } else {
                                        Icons.AutoMirrored.Filled.DirectionsWalk
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.selectedProfile == RouteProfile.CAR) "Vehículo" else "A pie",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(2.dp))
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Cambiar modo",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Bottom Sheet Content
                when {
                    uiState.isCrosshairMode -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            CrosshairPickerBar(
                                centerLat = mapCenterLat,
                                centerLon = mapCenterLon,
                                onSetDestination = { onSelectDestination(mapCenterLat, mapCenterLon, null) },
                                onSetOrigin = { onSelectOrigin(mapCenterLat, mapCenterLon, null) },
                                onCancel = onToggleCrosshairMode
                            )
                        }
                    }

                    uiState.destination != null -> {
                        WazeRouteBottomSheet(
                            uiState = uiState,
                            onSelectProfile = onSelectProfile,
                            onCalculateRoute = onCalculateRoute,
                            onStartNavigation = onStartNavigation,
                            onStopNavigation = onStopNavigation,
                            onShowSteps = { onShowStepsSheet(true) },
                            onSavePlace = { onShowSaveDialog(true) },
                            onUseGpsOrigin = onUseGpsOrigin,
                            onUseSanSalvadorOrigin = onUseSanSalvadorOrigin,
                            onPickOriginOnMap = onToggleCrosshairMode,
                            onClear = onClearRoute
                        )
                    }

                    uiState.initState is OfflineInitState.Ready -> {
                        WazeSearchBottomSheet(
                            homePlace = homePlace,
                            workPlace = workPlace,
                            onOpenSearch = { onShowSearchSheet(true) },
                            onPasteLinkClick = {
                                val clip = clipboard.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    onPasteIncomingText(clip)
                                } else {
                                    onShowSearchSheet(true)
                                }
                            },
                            onOpenCrosshair = onToggleCrosshairMode,
                            onOpenSaved = onOpenSavedPlaces,
                            onShortcutPlaceClick = { place ->
                                if (place != null) {
                                    onOpenSavedPlace(place)
                                } else {
                                    onShowSearchSheet(true)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (uiState.showSearchSheet) {
        LocationInputSheet(
            savedPlaces = savedPlaces,
            onDismiss = { onShowSearchSheet(false) },
            onSelectDestination = { lat, lon, label ->
                onSelectDestination(lat, lon, label)
            },
            onSelectOrigin = { lat, lon, label ->
                onSelectOrigin(lat, lon, label)
            },
            onUseGpsOrigin = onUseGpsOrigin
        )
    }

    if (uiState.showMenuSheet) {
        WazeMenuBottomSheet(
            isNightMode = uiState.isNightMode,
            savedCount = savedPlaces.size,
            onDismiss = { onShowMenuSheet(false) },
            onOpenSaved = {
                onShowMenuSheet(false)
                onOpenSavedPlaces()
            },
            onEnableCrosshair = {
                onShowMenuSheet(false)
                onToggleCrosshairMode()
            },
            onToggleNightMode = onToggleNightMode,
            onCenterGps = {
                onShowMenuSheet(false)
                onCenterMyLocation()
            }
        )
    }

    if (uiState.showStepsSheet && uiState.routeResult != null) {
        ModalBottomSheet(
            onDismissRequest = { onShowStepsSheet(false) },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 20.dp)
            ) {
                Text(
                    text = "Instrucciones de ruta (${uiState.routeResult.profile.label})",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "${MainViewModel.formatDistance(uiState.routeResult.totalDistanceMeters)} • ${MainViewModel.formatDuration(uiState.routeResult.totalTimeMillis)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(uiState.routeResult.steps, key = { it.index }) { step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onFocusStep(step.lat, step.lon) }
                                .minimumInteractiveComponentSize()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = signToIcon(step.sign),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = step.instructionText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                if (step.distanceMeters > 0) {
                                    Text(
                                        text = MainViewModel.formatDistance(step.distanceMeters),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    if (uiState.routeResult.offRoadTailMeters > 25.0) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Últimos ${uiState.routeResult.offRoadTailMeters.roundToInt()} m fuera de vía mapeada: sigue la línea punteada ámbar hasta el pin exacto.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showSaveDialog && uiState.destination != null) {
        SavePlaceDialog(
            initialName = uiState.destination.label,
            latitude = uiState.destination.latitude,
            longitude = uiState.destination.longitude,
            onDismiss = { onShowSaveDialog(false) },
            onSave = onSavePlace
        )
    }
}

@Composable
private fun WazeSearchBottomSheet(
    homePlace: SavedPlaceEntity?,
    workPlace: SavedPlaceEntity?,
    onOpenSearch: () -> Unit,
    onPasteLinkClick: () -> Unit,
    onOpenCrosshair: () -> Unit,
    onOpenSaved: () -> Unit,
    onShortcutPlaceClick: (SavedPlaceEntity?) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 10.dp,
        shadowElevation = 14.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 10.dp, bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle pill like Waze
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.outline)
            )
            Spacer(Modifier.height(12.dp))

            // Big rounded "¿Adónde vas?" pill bar
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenSearch)
                        .minimumInteractiveComponentSize()
                        .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
                        .testTag("open_search_bar"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "¿Adónde vas?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onPasteLinkClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("quick_paste_button")
                    ) {
                        Icon(
                            Icons.Default.ContentPaste,
                            contentDescription = "Pegar enlace de Google Maps o coordenadas",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Horizontal shortcut cards like Waze's Casa / Trabajo / Guardados row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WazeShortcutCard(
                    icon = Icons.Default.Home,
                    title = "Casa",
                    subtitle = if (homePlace != null) "Ir ahora" else "Definir",
                    onClick = { onShortcutPlaceClick(homePlace) }
                )
                WazeShortcutCard(
                    icon = Icons.Default.Work,
                    title = "Trabajo",
                    subtitle = if (workPlace != null) "Ir ahora" else "Definir",
                    onClick = { onShortcutPlaceClick(workPlace) }
                )
                WazeShortcutCard(
                    icon = Icons.Default.ContentPaste,
                    title = "Link Maps",
                    subtitle = "Pegar enlace",
                    highlighted = true,
                    onClick = onPasteLinkClick
                )
                WazeShortcutCard(
                    icon = Icons.Default.AddLocationAlt,
                    title = "Fijar pin",
                    subtitle = "En el mapa",
                    onClick = onOpenCrosshair
                )
                WazeShortcutCard(
                    icon = Icons.Default.Bookmark,
                    title = "Guardados",
                    subtitle = "Mis rutas",
                    onClick = onOpenSaved
                )
            }
        }
    }
}

@Composable
private fun WazeShortcutCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (highlighted) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlighted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun WazeNavigationTopBanner(
    step: RouteStep?,
    distanceToTurnMeters: Double,
    onStop: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A2230)
        ),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = Color(0xFF0099FF),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = signToIcon(step?.sign ?: Instruction.CONTINUE_ON_STREET),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = MainViewModel.formatDistance(distanceToTurnMeters),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = step?.instructionText ?: "Sigue la ruta trazada",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onStop,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("stop_navigation_banner_button")
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Detener navegación",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun WazeRouteBottomSheet(
    uiState: MainUiState,
    onSelectProfile: (RouteProfile) -> Unit,
    onCalculateRoute: () -> Unit,
    onStartNavigation: () -> Unit,
    onStopNavigation: () -> Unit,
    onShowSteps: () -> Unit,
    onSavePlace: () -> Unit,
    onUseGpsOrigin: () -> Unit,
    onUseSanSalvadorOrigin: () -> Unit,
    onPickOriginOnMap: () -> Unit,
    onClear: () -> Unit
) {
    val dest = uiState.destination ?: return
    val route = uiState.routeResult
    val shownDist = if (uiState.isNavigating && uiState.remainingDistanceMeters > 0) {
        uiState.remainingDistanceMeters
    } else {
        route?.totalDistanceMeters ?: 0.0
    }
    val shownTime = if (uiState.isNavigating && uiState.remainingTimeMillis > 0) {
        uiState.remainingTimeMillis
    } else {
        route?.totalTimeMillis ?: 0L
    }

    Surface(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 10.dp,
        shadowElevation = 14.dp,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 10.dp, bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.outline)
            )
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dest.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!dest.nearestRoad.isNullOrBlank()) {
                        Text(
                            text = "Cerca de: ${dest.nearestRoad}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = CoordinateParser.formatCoordinates(dest.latitude, dest.longitude),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onSavePlace,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("save_destination_button")
                ) {
                    Icon(
                        Icons.Default.BookmarkBorder,
                        contentDescription = "Guardar punto",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onClear,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("clear_destination_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar destino")
                }
            }

            if (!uiState.isNavigating) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = uiState.selectedProfile == RouteProfile.CAR,
                        onClick = { onSelectProfile(RouteProfile.CAR) },
                        label = { Text("Carro / Moto") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.DirectionsCar,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("profile_car_chip")
                    )
                    FilterChip(
                        selected = uiState.selectedProfile == RouteProfile.FOOT,
                        onClick = { onSelectProfile(RouteProfile.FOOT) },
                        label = { Text("A pie") },
                        leadingIcon = {
                            Icon(
                                Icons.AutoMirrored.Filled.DirectionsWalk,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("profile_foot_chip")
                    )
                    Spacer(Modifier.weight(1f))
                    if (uiState.manualOrigin != null) {
                        TextButton(onClick = onUseGpsOrigin) {
                            Text("Usar GPS", maxLines = 1)
                        }
                    }
                }
            }

            if (uiState.isRouting) {
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                    Text("Trazando ruta offline…", style = MaterialTheme.typography.bodyMedium)
                }
            } else if (uiState.routeError != null) {
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = uiState.routeError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onPickOriginOnMap) {
                                Text("Fijar inicio")
                            }
                            FilledTonalButton(onClick = onUseSanSalvadorOrigin) {
                                Text("Desde San Salvador")
                            }
                        }
                    }
                }
            } else if (route != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "${MainViewModel.formatDuration(shownTime)}  •  ${MainViewModel.formatDistance(shownDist)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (route.offRoadTailMeters > 25.0) {
                            Text(
                                text = "Últimos ${route.offRoadTailMeters.roundToInt()} m fuera de vía hasta el pin",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    IconButton(onClick = onCalculateRoute) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recalcular ruta")
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (uiState.isNavigating) {
                        Button(
                            onClick = onStopNavigation,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("stop_navigation_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Salir")
                        }
                    } else {
                        Button(
                            onClick = onStartNavigation,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("start_navigation_button")
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ir ahora")
                        }
                    }

                    FilledTonalButton(
                        onClick = onShowSteps,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("show_steps_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pasos (${route.steps.size})")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WazeMenuBottomSheet(
    isNightMode: Boolean,
    savedCount: Int,
    onDismiss: () -> Unit,
    onOpenSaved: () -> Unit,
    onEnableCrosshair: () -> Unit,
    onToggleNightMode: () -> Unit,
    onCenterGps: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "RutaSV Offline",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Mapa completo y rutas de El Salvador sin internet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))

            MenuActionRow(
                icon = Icons.Default.Bookmark,
                title = "Lugares guardados ($savedCount)",
                subtitle = "Casa, trabajo, fincas y destinos offline",
                onClick = onOpenSaved
            )
            MenuActionRow(
                icon = Icons.Default.AddLocationAlt,
                title = "Elegir punto en el mapa",
                subtitle = "Marca cualquier terreno o camino rural con la mira",
                onClick = onEnableCrosshair
            )
            MenuActionRow(
                icon = Icons.Default.MyLocation,
                title = "Centrar en mi GPS",
                subtitle = "Seguir mi posición automáticamente en la ruta",
                onClick = onCenterGps
            )
            MenuActionRow(
                icon = if (isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                title = if (isNightMode) "Cambiar a mapa de día" else "Cambiar a mapa nocturno",
                subtitle = "Reduce el brillo para conducir de noche",
                onClick = onToggleNightMode
            )
        }
    }
}

@Composable
private fun MenuActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CrosshairPickerBar(
    centerLat: Double,
    centerLon: Double,
    onSetDestination: () -> Unit,
    onSetOrigin: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Apunta la mira al destino",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = CoordinateParser.formatCoordinates(centerLat, centerLon),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = "Cancelar selección")
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSetDestination,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("crosshair_set_destination_button")
                ) {
                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ir aquí")
                }
                OutlinedButton(
                    onClick = onSetOrigin,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("crosshair_set_origin_button")
                ) {
                    Text("Usar inicio")
                }
            }
        }
    }
}

@Composable
private fun PreparingOfflineOverlay(stepLabel: String, progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.widthIn(max = 420.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.OfflinePin,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(52.dp)
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Preparando mapa de El Salvador",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stepLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                )
            }
        }
    }
}

@Composable
private fun ErrorOfflineOverlay(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.widthIn(max = 420.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Error al inicializar mapa offline",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onRetry) {
                    Text("Reintentar")
                }
            }
        }
    }
}

private fun signToIcon(sign: Int): ImageVector = when (sign) {
    Instruction.TURN_LEFT -> Icons.Default.TurnLeft
    Instruction.TURN_SLIGHT_LEFT, Instruction.KEEP_LEFT -> Icons.Default.TurnSlightLeft
    Instruction.TURN_SHARP_LEFT -> Icons.Default.TurnSharpLeft
    Instruction.TURN_RIGHT -> Icons.Default.TurnRight
    Instruction.TURN_SLIGHT_RIGHT, Instruction.KEEP_RIGHT -> Icons.Default.TurnSlightRight
    Instruction.TURN_SHARP_RIGHT -> Icons.Default.TurnSharpRight
    Instruction.USE_ROUNDABOUT, Instruction.LEAVE_ROUNDABOUT -> Icons.Default.RoundaboutRight
    Instruction.U_TURN_LEFT, Instruction.U_TURN_RIGHT, Instruction.U_TURN_UNKNOWN -> Icons.Default.UTurnLeft
    Instruction.FINISH -> Icons.Default.Flag
    else -> Icons.Default.Straight
}
