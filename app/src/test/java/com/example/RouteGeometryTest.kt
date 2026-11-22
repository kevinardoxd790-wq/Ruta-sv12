package com.example

import com.example.map.OfflineRouteResult
import com.example.map.RouteProfile
import com.example.util.RouteGeometry
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

        // Vehicle turns ~120m away from route -> should exceed 45m deviation threshold for silent reroute
        val offRoute = RouteGeometry.analyzePositionOnRoute(13.7025, -89.2212, route)
        assertTrue("Expected > 45m off-route, got ${offRoute.distanceToRouteMeters}", offRoute.distanceToRouteMeters > 45.0)
    }
}
