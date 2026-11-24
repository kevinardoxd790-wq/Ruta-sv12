package com.example.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ElSalvadorPlaces
import com.example.data.local.AppDatabase
import com.example.data.local.OfflinePlaceMatch
import com.example.data.local.OfflinePlaceRepository
import com.example.data.local.SavedPlaceEntity
import com.example.data.local.SavedPlaceRepository
import com.example.location.GpsPosition
import com.example.location.OfflineLocationTracker
import com.example.map.OfflineInitState
import com.example.map.OfflineMapDataManager
import com.example.map.OfflineRouteResult
import com.example.map.RouteProfile
import com.example.util.CoordinateParseResult
import com.example.util.CoordinateParser
import com.example.util.GoogleMapsLinkResolver
import com.example.util.RouteGeometry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class SelectedPoint(
    val latitude: Double,
    val longitude: Double,
    val label: String,
    val nearestRoad: String? = null,
    val isInsideElSalvador: Boolean = CoordinateParser.isInsideElSalvador(latitude, longitude)
)

data class CursorDisplayPosition(
    val latitude: Double,
    val longitude: Double,
    val bearingDegrees: Float,
    val accuracyMeters: Float,
    val isSnappedToRoute: Boolean,
    val nearestSegmentIndex: Int = 0,
    val distanceAlongRouteMeters: Double = 0.0
)

data class MainUiState(
    val initState: OfflineInitState = OfflineInitState.Preparing("Iniciando…", 0.05f),
    val hasLocationPermission: Boolean = false,
    val gpsPosition: GpsPosition? = null,
    val cursorPosition: CursorDisplayPosition? = null,
    val isFollowingGps: Boolean = true,
    val manualOrigin: SelectedPoint? = null,
    val destination: SelectedPoint? = null,
    val selectedProfile: RouteProfile = RouteProfile.CAR,
    val routeResult: OfflineRouteResult? = null,
    val isRouting: Boolean = false,
    val routeError: String? = null,
    val isNavigating: Boolean = false,
    val activeStepIndex: Int = 0,
    val distanceToNextTurnMeters: Double = 0.0,
    val remainingDistanceMeters: Double = 0.0,
    val remainingTimeMillis: Long = 0L,
    val currentRoadName: String? = null,
    val isNightMode: Boolean = false,
    val isCrosshairMode: Boolean = false,
    val showStepsSheet: Boolean = false,
    val showSearchSheet: Boolean = false,
    val showMenuSheet: Boolean = false,
    val showRouteOptionsSheet: Boolean = false,
    val showSaveDialog: Boolean = false,
    val pendingMessage: String? = null,
    val mapFocusRequest: MapFocusTarget? = null,
    val searchQuery: String = "",
    val offlineSearchResults: List<OfflinePlaceMatch> = emptyList(),
    val isIndexingOfflinePlaces: Boolean = false,
    val indexedPlacesCount: Int = 0
)

