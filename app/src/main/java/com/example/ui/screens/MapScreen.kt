package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.unit.sp
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
import com.example.ui.theme.AppIcons
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
    onShowRouteOptionsSheet: (Boolean) -> Unit,
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
    onClearRoute: () -> Unit,
    onSavePlace: (String, String, String) -> Unit,
    onOpenSavedPlace: (SavedPlaceEntity) -> Unit,
    onFocusStep: (Double, Double) -> Unit,
    onPasteIncomingText: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
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

    val isRouteActive = uiState.destination != null && !uiState.isCrosshairMode
    val is3dActive = isRouteActive && uiState.routeResult != null

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (val init = uiState.initState) {
                is OfflineInitState.Ready -> {
                    MapsforgeMapView(
                        mapFile = init.mapFile,
                        isNightMode = uiState.isNightMode,
                        is3dPerspective = is3dActive,
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
                        isCrosshairMode = uiState.isCrosshairMode,
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
                        border = BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                AppIcons.GpsFixed,
                                contentDescription = "Mira de punto",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            // TOP CHROME: Edge-to-edge Black Waze Banner when Route Active OR Floating Menu + GPS Pill
            if (isRouteActive) {
                val route = uiState.routeResult
                val activeStep = route?.steps?.getOrNull(uiState.activeStepIndex)
                    ?: route?.steps?.lastOrNull()
                WazeBlackTopManeuverBar(
                    step = activeStep,
                    distanceToTurnMeters = uiState.distanceToNextTurnMeters,
                    destinationLabel = uiState.destination?.label.orEmpty(),
                    isRouting = uiState.isRouting,
                    onOpenSteps = {
                        if (route != null) onShowStepsSheet(true)
                    },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
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
                                    uiState.gpsPosition != null -> AppIcons.GpsFixed
                                    !uiState.hasLocationPermission -> AppIcons.GpsOff
                                    else -> AppIcons.OfflinePin
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

            // RIGHT FLOATING MINI CONTROLS (only 2 clean circles in route mode, or zoom + night in browse mode)
            if (uiState.initState is OfflineInitState.Ready) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(
                            top = if (isRouteActive) 104.dp else 72.dp,
                            end = 14.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isRouteActive) {
                        Surface(
                            shape = CircleShape,
                            tonalElevation = 6.dp,
                            shadowElevation = 6.dp,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(48.dp)
                        ) {
                            IconButton(
                                onClick = onToggleNightMode,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("night_mode_button")
                            ) {
                                Icon(
                                    imageVector = if (uiState.isNightMode) AppIcons.LightMode else AppIcons.DarkMode,
                                    contentDescription = "Modo día o noche",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (!uiState.isFollowingGps) {
                            Surface(
                                shape = CircleShape,
                                tonalElevation = 6.dp,
                                shadowElevation = 6.dp,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.size(48.dp)
                            ) {
                                IconButton(
                                    onClick = onCenterMyLocation,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("recenter_route_button")
                                ) {
                                    Icon(
                                        AppIcons.MyLocation,
                                        contentDescription = "Centrar en mi vehículo",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    } else {
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
                                    imageVector = if (uiState.isNightMode) AppIcons.LightMode else AppIcons.DarkMode,
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
                                    Icon(AppIcons.Remove, contentDescription = "Alejar mapa")
                                }
                            }
                        }
                    }
                }
            }

            // BOTTOM SECTION
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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

                    isRouteActive -> {
                        // Waze Drive HUD row above compact bottom bar: Speedometer (left) + Street pill (center) + Exit (right)
                        val speedKmh = ((uiState.gpsPosition?.speedMps ?: 0f) * 3.6f).roundToInt().coerceAtLeast(0)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 600.dp)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Speedometer circle (bottom-left like Waze screenshot)
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF2B313B),
                                border = BorderStroke(2.5.dp, Color(0xFF475569)),
                                shadowElevation = 6.dp,
                                modifier = Modifier.size(60.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = speedKmh.toString(),
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 18.sp
                                    )
                                    Text(
                                        text = "km/h",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 10.sp,
                                        lineHeight = 11.sp
                                    )
                                }
                            }

                            // Center floating current street pill (like "15 Calle Poniente" in screenshot)
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 6.dp,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .padding(horizontal = 10.dp)
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        if (uiState.routeError != null) {
                                            onUseSanSalvadorOrigin()
                                        } else {
                                            onToggleProfile()
                                        }
                                    }
                            ) {
                                Text(
                                    text = when {
                                        uiState.routeError != null -> "Sin GPS • Toca: desde San Salvador"
                                        !uiState.currentRoadName.isNullOrBlank() -> uiState.currentRoadName
                                        else -> uiState.destination?.label ?: "Ruta Offline"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }

                            // Clean circular Close/Exit route button on bottom-right
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFACC15),
                                shadowElevation = 6.dp,
                                modifier = Modifier.size(54.dp)
                            ) {
                                IconButton(
                                    onClick = onClearRoute,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("clear_destination_button")
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Finalizar ruta",
                                        tint = Color(0xFF1A2230),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }

                        // Compact Waze ETA Bottom Bar (identical layout to screenshot: Search | ETA & time/km | Options)
                        WazeCompactDriveBottomBar(
                            uiState = uiState,
                            onOpenSearch = { onShowSearchSheet(true) },
                            onOpenRouteOptions = { onShowRouteOptionsSheet(true) },
                            onUseSanSalvadorOrigin = onUseSanSalvadorOrigin,
                            onPickOriginOnMap = onToggleCrosshairMode
                        )
                    }

                    uiState.initState is OfflineInitState.Ready -> {
                        // Browse/Idle state: Floating Recenter + Blue Vehículo pill + Waze ¿Adónde vas? sheet
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 600.dp)
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
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
                                        imageVector = if (uiState.isFollowingGps) AppIcons.GpsFixed else AppIcons.MyLocation,
                                        contentDescription = "Centrar en mi ubicación",
                                        tint = if (uiState.isFollowingGps && uiState.gpsPosition != null) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }

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
                                            AppIcons.DirectionsCar
                                        } else {
                                            AppIcons.DirectionsWalk
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
            query = uiState.searchQuery,
            offlineSearchResults = uiState.offlineSearchResults,
            isIndexingOfflinePlaces = uiState.isIndexingOfflinePlaces,
            indexedPlacesCount = uiState.indexedPlacesCount,
            onQueryChanged = onSearchQueryChanged,
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

    if (uiState.showRouteOptionsSheet && uiState.destination != null) {
        WazeRouteOptionsBottomSheet(
            uiState = uiState,
            onDismiss = { onShowRouteOptionsSheet(false) },
            onSelectProfile = { profile ->
                onShowRouteOptionsSheet(false)
                onSelectProfile(profile)
            },
            onShowSteps = { onShowStepsSheet(true) },
            onSavePlace = { onShowSaveDialog(true) },
            onRecalculate = {
                onShowRouteOptionsSheet(false)
                onCalculateRoute()
            },
            onClearRoute = {
                onShowRouteOptionsSheet(false)
                onClearRoute()
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
private fun WazeBlackTopManeuverBar(
    step: RouteStep?,
    distanceToTurnMeters: Double,
    destinationLabel: String,
    isRouting: Boolean,
    onOpenSteps: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF000000),
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenSteps)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = signToIcon(step?.sign ?: Instruction.CONTINUE_ON_STREET),
                contentDescription = "Siguiente giro",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isRouting) "Calculando…" else MainViewModel.formatDistance(distanceToTurnMeters),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = step?.streetName?.takeIf { it.isNotBlank() }
                        ?: step?.instructionText
                        ?: destinationLabel,
                    color = Color(0xFF29B6F6),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun WazeCompactDriveBottomBar(
    uiState: MainUiState,
    onOpenSearch: () -> Unit,
    onOpenRouteOptions: () -> Unit,
    onUseSanSalvadorOrigin: () -> Unit,
    onPickOriginOnMap: () -> Unit
) {
    val route = uiState.routeResult
    val shownDist = if (uiState.remainingDistanceMeters > 0) {
        uiState.remainingDistanceMeters
    } else {
        route?.totalDistanceMeters ?: 0.0
    }
    val shownTime = if (uiState.remainingTimeMillis > 0) {
        uiState.remainingTimeMillis
    } else {
        route?.totalTimeMillis ?: 0L
    }

    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
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
                .padding(horizontal = 18.dp)
                .padding(top = 8.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.outline)
            )
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left circular Search button (matches Waze bottom-left magnifying glass)
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("drive_search_button")
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Buscar otro destino",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Center compact ETA + duration & distance
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                        .clickable(enabled = route != null) { onOpenRouteOptions() },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        uiState.isRouting -> {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Trazando ruta…",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        uiState.routeError != null -> {
                            Text(
                                text = "Esperando origen",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(
                                    onClick = onUseSanSalvadorOrigin,
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Desde San Salvador", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = onPickOriginOnMap,
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Fijar", fontSize = 12.sp)
                                }
                            }
                        }
                        route != null -> {
                            Text(
                                text = MainViewModel.formatClockEta(shownTime),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${MainViewModel.formatDuration(shownTime)}  •  ${MainViewModel.formatDistance(shownDist)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Right circular Route Options button (matches Waze bottom-right split-route icon)
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(
                        onClick = onOpenRouteOptions,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("drive_route_options_button")
                    ) {
                        Icon(
                            AppIcons.AltRoute,
                            contentDescription = "Opciones de ruta",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WazeRouteOptionsBottomSheet(
    uiState: MainUiState,
    onDismiss: () -> Unit,
    onSelectProfile: (RouteProfile) -> Unit,
    onShowSteps: () -> Unit,
    onSavePlace: () -> Unit,
    onRecalculate: () -> Unit,
    onClearRoute: () -> Unit
) {
    val dest = uiState.destination ?: return
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
                text = dest.label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = CoordinateParser.formatCoordinates(dest.latitude, dest.longitude),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))

            MenuActionRow(
                icon = Icons.AutoMirrored.Filled.List,
                title = "Ver pasos (${uiState.routeResult?.steps?.size ?: 0})",
                subtitle = "Lista completa de giros y calles",
                onClick = onShowSteps
            )
            MenuActionRow(
                icon = AppIcons.BookmarkBorder,
                title = "Guardar lugar offline",
                subtitle = "Guarda como Casa, Trabajo o punto favorito",
                onClick = onSavePlace
            )
            MenuActionRow(
                icon = if (uiState.selectedProfile == RouteProfile.CAR) {
                    AppIcons.DirectionsWalk
                } else {
                    AppIcons.DirectionsCar
                },
                title = if (uiState.selectedProfile == RouteProfile.CAR) {
                    "Cambiar a ruta A pie"
                } else {
                    "Cambiar a ruta en Carro / Moto"
                },
                subtitle = "Modo actual: ${uiState.selectedProfile.label}",
                onClick = {
                    val next = if (uiState.selectedProfile == RouteProfile.CAR) {
                        RouteProfile.FOOT
                    } else {
                        RouteProfile.CAR
                    }
                    onSelectProfile(next)
                }
            )
            MenuActionRow(
                icon = Icons.Default.Refresh,
                title = "Recalcular ruta",
                subtitle = "Volver a trazar desde tu posición actual",
                onClick = onRecalculate
            )

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onClearRoute,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Finalizar ruta")
            }
        }
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
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.outline)
            )
            Spacer(Modifier.height(12.dp))

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
                            AppIcons.ContentPaste,
                            contentDescription = "Pegar enlace de Google Maps o coordenadas",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

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
                    icon = AppIcons.Work,
                    title = "Trabajo",
                    subtitle = if (workPlace != null) "Ir ahora" else "Definir",
                    onClick = { onShortcutPlaceClick(workPlace) }
                )
                WazeShortcutCard(
                    icon = AppIcons.ContentPaste,
                    title = "Link Maps",
                    subtitle = "Pegar enlace",
                    highlighted = true,
                    onClick = onPasteLinkClick
                )
                WazeShortcutCard(
                    icon = AppIcons.AddLocationAlt,
                    title = "Fijar pin",
                    subtitle = "En el mapa",
                    onClick = onOpenCrosshair
                )
                WazeShortcutCard(
                    icon = AppIcons.Bookmark,
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
                icon = AppIcons.Bookmark,
                title = "Lugares guardados ($savedCount)",
                subtitle = "Casa, trabajo, fincas y destinos offline",
                onClick = onOpenSaved
            )
            MenuActionRow(
                icon = AppIcons.AddLocationAlt,
                title = "Elegir punto en el mapa",
                subtitle = "Marca cualquier terreno o camino rural con la mira",
                onClick = onEnableCrosshair
            )
            MenuActionRow(
                icon = AppIcons.MyLocation,
                title = "Centrar en mi GPS",
                subtitle = "Seguir mi posición automáticamente en la ruta",
                onClick = onCenterGps
            )
            MenuActionRow(
                icon = if (isNightMode) AppIcons.LightMode else AppIcons.DarkMode,
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
                    Icon(AppIcons.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
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
                    AppIcons.OfflinePin,
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
    Instruction.TURN_LEFT -> AppIcons.TurnLeft
    Instruction.TURN_SLIGHT_LEFT, Instruction.KEEP_LEFT -> AppIcons.TurnSlightLeft
    Instruction.TURN_SHARP_LEFT -> AppIcons.TurnSharpLeft
    Instruction.TURN_RIGHT -> AppIcons.TurnRight
    Instruction.TURN_SLIGHT_RIGHT, Instruction.KEEP_RIGHT -> AppIcons.TurnSlightRight
    Instruction.TURN_SHARP_RIGHT -> AppIcons.TurnSharpRight
    Instruction.USE_ROUNDABOUT, Instruction.LEAVE_ROUNDABOUT -> AppIcons.RoundaboutRight
    Instruction.U_TURN_LEFT, Instruction.U_TURN_RIGHT, Instruction.U_TURN_UNKNOWN -> AppIcons.UTurnLeft
    Instruction.FINISH -> AppIcons.Flag
    else -> AppIcons.Straight
}
