package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ElSalvadorPlaces
import com.example.data.local.AppDatabase
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
import kotlinx.coroutines.Job
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

data class MainUiState(
    val initState: OfflineInitState = OfflineInitState.Preparing("Iniciando…", 0.05f),
    val hasLocationPermission: Boolean = false,
    val gpsPosition: GpsPosition? = null,
    val manualOrigin: SelectedPoint? = null,
    val destination: SelectedPoint? = null,
    val selectedProfile: RouteProfile = RouteProfile.CAR,
    val routeResult: OfflineRouteResult? = null,
    val isRouting: Boolean = false,
    val routeError: String? = null,
    val isNavigating: Boolean = false,
    val activeStepIndex: Int = 0,
    val distanceToNextTurnMeters: Double = 0.0,
    val isNightMode: Boolean = false,
    val isCrosshairMode: Boolean = false,
    val showStepsSheet: Boolean = false,
    val showSearchSheet: Boolean = false,
    val showSaveDialog: Boolean = false,
    val pendingMessage: String? = null,
    val mapFocusRequest: MapFocusTarget? = null
)

data class MapFocusTarget(
    val latitude: Double,
    val longitude: Double,
    val zoom: Byte? = null,
    val nonce: Long = System.nanoTime()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val mapDataManager = OfflineMapDataManager(application)
    private val repository = SavedPlaceRepository(
        AppDatabase.getInstance(application).savedPlaceDao()
    )

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val savedPlaces: StateFlow<List<SavedPlaceEntity>> = repository.allPlaces.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private var locationJob: Job? = null

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
                _uiState.update { state ->
                    val next = state.copy(gpsPosition = pos)
                    updateNavigationProgress(next, pos)
                }
            }
        }
    }

    private fun updateNavigationProgress(state: MainUiState, pos: GpsPosition?): MainUiState {
        if (!state.isNavigating || pos == null) return state
        val route = state.routeResult ?: return state
        if (route.steps.isEmpty()) return state

        var currentIdx = state.activeStepIndex.coerceIn(0, route.steps.lastIndex)
        if (currentIdx < route.steps.lastIndex) {
            val nextStep = route.steps[currentIdx + 1]
            val distToNext = OfflineLocationTracker.haversineMeters(
                pos.latitude, pos.longitude, nextStep.lat, nextStep.lon
            )
            if (distToNext < 28.0) {
                currentIdx += 1
            }
        }

        val targetStep = if (currentIdx < route.steps.lastIndex) {
            route.steps[currentIdx + 1]
        } else {
            route.steps.last()
        }
        val dist = OfflineLocationTracker.haversineMeters(
            pos.latitude, pos.longitude, targetStep.lat, targetStep.lon
        )

        return state.copy(
            activeStepIndex = currentIdx,
            distanceToNextTurnMeters = dist
        )
    }

    fun onIncomingTextOrUri(rawText: String?) {
        when (val parsed = CoordinateParser.parse(rawText)) {
            is CoordinateParseResult.Success -> {
                val label = parsed.label ?: "Punto recibido (${CoordinateParser.formatCoordinates(parsed.latitude, parsed.longitude)})"
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
                        pendingMessage = "Los enlaces cortos (${parsed.url}) no contienen coordenadas y requieren internet para abrirse. Pide que te envíen los números de latitud y longitud o una ubicación geo:."
                    )
                }
            }
            CoordinateParseResult.NoMatch -> {
                _uiState.update {
                    it.copy(pendingMessage = "No se encontraron coordenadas válidas en el texto recibido.")
                }
            }
            CoordinateParseResult.Empty -> Unit
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
                routeError = null,
                isNavigating = false,
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
                mapFocusRequest = MapFocusTarget(lat, lon, zoom = 14.toByte())
            )
        }
        if (_uiState.value.destination != null) {
            calculateCurrentRoute()
        }
    }

    fun useLiveGpsAsOrigin() {
        _uiState.update { it.copy(manualOrigin = null) }
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

    fun centerOnMyLocationOrDefault() {
        val gps = _uiState.value.gpsPosition
        if (gps != null) {
            _uiState.update {
                it.copy(mapFocusRequest = MapFocusTarget(gps.latitude, gps.longitude, zoom = 16.toByte()))
            }
        } else {
            _uiState.update {
                it.copy(
                    mapFocusRequest = MapFocusTarget(
                        ElSalvadorPlaces.SAN_SALVADOR_LAT,
                        ElSalvadorPlaces.SAN_SALVADOR_LON,
                        zoom = 12.toByte()
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
                    _uiState.update {
                        it.copy(
                            isRouting = false,
                            routeResult = route,
                            routeError = null,
                            activeStepIndex = 0,
                            distanceToNextTurnMeters = route.steps.firstOrNull()?.distanceMeters ?: 0.0
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
        val focusLat = _uiState.value.gpsPosition?.latitude ?: route.origin.latitude
        val focusLon = _uiState.value.gpsPosition?.longitude ?: route.origin.longitude
        _uiState.update {
            it.copy(
                isNavigating = true,
                activeStepIndex = 0,
                mapFocusRequest = MapFocusTarget(focusLat, focusLon, zoom = 17.toByte())
            )
        }
    }

    fun stopNavigation() {
        _uiState.update { it.copy(isNavigating = false) }
    }

    fun clearRoute() {
        _uiState.update {
            it.copy(
                destination = null,
                routeResult = null,
                routeError = null,
                isNavigating = false,
                showStepsSheet = false
            )
        }
    }

    fun toggleNightMode() {
        _uiState.update { it.copy(isNightMode = !it.isNightMode) }
    }

    fun toggleCrosshairMode() {
        _uiState.update { it.copy(isCrosshairMode = !it.isCrosshairMode) }
    }

    fun setSearchSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showSearchSheet = visible) }
    }

    fun setStepsSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showStepsSheet = visible) }
    }

    fun setSaveDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showSaveDialog = visible) }
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
                mapFocusRequest = MapFocusTarget(lat, lon, zoom = 17.toByte())
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        mapDataManager.close()
    }

    companion object {
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
                hours > 0 && mins > 0 -> "${hours} h ${mins} min"
                hours > 0 -> "${hours} h"
                else -> "$mins min"
            }
        }
    }
}
