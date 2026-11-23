package com.example.util

import com.example.location.OfflineLocationTracker
import com.example.map.OfflineRouteResult
import org.mapsforge.core.model.LatLong
import kotlin.math.abs
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
    val isNearOffRoadTail: Boolean,
    val distanceAlongRouteMeters: Double = 0.0
)

data class RouteSample(
    val latitude: Double,
    val longitude: Double,
    val segmentIndex: Int,
    val bearingDegrees: Float,
    val distanceAlongRouteMeters: Double
)

object RouteGeometry {

    fun computeCumulativeDistances(points: List<LatLong>): DoubleArray {
        if (points.isEmpty()) return DoubleArray(0)
        val cumulative = DoubleArray(points.size)
        cumulative[0] = 0.0
        for (i in 1 until points.size) {
            val segLen = OfflineLocationTracker.haversineMeters(
                points[i - 1].latitude,
                points[i - 1].longitude,
                points[i].latitude,
                points[i].longitude
            )
            cumulative[i] = cumulative[i - 1] + segLen
        }
        return cumulative
    }

    fun analyzePositionOnRoute(
        lat: Double,
        lon: Double,
        route: OfflineRouteResult,
        previousSegmentIndex: Int? = null,
        previousProgressMeters: Double? = null
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
                isNearOffRoadTail = false,
                distanceAlongRouteMeters = 0.0
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
                isNearOffRoadTail = route.offRoadTailMeters > 25.0 && d < 60.0,
                distanceAlongRouteMeters = 0.0
            )
        }

        val cumulative = computeCumulativeDistances(points)
        val metersPerDegLat = 111_320.0
        val metersPerDegLon = (111_320.0 * cos(Math.toRadians(lat))).coerceAtLeast(1000.0)

        var bestDistSq = Double.MAX_VALUE
        var bestSnapLat = points[0].latitude
        var bestSnapLon = points[0].longitude
        var bestSegIdx = 0
        var bestT = 0.0
        var bestBearing = 0f

        var fwdDistSq = Double.MAX_VALUE
        var fwdSnapLat = points[0].latitude
        var fwdSnapLon = points[0].longitude
        var fwdSegIdx = -1
        var fwdT = 0.0
        var fwdBearing = 0f

        val minForwardSeg = previousSegmentIndex?.coerceIn(0, points.size - 2) ?: 0

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
                bestT = t
                bestSnapLat = a.latitude + t * (b.latitude - a.latitude)
                bestSnapLon = a.longitude + t * (b.longitude - a.longitude)
                bestBearing = bearingDegrees(a.latitude, a.longitude, b.latitude, b.longitude)
            }

            if (previousSegmentIndex != null && i >= minForwardSeg && distSq < fwdDistSq) {
                fwdDistSq = distSq
                fwdSegIdx = i
                fwdT = t
                fwdSnapLat = a.latitude + t * (b.latitude - a.latitude)
                fwdSnapLon = a.longitude + t * (b.longitude - a.longitude)
                fwdBearing = bearingDegrees(a.latitude, a.longitude, b.latitude, b.longitude)
            }
        }

        // Prefer monotonic forward progress when the forward candidate is still cleanly on-route
        // so intersections, loops, or GPS micro-jitter never flicker the trimmed route backward.
        val bestDist = sqrt(bestDistSq)
        val fwdDist = if (fwdSegIdx >= 0) sqrt(fwdDistSq) else Double.MAX_VALUE
        val useForward = previousSegmentIndex != null &&
            fwdSegIdx >= 0 &&
            bestSegIdx < minForwardSeg &&
            fwdDist <= 35.0 &&
            (fwdDist - bestDist) <= 15.0

        val chosenSegIdx = if (useForward) fwdSegIdx else bestSegIdx
        val chosenT = if (useForward) fwdT else bestT
        var chosenLat = if (useForward) fwdSnapLat else bestSnapLat
        var chosenLon = if (useForward) fwdSnapLon else bestSnapLon
        var chosenBearing = if (useForward) fwdBearing else bestBearing
        val chosenDist = if (useForward) fwdDist else bestDist

        val segLength = cumulative[chosenSegIdx + 1] - cumulative[chosenSegIdx]
        var progressMeters = cumulative[chosenSegIdx] + segLength * chosenT

        // Prevent tiny backward jitter (< 6m) when already snapped to route
        if (previousProgressMeters != null &&
            chosenDist <= 35.0 &&
            progressMeters < previousProgressMeters &&
            (previousProgressMeters - progressMeters) < 6.0
        ) {
            progressMeters = previousProgressMeters
            val clampedSample = samplePointAtDistance(points, cumulative, progressMeters, 8.0)
            chosenLat = clampedSample.latitude
            chosenLon = clampedSample.longitude
            chosenBearing = clampedSample.bearingDegrees
        }

        val totalPolyDist = cumulative.last()
        val remaining = (totalPolyDist - progressMeters).coerceAtLeast(0.0) + route.offRoadTailMeters

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
            distanceToRouteMeters = chosenDist,
            snappedLat = chosenLat,
            snappedLon = chosenLon,
            segmentBearingDegrees = chosenBearing,
            nearestSegmentIndex = chosenSegIdx,
            remainingDistanceMeters = remaining,
            isNearOffRoadTail = nearTail,
            distanceAlongRouteMeters = progressMeters
        )
    }

    fun samplePointAtDistance(
        points: List<LatLong>,
        cumulativeDistances: DoubleArray,
        distanceMeters: Double,
        lookAheadMetersForBearing: Double = 8.0
    ): RouteSample {
        if (points.isEmpty() || cumulativeDistances.isEmpty()) {
            return RouteSample(0.0, 0.0, 0, 0f, 0.0)
        }
        if (points.size == 1 || cumulativeDistances.size == 1) {
            return RouteSample(points[0].latitude, points[0].longitude, 0, 0f, 0.0)
        }

        val total = cumulativeDistances.last()
        val d = distanceMeters.coerceIn(0.0, total)
        val segIdx = findSegmentIndexForDistance(cumulativeDistances, d)

        val segStart = cumulativeDistances[segIdx]
        val segEnd = cumulativeDistances[segIdx + 1]
        val segLen = segEnd - segStart
        val t = if (segLen > 1e-6) ((d - segStart) / segLen).coerceIn(0.0, 1.0) else 0.0

        val a = points[segIdx]
        val b = points[segIdx + 1]
        val lat = a.latitude + t * (b.latitude - a.latitude)
        val lon = a.longitude + t * (b.longitude - a.longitude)

        val bearing = if (lookAheadMetersForBearing > 0.0 && total > d + 0.5) {
            val lookDist = (d + lookAheadMetersForBearing).coerceAtMost(total)
            val lookSeg = findSegmentIndexForDistance(cumulativeDistances, lookDist)
            val lookStart = cumulativeDistances[lookSeg]
            val lookLen = cumulativeDistances[lookSeg + 1] - lookStart
            val lookT = if (lookLen > 1e-6) ((lookDist - lookStart) / lookLen).coerceIn(0.0, 1.0) else 0.0
            val la = points[lookSeg]
            val lb = points[lookSeg + 1]
            val lookLat = la.latitude + lookT * (lb.latitude - la.latitude)
            val lookLon = la.longitude + lookT * (lb.longitude - la.longitude)
            if (OfflineLocationTracker.haversineMeters(lat, lon, lookLat, lookLon) > 0.6) {
                bearingDegrees(lat, lon, lookLat, lookLon)
            } else {
                fallbackSegmentBearing(points, segIdx)
            }
        } else {
            fallbackSegmentBearing(points, segIdx)
        }

        return RouteSample(
            latitude = lat,
            longitude = lon,
            segmentIndex = segIdx,
            bearingDegrees = bearing,
            distanceAlongRouteMeters = d
        )
    }

    fun buildRemainingPolylineFromDistance(
        points: List<LatLong>,
        cumulativeDistances: DoubleArray,
        startDistanceMeters: Double
    ): List<LatLong> {
        if (points.size < 2 || cumulativeDistances.size != points.size) return emptyList()
        val total = cumulativeDistances.last()
        if (startDistanceMeters >= total - 0.3) return emptyList()

        val start = samplePointAtDistance(points, cumulativeDistances, startDistanceMeters, 0.0)
        val remaining = ArrayList<LatLong>(points.size - start.segmentIndex + 1)
        val startLatLong = LatLong(start.latitude, start.longitude)
        remaining.add(startLatLong)

        for (i in (start.segmentIndex + 1) until points.size) {
            val pt = points[i]
            val isDuplicateStart = abs(pt.latitude - start.latitude) < 1e-7 &&
                abs(pt.longitude - start.longitude) < 1e-7
            if (!isDuplicateStart || i == points.lastIndex) {
                remaining.add(pt)
            }
        }

        if (remaining.size < 2) {
            remaining.add(points.last())
        }
        return remaining
    }

    private fun findSegmentIndexForDistance(cumulativeDistances: DoubleArray, d: Double): Int {
        val lastSeg = cumulativeDistances.size - 2
        if (d <= 0.0) return 0
        if (d >= cumulativeDistances.last()) return lastSeg

        var low = 0
        var high = lastSeg
        while (low <= high) {
            val mid = (low + high) ushr 1
            val s = cumulativeDistances[mid]
            val e = cumulativeDistances[mid + 1]
            when {
                d < s -> high = mid - 1
                d > e -> low = mid + 1
                else -> return mid
            }
        }
        return low.coerceIn(0, lastSeg)
    }

    private fun fallbackSegmentBearing(points: List<LatLong>, segIdx: Int): Float {
        var idx = segIdx
        while (idx < points.size - 1) {
            val a = points[idx]
            val b = points[idx + 1]
            if (abs(a.latitude - b.latitude) > 1e-8 || abs(a.longitude - b.longitude) > 1e-8) {
                return bearingDegrees(a.latitude, a.longitude, b.latitude, b.longitude)
            }
            idx++
        }
        idx = segIdx
        while (idx >= 0 && idx < points.size - 1) {
            val a = points[idx]
            val b = points[idx + 1]
            if (abs(a.latitude - b.latitude) > 1e-8 || abs(a.longitude - b.longitude) > 1e-8) {
                return bearingDegrees(a.latitude, a.longitude, b.latitude, b.longitude)
            }
            idx--
        }
        return 0f
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
