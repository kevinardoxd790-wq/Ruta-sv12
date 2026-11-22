package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class GpsPosition(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val bearing: Float,
    val speedMps: Float,
    val provider: String,
    val timestamp: Long
)

object OfflineLocationTracker {

    @SuppressLint("MissingPermission")
    fun observeLocation(context: Context): Flow<GpsPosition?> = callbackFlow {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Emit best known last location immediately if available
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        val bestLast = providers
            .mapNotNull { provider ->
                runCatching {
                    if (locationManager.isProviderEnabled(provider)) {
                        locationManager.getLastKnownLocation(provider)
                    } else {
                        null
                    }
                }.getOrNull()
            }
            .maxByOrNull { it.time }

        var lastLocation: Location? = bestLast

        fun Location.toGpsPosition(): GpsPosition {
            val prev = lastLocation
            val computedBearing = when {
                hasBearing() && bearing != 0f -> bearing
                prev != null && haversineMeters(prev.latitude, prev.longitude, latitude, longitude) > 1.5 ->
                    com.example.util.RouteGeometry.bearingDegrees(
                        prev.latitude, prev.longitude, latitude, longitude
                    )
                else -> 0f
            }
            lastLocation = this
            return GpsPosition(
                latitude = latitude,
                longitude = longitude,
                accuracyMeters = accuracy,
                bearing = computedBearing,
                speedMps = if (hasSpeed()) speed else 0f,
                provider = provider ?: "gps",
                timestamp = time
            )
        }

        trySend(bestLast?.toGpsPosition())

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location.toGpsPosition())
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        val registered = mutableListOf<String>()
        for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
            runCatching {
                if (locationManager.allProviders.contains(provider)) {
                    locationManager.requestLocationUpdates(
                        provider,
                        1000L,
                        1f,
                        listener,
                        Looper.getMainLooper()
                    )
                    registered.add(provider)
                }
            }
        }

        awaitClose {
            runCatching { locationManager.removeUpdates(listener) }
        }
    }

    fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
}
