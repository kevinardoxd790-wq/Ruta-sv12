package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.map.OfflineInitState
import com.example.map.OfflineMapDataManager
import com.example.map.RouteProfile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfflineRoutingIntegrationTest {

    @Test
    fun initializesElSalvadorOfflineGraphAndRoutesCarAndFoot() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = OfflineMapDataManager(context)
        try {
            val state = manager.initialize { }
            assertTrue("Expected Ready state but got $state", state is OfflineInitState.Ready)

            // San Salvador -> Santa Ana (car)
            val carRoute = manager.calculateRoute(
                startLat = 13.6929,
                startLon = -89.2182,
                endLat = 13.9942,
                endLon = -89.5597,
                profile = RouteProfile.CAR
            )
            assertTrue("Car route failed: ${carRoute.exceptionOrNull()}", carRoute.isSuccess)
            val car = carRoute.getOrThrow()
            assertTrue("Expected > 40 km route", car.totalDistanceMeters > 40_000.0)
            assertTrue("Expected polyline points", car.routePoints.size > 20)
            assertTrue("Expected turn instructions", car.steps.isNotEmpty())

            // Local walking route in San Salvador (foot)
            val footRoute = manager.calculateRoute(
                startLat = 13.6929,
                startLon = -89.2182,
                endLat = 13.6995,
                endLon = -89.2105,
                profile = RouteProfile.FOOT
            )
            assertTrue("Foot route failed: ${footRoute.exceptionOrNull()}", footRoute.isSuccess)
        } finally {
            manager.close()
        }
    }
}
