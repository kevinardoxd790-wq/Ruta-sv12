package com.example

import com.example.map.OfflineRouteResult
import com.example.map.RouteProfile
import com.example.util.RouteGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mapsforge.core.model.LatLong

class RouteGeometryTest {

    private fun sampleRoute(): OfflineRouteResult {
        val p1 = LatLong(13.7000, -89.2200)
        val p2 = LatLong(13.7050, -89.2200) // ~555m North
        val p3 = LatLong(13.7050, -89.2150) // ~540m East
        return OfflineRouteResult(
            profile = RouteProfile.CAR,
            origin = p1,
            destination = p3,
            routePoints = listOf(p1, p2, p3),
            offRoadTailPoints = emptyList(),
            offRoadTailMeters = 0.0,
            totalDistanceMeters = 1095.0,
            totalTimeMillis = 90_000L,
            steps = emptyList()
        )
    }

    @Test
    fun snapsMovingCursorAlongRouteAndDetectsOffRouteDeviation() {
        val route = sampleRoute()

        // Vehicle moving ~10m off the Northbound segment -> should snap onto route (~0 deg North)
        val onRoute = RouteGeometry.analyzePositionOnRoute(13.7025, -89.2199, route)
        assertTrue("Expected < 20m to route, got ${onRoute.distanceToRouteMeters}", onRoute.distanceToRouteMeters < 20.0)
        assertTrue("Expected North bearing near 0, got ${onRoute.segmentBearingDegrees}", onRoute.segmentBearingDegrees < 10f || onRoute.segmentBearingDegrees > 350f)
        assertTrue("Expected remaining distance > 700m", onRoute.remainingDistanceMeters > 700.0)
        assertTrue("Expected positive distanceAlongRouteMeters", onRoute.distanceAlongRouteMeters > 200.0)

        // Vehicle turns ~120m away from route -> should exceed 45m deviation threshold for silent reroute
        val offRoute = RouteGeometry.analyzePositionOnRoute(13.7025, -89.2212, route)
        assertTrue("Expected > 45m off-route, got ${offRoute.distanceToRouteMeters}", offRoute.distanceToRouteMeters > 45.0)
    }

    @Test
    fun interpolatesSamplePointAndTrimsTraversedPolylineCleanly() {
        val route = sampleRoute()
        val cumulative = RouteGeometry.computeCumulativeDistances(route.routePoints)
        assertEquals(3, cumulative.size)
        val seg1Len = cumulative[1]
        val totalLen = cumulative[2]

        // Sample midway along first segment
        val midFirst = RouteGeometry.samplePointAtDistance(route.routePoints, cumulative, seg1Len * 0.5, lookAheadMetersForBearing = 8.0)
        assertEquals(0, midFirst.segmentIndex)
        assertTrue(midFirst.latitude > 13.7020 && midFirst.latitude < 13.7030)

        // Trim polyline from midFirst -> starts at midFirst and keeps p2 and p3
        val trimmedFirst = RouteGeometry.buildRemainingPolylineFromDistance(route.routePoints, cumulative, seg1Len * 0.5)
        assertEquals(3, trimmedFirst.size)
        assertEquals(midFirst.latitude, trimmedFirst[0].latitude, 1e-6)
        assertEquals(route.routePoints[1].latitude, trimmedFirst[1].latitude, 1e-6)
        assertEquals(route.routePoints[2].longitude, trimmedFirst[2].longitude, 1e-6)

        // Sample midway along second segment (after corner) -> trimmed polyline has only 2 points: interpolated start & destination
        val midSecondDist = seg1Len + (totalLen - seg1Len) * 0.5
        val trimmedSecond = RouteGeometry.buildRemainingPolylineFromDistance(route.routePoints, cumulative, midSecondDist)
        assertEquals(2, trimmedSecond.size)
        assertTrue(trimmedSecond[0].longitude > -89.2185 && trimmedSecond[0].longitude < -89.2165)
        assertEquals(route.routePoints[2].longitude, trimmedSecond[1].longitude, 1e-6)

        // At destination -> empty remaining polyline
        val atEnd = RouteGeometry.buildRemainingPolylineFromDistance(route.routePoints, cumulative, totalLen)
        assertTrue(atEnd.isEmpty())
    }

    @Test
    fun preventsMicroBackwardJitterWhenTrackingProgressMonotonically() {
        val route = sampleRoute()
        val first = RouteGeometry.analyzePositionOnRoute(13.7030, -89.2200, route)
        // Simulate 3m GPS jitter slightly behind first reading
        val jitterBack = RouteGeometry.analyzePositionOnRoute(
            lat = 13.702975,
            lon = -89.2200,
            route = route,
            previousSegmentIndex = first.nearestSegmentIndex,
            previousProgressMeters = first.distanceAlongRouteMeters
        )
        assertTrue(
            "Progress should not step backward on <6m GPS jitter",
            jitterBack.distanceAlongRouteMeters >= first.distanceAlongRouteMeters
        )
    }
}
