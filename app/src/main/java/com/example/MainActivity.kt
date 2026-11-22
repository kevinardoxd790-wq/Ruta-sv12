package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.MainViewModel
import com.example.ui.screens.MapScreen
import com.example.ui.screens.SavedPlacesScreen
import com.example.ui.theme.RutaSVTheme
import kotlinx.serialization.Serializable
import org.mapsforge.map.android.graphics.AndroidGraphicFactory

@Serializable
data object RouteMap

@Serializable
data object RouteSavedPlaces

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidGraphicFactory.createInstance(application)
        enableEdgeToEdge()

        val hasLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        viewModel.onLocationPermissionChanged(hasLocation)

        handleIncomingIntent(intent)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val savedPlaces by viewModel.savedPlaces.collectAsStateWithLifecycle()
            val navController = rememberNavController()

            RutaSVTheme(darkTheme = uiState.isNightMode) {
                NavHost(
                    navController = navController,
                    startDestination = RouteMap
                ) {
                    composable<RouteMap> {
                        MapScreen(
                            uiState = uiState,
                            savedPlaces = savedPlaces,
                            onPermissionResult = viewModel::onLocationPermissionChanged,
                            onOpenSavedPlaces = { navController.navigate(RouteSavedPlaces) },
                            onShowSearchSheet = viewModel::setSearchSheetVisible,
                            onShowMenuSheet = viewModel::setMenuSheetVisible,
                            onShowStepsSheet = viewModel::setStepsSheetVisible,
                            onShowSaveDialog = viewModel::setSaveDialogVisible,
                            onToggleNightMode = viewModel::toggleNightMode,
                            onToggleCrosshairMode = viewModel::toggleCrosshairMode,
                            onSelectDestination = { lat, lon, label ->
                                viewModel.setDestinationPoint(lat, lon, label, autoRoute = true)
                            },
                            onSelectOrigin = viewModel::setOriginPoint,
                            onUseGpsOrigin = viewModel::useLiveGpsAsOrigin,
                            onUseSanSalvadorOrigin = viewModel::startSanSalvadorFallbackOriginAndRoute,
                            onCenterMyLocation = viewModel::centerOnMyLocationOrDefault,
                            onUserPanMap = viewModel::onUserPannedMap,
                            onSelectProfile = viewModel::selectProfile,
                            onToggleProfile = viewModel::toggleRouteProfile,
                            onCalculateRoute = viewModel::calculateCurrentRoute,
                            onStartNavigation = viewModel::startNavigation,
                            onStopNavigation = viewModel::stopNavigation,
                            onClearRoute = viewModel::clearRoute,
                            onSavePlace = viewModel::saveCurrentDestination,
                            onOpenSavedPlace = viewModel::openSavedPlace,
                            onFocusStep = viewModel::focusOnStep,
                            onPasteIncomingText = viewModel::onIncomingTextOrUri,
                            onRetryInit = viewModel::initializeOfflineData,
                            onDismissMessage = viewModel::dismissPendingMessage
                        )
                    }

                    composable<RouteSavedPlaces> {
                        SavedPlacesScreen(
                            places = savedPlaces,
                            onBack = { navController.popBackStack() },
                            onNavigateToPlace = { place ->
                                viewModel.openSavedPlace(place)
                                navController.popBackStack()
                            },
                            onDeletePlace = viewModel::deleteSavedPlace,
                            onAddManualPlace = viewModel::saveCustomPlace
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.dataString?.let { viewModel.onIncomingTextOrUri(it) }
            }
            Intent.ACTION_SEND -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)?.let { viewModel.onIncomingTextOrUri(it) }
            }
        }
    }
}