data class MapFocusTarget(
    val latitude: Double,
    val longitude: Double,
    val zoom: Byte? = null,
    val nonce: Long = System.nanoTime()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val mapDataManager = OfflineMapDataManager(application)
    private val db = AppDatabase.getInstance(application)
    private val repository = SavedPlaceRepository(db.savedPlaceDao())
    private val offlinePlaceRepository = OfflinePlaceRepository(db.offlinePlaceDao())

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val savedPlaces: StateFlow<List<SavedPlaceEntity>> = repository.allPlaces.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private var locationJob: Job? = null
    private var rerouteJob: Job? = null
    private var searchJob: Job? = null
    private var indexJob: Job? = null
    private var isAutoRerouting: Boolean = false
    private var lastRerouteElapsedMs: Long = 0L
    private var lastRoadLookupElapsedMs: Long = 0L

    init {
        initializeOfflineData()
    }

    fun initializeOfflineData() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(initState = OfflineInitState.Preparing("Verificando archivos offline…", 0.05f))
            }
            val result = mapDataManager.initialize { progress ->
                _uiState.update { it.copy(initState = progress) }
            }
            _uiState.update { it.copy(initState = result) }
            if (result is OfflineInitState.Ready) {
                ensureOfflineSearchIndexBuilt()
            }
        }
    }

    private fun ensureOfflineSearchIndexBuilt() {
        if (indexJob?.isActive == true) return
        indexJob = viewModelScope.launch(Dispatchers.IO) {
            val existingCount = runCatching { offlinePlaceRepository.count() }.getOrDefault(0)
            if (existingCount > 100) {
                _uiState.update {
                    it.copy(isIndexingOfflinePlaces = false, indexedPlacesCount = existingCount)
                }
                return@launch
            }
            _uiState.update { it.copy(isIndexingOfflinePlaces = true, indexedPlacesCount = existingCount) }
            runCatching {
                offlinePlaceRepository.clearAll()
                val total = mapDataManager.buildOfflineSearchIndex(
                    onProgress = { _, countSoFar ->
                        _uiState.update { it.copy(indexedPlacesCount = countSoFar) }
                    },
                    onBatch = { batch ->
                        offlinePlaceRepository.insertBatch(batch)
                    }
                )
                _uiState.update {
                    it.copy(isIndexingOfflinePlaces = false, indexedPlacesCount = total)
                }
                val currentQuery = _uiState.value.searchQuery
                if (currentQuery.isNotBlank()) {
                    onSearchQueryChanged(currentQuery)
                }
            }.onFailure {
                _uiState.update { it.copy(isIndexingOfflinePlaces = false) }
            }
        }
    }

    fun onLocationPermissionChanged(granted: Boolean) {
        _uiState.update { it.copy(hasLocationPermission = granted) }
        if (granted) {
            startLocationUpdates()
        } else {
            locationJob?.cancel()
            locationJob = null
        }
    }

    private fun startLocationUpdates() {
        if (locationJob?.isActive == true) return
        locationJob = viewModelScope.launch {
            OfflineLocationTracker.observeLocation(getApplication()).collect { pos ->
                if (pos == null) return@collect
                var shouldSilentReroute = false
                var rerouteDest: SelectedPoint? = null
                var rerouteProfile = RouteProfile.CAR

                _uiState.update { state ->
                    val route = state.routeResult
                    val prevCursor = state.cursorPosition
                    val snap = if (route != null) {
                        RouteGeometry.analyzePositionOnRoute(
                            lat = pos.latitude,
                            lon = pos.longitude,
                            route = route,
                            previousSegmentIndex = if (prevCursor?.isSnappedToRoute == true) prevCursor.nearestSegmentIndex else null,
                            previousProgressMeters = if (prevCursor?.isSnappedToRoute == true) prevCursor.distanceAlongRouteMeters else null
                        )
                    } else {
                        null
                    }

                    val snapped = snap != null && snap.distanceToRouteMeters <= SNAP_TO_ROUTE_MAX_METERS
                    val cursor = CursorDisplayPosition(
                        latitude = if (snapped) snap!!.snappedLat else pos.latitude,
                        longitude = if (snapped) snap!!.snappedLon else pos.longitude,
                        bearingDegrees = when {
                            snapped -> snap!!.segmentBearingDegrees
                            pos.bearing != 0f -> pos.bearing
                            else -> state.cursorPosition?.bearingDegrees ?: 0f
                        },
                        accuracyMeters = pos.accuracyMeters,
                        isSnappedToRoute = snapped,
                        nearestSegmentIndex = snap?.nearestSegmentIndex ?: 0,
                        distanceAlongRouteMeters = snap?.distanceAlongRouteMeters ?: 0.0
                    )

                    val updatedNav = updateNavigationProgress(state.copy(
                        gpsPosition = pos,
                        cursorPosition = cursor
                    ), pos, snap)

                    // Check silent auto-rerouting when following GPS (never interrupts or notifies driver)
                    val dest = updatedNav.destination
                    if (route != null &&
                        dest != null &&
                        updatedNav.manualOrigin == null &&
                        updatedNav.initState is OfflineInitState.Ready &&
                        snap != null &&
                        !snap.isNearOffRoadTail &&
                        snap.distanceToRouteMeters > OFF_ROUTE_REROUTE_THRESHOLD_METERS &&
                        pos.accuracyMeters <= MAX_ACCURACY_FOR_REROUTE_METERS
                    ) {
                        val distToDest = OfflineLocationTracker.haversineMeters(
                            pos.latitude, pos.longitude, dest.latitude, dest.longitude
                        )
                        val now = SystemClock.elapsedRealtime()
                        if (distToDest > 50.0 &&
                            !isAutoRerouting &&
                            (now - lastRerouteElapsedMs) >= SILENT_REROUTE_COOLDOWN_MS
                        ) {
                            shouldSilentReroute = true
                            rerouteDest = dest
                            rerouteProfile = updatedNav.selectedProfile
                        }
                    }

                    updatedNav
                }

                if (shouldSilentReroute && rerouteDest != null) {
                    triggerSilentAutoReroute(pos, rerouteDest!!, rerouteProfile)
                }

                // Periodically refresh current street name pill while moving
                val nowRoad = SystemClock.elapsedRealtime()
                if (nowRoad - lastRoadLookupElapsedMs > 7000L) {
                    lastRoadLookupElapsedMs = nowRoad
                    viewModelScope.launch {
                        val name = mapDataManager.findNearestRoadName(pos.latitude, pos.longitude)
                        if (!name.isNullOrBlank()) {
                            _uiState.update { it.copy(currentRoadName = name) }
                        }
                    }
                }
            }
        }
    }

    private fun triggerSilentAutoReroute(
        gps: GpsPosition,
        dest: SelectedPoint,
        profile: RouteProfile
    ) {
        if (isAutoRerouting) return
        isAutoRerouting = true
        lastRerouteElapsedMs = SystemClock.elapsedRealtime()
        rerouteJob?.cancel()
        rerouteJob = viewModelScope.launch {
            try {
                val result = mapDataManager.calculateRoute(
                    startLat = gps.latitude,
                    startLon = gps.longitude,
                    endLat = dest.latitude,
                    endLon = dest.longitude,
                    profile = profile
                )
                result.onSuccess { newRoute ->
                    // Silently swap route without any dialog, toast, or alert to the driver
                    _uiState.update { state ->
                        if (state.destination?.latitude != dest.latitude ||
                            state.destination.longitude != dest.longitude
                        ) {
                            return@update state
                        }
                        val snap = RouteGeometry.analyzePositionOnRoute(gps.latitude, gps.longitude, newRoute)
                        val snapped = snap.distanceToRouteMeters <= SNAP_TO_ROUTE_MAX_METERS
                        val cursor = CursorDisplayPosition(
                            latitude = if (snapped) snap.snappedLat else gps.latitude,
                            longitude = if (snapped) snap.snappedLon else gps.longitude,
                            bearingDegrees = if (snapped) snap.segmentBearingDegrees else (state.cursorPosition?.bearingDegrees ?: gps.bearing),
                            accuracyMeters = gps.accuracyMeters,
                            isSnappedToRoute = snapped,
                            nearestSegmentIndex = snap.nearestSegmentIndex,
                            distanceAlongRouteMeters = snap.distanceAlongRouteMeters
                        )
                        state.copy(
                            routeResult = newRoute,
                            routeError = null,
                            cursorPosition = cursor,
                            activeStepIndex = 0,
                            distanceToNextTurnMeters = newRoute.steps.firstOrNull()?.distanceMeters ?: 0.0,
                            remainingDistanceMeters = newRoute.totalDistanceMeters,
                            remainingTimeMillis = newRoute.totalTimeMillis
                        )
                    }
                }
            } finally {
                isAutoRerouting = false
            }
        }
    }

    private fun updateNavigationProgress(
        state: MainUiState,
        pos: GpsPosition,
        snap: com.example.util.RouteSnapAnalysis?
    ): MainUiState {
        val route = state.routeResult ?: return state
        if (route.steps.isEmpty()) return state

        var currentIdx = state.activeStepIndex.coerceIn(0, route.steps.lastIndex)
        // Advance forward if driver passed upcoming step maneuver point
        for (lookAhead in (currentIdx + 1)..route.steps.lastIndex) {
            val candidateStep = route.steps[lookAhead]
            val distToStep = OfflineLocationTracker.haversineMeters(
                pos.latitude, pos.longitude, candidateStep.lat, candidateStep.lon
            )
            if (distToStep < 30.0) {
                currentIdx = lookAhead
            } else if (lookAhead == currentIdx + 1) {
                break
            }
        }

        val targetStep = if (currentIdx < route.steps.lastIndex) {
            route.steps[currentIdx + 1]
        } else {
            route.steps.last()
        }
        val distToTurn = OfflineLocationTracker.haversineMeters(
            pos.latitude, pos.longitude, targetStep.lat, targetStep.lon
        )

        val remainingDist = snap?.remainingDistanceMeters ?: route.totalDistanceMeters
        val ratio = if (route.totalDistanceMeters > 1.0) {
            (remainingDist / route.totalDistanceMeters).coerceIn(0.0, 1.0)
        } else {
            1.0
        }
        val remainingTime = (route.totalTimeMillis * ratio).toLong().coerceAtLeast(60_000L)

        val currentRoad = route.steps.getOrNull(currentIdx)?.streetName?.takeIf { it.isNotBlank() }
            ?: state.currentRoadName

        return state.copy(
            activeStepIndex = currentIdx,
            distanceToNextTurnMeters = distToTurn,
            remainingDistanceMeters = remainingDist,
            remainingTimeMillis = remainingTime,
            currentRoadName = currentRoad
        )
    }

    fun onIncomingTextOrUri(rawText: String?) {
        if (rawText.isNullOrBlank()) return
        viewModelScope.launch {
            when (val parsed = GoogleMapsLinkResolver.resolve(rawText)) {
                is CoordinateParseResult.Success -> {
                    val label = parsed.label ?: "Ubicación (${CoordinateParser.formatCoordinates(parsed.latitude, parsed.longitude)})"
                    setDestinationPoint(
                        lat = parsed.latitude,
                        lon = parsed.longitude,
                        label = label,
                        autoRoute = true
                    )
                    if (!parsed.isInsideElSalvador) {
                        _uiState.update {
                            it.copy(
                                pendingMessage = "Estas coordenadas parecen estar fuera de El Salvador; el mapa solo cubre el territorio salvadoreño."
                            )
                        }
                    }
                }
                is CoordinateParseResult.ShortenedLinkWarning -> {
                    _uiState.update {
                        it.copy(
                            pendingMessage = "Enlace corto de Google Maps detectado. Sin internet no puede expandirse: en Google Maps toca el pin y copia las coordenadas (ej. 13.6929, -89.2182) o comparte el enlace completo."
                        )
                    }
                }
                CoordinateParseResult.NoMatch -> {
                    _uiState.update {
                        it.copy(pendingMessage = "No se encontraron coordenadas válidas en el enlace o texto recibido.")
                    }
                }
                CoordinateParseResult.Empty -> Unit
            }
        }
    }

    fun setDestinationPoint(
        lat: Double,
        lon: Double,
        label: String? = null,
        autoRoute: Boolean = true
    ) {
        val displayLabel = label?.takeIf { it.isNotBlank() }
            ?: "Destino (${CoordinateParser.formatCoordinates(lat, lon)})"
        val point = SelectedPoint(
            latitude = lat,
            longitude = lon,
            label = displayLabel
        )
        _uiState.update {
            it.copy(
                destination = point,
                isCrosshairMode = false,
                showSearchSheet = false,
                showMenuSheet = false,
                routeError = null,
                isNavigating = false,
                isFollowingGps = false,
                mapFocusRequest = MapFocusTarget(lat, lon, zoom = 15.toByte())
            )
        }
        viewModelScope.launch {
            val roadName = mapDataManager.findNearestRoadName(lat, lon)
            if (roadName != null) {
                _uiState.update { state ->
                    if (state.destination?.latitude == lat && state.destination.longitude == lon) {
                        state.copy(destination = state.destination.copy(nearestRoad = roadName))
                    } else state
                }
            }
        }
        if (autoRoute) {
            calculateCurrentRoute()
        }
    }

    fun setOriginPoint(lat: Double, lon: Double, label: String? = null) {
        val displayLabel = label?.takeIf { it.isNotBlank() }
            ?: "Origen (${CoordinateParser.formatCoordinates(lat, lon)})"
        _uiState.update {
            it.copy(
                manualOrigin = SelectedPoint(lat, lon, displayLabel),
                isCrosshairMode = false,
                routeError = null,
                isFollowingGps = false,
                mapFocusRequest = MapFocusTarget(lat, lon, zoom = 14.toByte())
            )
        }
        if (_uiState.value.destination != null) {
            calculateCurrentRoute()
        }
    }

    fun useLiveGpsAsOrigin() {
        _uiState.update { it.copy(manualOrigin = null, isFollowingGps = true) }
        val gps = _uiState.value.gpsPosition
        if (gps != null) {
            _uiState.update {
                it.copy(mapFocusRequest = MapFocusTarget(gps.latitude, gps.longitude, zoom = 16.toByte()))
            }
            if (_uiState.value.destination != null) {
                calculateCurrentRoute()
            }
        } else {
            _uiState.update {
                it.copy(
                    pendingMessage = "Esperando señal GPS… También puedes fijar un punto de inicio manual en el mapa."
                )
            }
        }
    }

    fun onUserPannedMap() {
        if (_uiState.value.isFollowingGps) {
            _uiState.update { it.copy(isFollowingGps = false) }
        }
    }

    fun centerOnMyLocationOrDefault() {
        val gps = _uiState.value.gpsPosition
        if (gps != null) {
            _uiState.update {
                it.copy(
                    isFollowingGps = true,
                    mapFocusRequest = MapFocusTarget(gps.latitude, gps.longitude, zoom = 17.toByte())
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    mapFocusRequest = MapFocusTarget(
                        ElSalvadorPlaces.SAN_SALVADOR_LAT,
                        ElSalvadorPlaces.SAN_SALVADOR_LON,
                        zoom = 13.toByte()
                    ),
                    pendingMessage = "Sin señal GPS todavía. Centrado en San Salvador."
                )
            }
        }
    }

    fun selectProfile(profile: RouteProfile) {
        _uiState.update { it.copy(selectedProfile = profile, isNavigating = false) }
        if (_uiState.value.destination != null) {
            calculateCurrentRoute()
        }
    }

    fun toggleRouteProfile() {
        val next = if (_uiState.value.selectedProfile == RouteProfile.CAR) {
            RouteProfile.FOOT
        } else {
            RouteProfile.CAR
        }
        selectProfile(next)
    }

    fun calculateCurrentRoute() {
        val state = _uiState.value
        val dest = state.destination ?: return
        if (state.initState !is OfflineInitState.Ready) return

        val originLatLon: Pair<Double, Double>? = when {
            state.manualOrigin != null -> state.manualOrigin.latitude to state.manualOrigin.longitude
            state.gpsPosition != null -> state.gpsPosition.latitude to state.gpsPosition.longitude
            else -> null
        }

        if (originLatLon == null) {
            _uiState.update {
                it.copy(
                    routeError = "Activa el GPS o toca \"Fijar inicio\" (o usa San Salvador como inicio) para calcular la ruta sin señal."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isRouting = true, routeError = null) }
            val result = mapDataManager.calculateRoute(
                startLat = originLatLon.first,
                startLon = originLatLon.second,
                endLat = dest.latitude,
                endLon = dest.longitude,
                profile = state.selectedProfile
            )
            result.fold(
                onSuccess = { route ->
                    _uiState.update { current ->
                        val gps = current.gpsPosition
                        val snap = if (gps != null) {
                            RouteGeometry.analyzePositionOnRoute(gps.latitude, gps.longitude, route)
                        } else null
                        val snapped = snap != null && snap.distanceToRouteMeters <= SNAP_TO_ROUTE_MAX_METERS
                        val updatedCursor = if (gps != null) {
                            CursorDisplayPosition(
                                latitude = if (snapped) snap!!.snappedLat else gps.latitude,
                                longitude = if (snapped) snap!!.snappedLon else gps.longitude,
                                bearingDegrees = if (snapped) snap!!.segmentBearingDegrees else (current.cursorPosition?.bearingDegrees ?: gps.bearing),
                                accuracyMeters = gps.accuracyMeters,
                                isSnappedToRoute = snapped,
                                nearestSegmentIndex = snap?.nearestSegmentIndex ?: 0,
                                distanceAlongRouteMeters = snap?.distanceAlongRouteMeters ?: 0.0
                            )
                        } else current.cursorPosition

                        val focusLat = updatedCursor?.latitude ?: route.origin.latitude
                        val focusLon = updatedCursor?.longitude ?: route.origin.longitude

                        current.copy(
                            isRouting = false,
                            routeResult = route,
                            routeError = null,
                            isNavigating = true,
                            isFollowingGps = current.manualOrigin == null,
                            cursorPosition = updatedCursor,
                            activeStepIndex = 0,
                            distanceToNextTurnMeters = route.steps.firstOrNull()?.distanceMeters ?: 0.0,
                            remainingDistanceMeters = route.totalDistanceMeters,
                            remainingTimeMillis = route.totalTimeMillis,
                            currentRoadName = route.steps.firstOrNull()?.streetName?.takeIf { it.isNotBlank() }
                                ?: current.currentRoadName,
                            mapFocusRequest = MapFocusTarget(focusLat, focusLon, zoom = 17.toByte())
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isRouting = false,
                            routeResult = null,
                            routeError = err.localizedMessage ?: "No se pudo calcular la ruta."
                        )
                    }
                }
            )
        }
    }

    fun startSanSalvadorFallbackOriginAndRoute() {
        setOriginPoint(
            ElSalvadorPlaces.SAN_SALVADOR_LAT,
            ElSalvadorPlaces.SAN_SALVADOR_LON,
            "San Salvador (Inicio manual)"
        )
    }

    fun startNavigation() {
        val route = _uiState.value.routeResult ?: return
        val cursor = _uiState.value.cursorPosition
        val focusLat = cursor?.latitude ?: _uiState.value.gpsPosition?.latitude ?: route.origin.latitude
        val focusLon = cursor?.longitude ?: _uiState.value.gpsPosition?.longitude ?: route.origin.longitude
        _uiState.update {
            it.copy(
                isNavigating = true,
                isFollowingGps = true,
                activeStepIndex = 0,
                remainingDistanceMeters = route.totalDistanceMeters,
                remainingTimeMillis = route.totalTimeMillis,
                mapFocusRequest = MapFocusTarget(focusLat, focusLon, zoom = 17.toByte())
            )
        }
    }

    fun stopNavigation() {
        _uiState.update { it.copy(isNavigating = false) }
    }

    fun clearRoute() {
        rerouteJob?.cancel()
        isAutoRerouting = false
        _uiState.update {
            it.copy(
                destination = null,
                routeResult = null,
                routeError = null,
                isNavigating = false,
                showStepsSheet = false,
                showRouteOptionsSheet = false,
                remainingDistanceMeters = 0.0,
                remainingTimeMillis = 0L
            )
        }
    }

    fun toggleNightMode() {
        _uiState.update { it.copy(isNightMode = !it.isNightMode) }
    }

    fun toggleCrosshairMode() {
        _uiState.update { it.copy(isCrosshairMode = !it.isCrosshairMode, showMenuSheet = false) }
    }

    fun setSearchSheetVisible(visible: Boolean) {
        _uiState.update {
            if (visible) it.copy(showSearchSheet = true)
            else it.copy(showSearchSheet = false, searchQuery = "", offlineSearchResults = emptyList())
        }
    }

    fun onSearchQueryChanged(rawQuery: String) {
        _uiState.update { it.copy(searchQuery = rawQuery) }
        searchJob?.cancel()
        if (rawQuery.trim().length < 2) {
            _uiState.update { it.copy(offlineSearchResults = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            delay(80L)
            val state = _uiState.value
            val refLat = state.gpsPosition?.latitude
                ?: state.manualOrigin?.latitude
                ?: ElSalvadorPlaces.SAN_SALVADOR_LAT
            val refLon = state.gpsPosition?.longitude
                ?: state.manualOrigin?.longitude
                ?: ElSalvadorPlaces.SAN_SALVADOR_LON
            val matches = runCatching {
                offlinePlaceRepository.search(
                    rawQuery = rawQuery,
                    userLat = refLat,
                    userLon = refLon,
                    maxResults = 30
                )
            }.getOrDefault(emptyList())
            _uiState.update { it.copy(offlineSearchResults = matches) }
        }
    }

    fun setMenuSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showMenuSheet = visible) }
    }

    fun setRouteOptionsSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showRouteOptionsSheet = visible) }
    }

    fun setStepsSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showStepsSheet = visible, showRouteOptionsSheet = false) }
    }

    fun setSaveDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showSaveDialog = visible, showRouteOptionsSheet = false) }
    }

    fun dismissPendingMessage() {
        _uiState.update { it.copy(pendingMessage = null) }
    }

    fun saveCurrentDestination(name: String, notes: String, category: String) {
        val dest = _uiState.value.destination ?: return
        viewModelScope.launch {
            repository.savePlace(
                name = name.trim().ifEmpty { dest.label },
                notes = notes.trim(),
                category = category,
                latitude = dest.latitude,
                longitude = dest.longitude
            )
            _uiState.update {
                it.copy(
                    destination = dest.copy(label = name.trim().ifEmpty { dest.label }),
                    showSaveDialog = false,
                    pendingMessage = "Guardado en tus lugares offline."
                )
            }
        }
    }

    fun saveCustomPlace(
        name: String,
        notes: String,
        category: String,
        latitude: Double,
        longitude: Double
    ) {
        viewModelScope.launch {
            repository.savePlace(
                name = name.trim(),
                notes = notes.trim(),
                category = category,
                latitude = latitude,
                longitude = longitude
            )
        }
    }

    fun openSavedPlace(place: SavedPlaceEntity) {
        viewModelScope.launch { repository.markUsed(place.id) }
        setDestinationPoint(place.latitude, place.longitude, place.name, autoRoute = true)
    }

    fun deleteSavedPlace(id: Int) {
        viewModelScope.launch { repository.deleteById(id) }
    }

    fun focusOnStep(lat: Double, lon: Double) {
        _uiState.update {
            it.copy(
                showStepsSheet = false,
                isFollowingGps = false,
                mapFocusRequest = MapFocusTarget(lat, lon, zoom = 17.toByte())
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        mapDataManager.close()
    }

    companion object {
        private const val SNAP_TO_ROUTE_MAX_METERS = 35.0
        private const val OFF_ROUTE_REROUTE_THRESHOLD_METERS = 45.0
        private const val MAX_ACCURACY_FOR_REROUTE_METERS = 55f
        private const val SILENT_REROUTE_COOLDOWN_MS = 4500L

        fun formatDistance(meters: Double): String {
            return if (meters < 1000) {
                "${meters.roundToInt()} m"
            } else {
                String.format(java.util.Locale.US, "%.1f km", meters / 1000.0)
            }
        }

        fun formatDuration(millis: Long): String {
            val totalMin = (millis / 60_000L).coerceAtLeast(1L)
            val hours = totalMin / 60L
            val mins = totalMin % 60L
            return when {
                hours > 0 && mins > 0 -> "$hours:${String.format(java.util.Locale.US, "%02d", mins)} h"
                hours > 0 -> "${hours} h"
                else -> "$mins min"
            }
        }

        fun formatClockEta(remainingMillis: Long): String {
            val arrival = java.util.Date(System.currentTimeMillis() + remainingMillis.coerceAtLeast(60_000L))
            return java.text.SimpleDateFormat("h:mm a", java.util.Locale.US).format(arrival)
        }
    }
}
