package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.LightMode
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
    onShowStepsSheet: (Boolean) -> Unit,
    onShowSaveDialog: (Boolean) -> Unit,
    onToggleNightMode: () -> Unit,
    onToggleCrosshairMode: () -> Unit,
    onSelectDestination: (Double, Double, String?) -> Unit,
    onSelectOrigin: (Double, Double, String?) -> Unit,
    onUseGpsOrigin: () -> Unit,
    onUseSanSalvadorOrigin: () -> Unit,
    onCenterMyLocation: () -> Unit,
    onSelectProfile: (RouteProfile) -> Unit,
    onCalculateRoute: () -> Unit,
    onStartNavigation: () -> Unit,
    onStopNavigation: () -> Unit,
    onClearRoute: () -> Unit,
    onSavePlace: (String, String, String) -> Unit,
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (val init = uiState.initState) {
                is OfflineInitState.Ready -> {
                    MapsforgeMapView(
                        mapFile = init.mapFile,
                        isNightMode = uiState.isNightMode,
                        gpsPosition = uiState.gpsPosition,
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

            // Crosshair reticle when aiming manually on remote terrain
            if (uiState.isCrosshairMode && uiState.initState is OfflineInitState.Ready) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp,
                            MaterialTheme.colorScheme.tertiary
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.GpsFixed,
                                contentDescription = "Mira de punto",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            // TOP CHROME: Search / Navigation Banner
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.isNavigating && uiState.routeResult != null) {
                    val steps = uiState.routeResult.steps
                    val activeStep = steps.getOrNull(uiState.activeStepIndex) ?: steps.lastOrNull()
                    NavigationTopBanner(
                        step = activeStep,
                        distanceToTurnMeters = uiState.distanceToNextTurnMeters,
                        onStop = onStopNavigation
                    )
                } else {
                    TopSearchAndStatusBar(
                        hasLocationPermission = uiState.hasLocationPermission,
                        gpsAccuracyMeters = uiState.gpsPosition?.accuracyMeters,
                        hasManualOrigin = uiState.manualOrigin != null,
                        onOpenSearch = { onShowSearchSheet(true) },
                        onPasteClick = {
                            val clip = clipboard.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                onPasteIncomingText(clip)
                            } else {
                                onShowSearchSheet(true)
                            }
                        },
                        onOpenSaved = onOpenSavedPlaces,
                        onRequestLocationPermission = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    )
                }
            }

            // RIGHT MAP CONTROLS
            if (uiState.initState is OfflineInitState.Ready) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MapControlPillButton(
                        icon = Icons.Default.MyLocation,
                        contentDescription = "Centrar en mi GPS",
                        testTag = "my_location_button",
                        highlighted = uiState.gpsPosition != null,
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
                        }
                    )
                    MapControlPillButton(
                        icon = Icons.Default.AddLocationAlt,
                        contentDescription = "Fijar punto con mira en el mapa",
                        testTag = "crosshair_mode_button",
                        highlighted = uiState.isCrosshairMode,
                        onClick = onToggleCrosshairMode
                    )
                    MapControlPillButton(
                        icon = if (uiState.isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Cambiar mapa día o noche",
                        testTag = "night_mode_button",
                        highlighted = uiState.isNightMode,
                        onClick = onToggleNightMode
                    )
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 6.dp,
                        shadowElevation = 4.dp,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
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
                            HorizontalDivider(modifier = Modifier.width(32.dp))
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

            // BOTTOM OVERLAY: Crosshair confirmation OR Route Sheet OR Empty hint
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                when {
                    uiState.isCrosshairMode -> {
                        CrosshairPickerBar(
                            centerLat = mapCenterLat,
                            centerLon = mapCenterLon,
                            onSetDestination = { onSelectDestination(mapCenterLat, mapCenterLon, null) },
                            onSetOrigin = { onSelectOrigin(mapCenterLat, mapCenterLon, null) },
                            onCancel = onToggleCrosshairMode
                        )
                    }

                    uiState.destination != null -> {
                        RouteDestinationPanel(
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
                        FirstTimeHintPill(
                            onOpenSearch = { onShowSearchSheet(true) },
                            onEnableCrosshair = onToggleCrosshairMode
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
                    color = MaterialTheme.colorScheme.secondary
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
private fun TopSearchAndStatusBar(
    hasLocationPermission: Boolean,
    gpsAccuracyMeters: Float?,
    hasManualOrigin: Boolean,
    onOpenSearch: () -> Unit,
    onPasteClick: () -> Unit,
    onOpenSaved: () -> Unit,
    onRequestLocationPermission: () -> Unit
) {
    Column(
        modifier = Modifier.widthIn(max = 600.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 8.dp,
            shadowElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenSearch)
                    .minimumInteractiveComponentSize()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("open_search_bar"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Pegar ubicación, link o buscar…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onPasteClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("quick_paste_button")
                ) {
                    Icon(
                        Icons.Default.ContentPaste,
                        contentDescription = "Pegar coordenadas del portapapeles",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(
                    onClick = onOpenSaved,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("saved_places_button")
                ) {
                    Icon(
                        Icons.Default.Bookmark,
                        contentDescription = "Lugares guardados",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.94f),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.OfflinePin,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "100% OFFLINE • EL SALVADOR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = when {
                    !hasLocationPermission -> MaterialTheme.colorScheme.tertiaryContainer
                    gpsAccuracyMeters != null -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }.copy(alpha = 0.94f),
                shadowElevation = 2.dp,
                modifier = Modifier.clickable(enabled = !hasLocationPermission) {
                    onRequestLocationPermission()
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (gpsAccuracyMeters != null) Icons.Default.GpsFixed else Icons.Default.GpsOff,
                        contentDescription = null,
                        tint = if (gpsAccuracyMeters != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = when {
                            !hasLocationPermission -> "Activar GPS"
                            hasManualOrigin -> "Inicio manual"
                            gpsAccuracyMeters != null -> "GPS ±${gpsAccuracyMeters.roundToInt()} m"
                            else -> "Buscando GPS"
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationTopBanner(
    step: RouteStep?,
    distanceToTurnMeters: Double,
    onStop: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
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
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = signToIcon(step?.sign ?: Instruction.CONTINUE_ON_STREET),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = MainViewModel.formatDistance(distanceToTurnMeters),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = step?.instructionText ?: "Sigue la ruta trazada",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onStop,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("stop_navigation_banner_button")
            ) {
                Text("Salir")
            }
        }
    }
}

@Composable
private fun RouteDestinationPanel(
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

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dest.label,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!dest.nearestRoad.isNullOrBlank()) {
                        Text(
                            text = "Vía más cercana: ${dest.nearestRoad}",
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

            Spacer(Modifier.height(8.dp))

            // Profile selector + Origin selector
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

            if (uiState.isRouting) {
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                    Text("Calculando ruta offline…", style = MaterialTheme.typography.bodyMedium)
                }
            } else if (uiState.routeError != null) {
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
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
                            text = "${MainViewModel.formatDistance(route.totalDistanceMeters)}  •  ${MainViewModel.formatDuration(route.totalTimeMillis)}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (route.offRoadTailMeters > 25.0) {
                            Text(
                                text = "Incluye ${route.offRoadTailMeters.roundToInt()} m fuera de camino hasta el pin",
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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("stop_navigation_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Finalizar guía")
                        }
                    } else {
                        Button(
                            onClick = onStartNavigation,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("start_navigation_button")
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Iniciar guía")
                        }
                    }

                    FilledTonalButton(
                        onClick = onShowSteps,
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

@Composable
private fun CrosshairPickerBar(
    centerLat: Double,
    centerLon: Double,
    onSetDestination: () -> Unit,
    onSetOrigin: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
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
                        text = "Apunta el centro del mapa al punto",
                        style = MaterialTheme.typography.titleSmall
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
private fun FirstTimeHintPill(
    onOpenSearch: () -> Unit,
    onEnableCrosshair: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Mantén presionado cualquier lugar del mapa o pega unas coordenadas de WhatsApp para calcular tu ruta sin internet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onOpenSearch,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("hint_paste_button")
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Pegar ubicación")
                }
                FilledTonalButton(
                    onClick = onEnableCrosshair,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("hint_pick_map_button")
                ) {
                    Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Elegir en mapa")
                }
            }
        }
    }
}

@Composable
private fun MapControlPillButton(
    icon: ImageVector,
    contentDescription: String,
    testTag: String,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        tonalElevation = 6.dp,
        shadowElevation = 4.dp,
        color = if (highlighted) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag(testTag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (highlighted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
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
