package com.example.util

import com.example.location.OfflineLocationTracker
import com.example.map.OfflineRouteResult
import org.mapsforge.core.model.LatLong
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class RouteSnapAnalysis(
    val distanceToRouteMeters: Double,
    val snappedLat: Double,
    val snappedLon: Double,
    val segmentBearingDegrees: Float,
    val nearestSegmentIndex: Int,
    val remainingDistanceMeters: Double,
    val isNearOffRoadTail: Boolean
)

object RouteGeometry {

    fun analyzePositionOnRoute(
        lat: Double,
        lon: Double,
        route: OfflineRouteResult
    ): RouteSnapAnalysis {
        val points = route.routePoints
        if (points.isEmpty()) {
            val direct = OfflineLocationTracker.haversineMeters(
                lat, lon, route.destination.latitude, route.destination.longitude
            )
            return RouteSnapAnalysis(
                distanceToRouteMeters = direct,
                snappedLat = lat,
                snappedLon = lon,
                segmentBearingDegrees = bearingDegrees(
                    lat, lon, route.destination.latitude, route.destination.longitude
                ),
                nearestSegmentIndex = 0,
                remainingDistanceMeters = direct,
                isNearOffRoadTail = false
            )
        }
        if (points.size == 1) {
            val d = OfflineLocationTracker.haversineMeters(
                lat, lon, points[0].latitude, points[0].longitude
            )
            return RouteSnapAnalysis(
                distanceToRouteMeters = d,
                snappedLat = points[0].latitude,
                snappedLon = points[0].longitude,
                segmentBearingDegrees = 0f,
                nearestSegmentIndex = 0,
                remainingDistanceMeters = d + route.offRoadTailMeters,
                isNearOffRoadTail = route.offRoadTailMeters > 25.0 && d < 60.0
            )
        }

        val metersPerDegLat = 111_320.0
        val metersPerDegLon = (111_320.0 * cos(Math.toRadians(lat))).coerceAtLeast(1000.0)

        var bestDistSq = Double.MAX_VALUE
        var bestSnapLat = points[0].latitude
        var bestSnapLon = points[0].longitude
        var bestSegIdx = 0
        var bestBearing = 0f

        for (i in 0 until points.size - 1) {
            val a = points[i]
            val b = points[i + 1]

            val ax = (a.longitude - lon) * metersPerDegLon
            val ay = (a.latitude - lat) * metersPerDegLat
            val bx = (b.longitude - lon) * metersPerDegLon
            val by = (b.latitude - lat) * metersPerDegLat

            val dx = bx - ax
            val dy = by - ay
            val lenSq = dx * dx + dy * dy
            val t = if (lenSq > 1e-6) {
                ((-ax * dx - ay * dy) / lenSq).coerceIn(0.0, 1.0)
            } else {
                0.0
            }

            val px = ax + t * dx
            val py = ay + t * dy
            val distSq = px * px + py * py

            if (distSq < bestDistSq) {
                bestDistSq = distSq
                bestSegIdx = i
                bestSnapLat = a.latitude + t * (b.latitude - a.latitude)
                bestSnapLon = a.longitude + t * (b.longitude - a.longitude)
                bestBearing = bearingDegrees(a.latitude, a.longitude, b.latitude, b.longitude)
            }
        }

        // Compute remaining distance along polyline from snapped point to end + off-road tail
        var remaining = OfflineLocationTracker.haversineMeters(
            bestSnapLat, bestSnapLon,
            points[bestSegIdx + 1].latitude, points[bestSegIdx + 1].longitude
        )
        for (i in (bestSegIdx + 1) until (points.size - 1)) {
            remaining += OfflineLocationTracker.haversineMeters(
                points[i].latitude, points[i].longitude,
                points[i + 1].latitude, points[i + 1].longitude
            )
        }
        remaining += route.offRoadTailMeters

        // Check if user is near the off-road tail segment (e.g., entering farm/rural terrain)
        val nearTail = if (route.offRoadTailPoints.size >= 2) {
            val tailStart = route.offRoadTailPoints.first()
            val tailEnd = route.offRoadTailPoints.last()
            val tailDist = distanceToSegmentMeters(lat, lon, tailStart, tailEnd, metersPerDegLat, metersPerDegLon)
            tailDist < 55.0
        } else {
            false
        }

        return RouteSnapAnalysis(
            distanceToRouteMeters = sqrt(bestDistSq),
            snappedLat = bestSnapLat,
            snappedLon = bestSnapLon,
            segmentBearingDegrees = bestBearing,
            nearestSegmentIndex = bestSegIdx,
            remainingDistanceMeters = remaining,
            isNearOffRoadTail = nearTail
        )
    }

    private fun distanceToSegmentMeters(
        lat: Double,
        lon: Double,
        a: LatLong,
        b: LatLong,
        metersPerDegLat: Double,
        metersPerDegLon: Double
    ): Double {
        val ax = (a.longitude - lon) * metersPerDegLon
        val ay = (a.latitude - lat) * metersPerDegLat
        val bx = (b.longitude - lon) * metersPerDegLon
        val by = (b.latitude - lat) * metersPerDegLat
        val dx = bx - ax
        val dy = by - ay
        val lenSq = dx * dx + dy * dy
        val t = if (lenSq > 1e-6) ((-ax * dx - ay * dy) / lenSq).coerceIn(0.0, 1.0) else 0.0
        val px = ax + t * dx
        val py = ay + t * dy
        return sqrt(px * px + py * py)
    }

    fun bearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dLambda = Math.toRadians(lon2 - lon1)
        val y = sin(dLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLambda)
        val theta = Math.toDegrees(atan2(y, x))
        return ((theta + 360.0) % 360.0).toFloat()
    }
}
