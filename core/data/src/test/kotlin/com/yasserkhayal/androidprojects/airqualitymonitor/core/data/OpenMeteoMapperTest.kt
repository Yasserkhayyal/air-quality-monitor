package com.yasserkhayal.androidprojects.airqualitymonitor.core.data

import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import org.junit.Assert.assertEquals
import org.junit.Test

class OpenMeteoMapperTest {
    @Test
    fun `maps response to domain snapshot`() {
        val response = OpenMeteoResponse(
            current = OpenMeteoCurrent(
                time = "2026-09-25T16:00",
                europeanAqi = 42,
                pm2_5 = 12.5,
                pm10 = 20.0,
                carbonMonoxide = 300.0,
                nitrogenDioxide = 18.0,
                ozone = 46.0,
            ),
        )

        val location = Location(51.5, -0.1)

        val snapshot = response.toDomain(location)

        assertEquals(location, snapshot.location)
        assertEquals("2026-09-25 16:00", snapshot.timestamp)
        assertEquals(42, snapshot.europeanAqi)
        assertEquals(12.5, snapshot.pm2_5 ?: 0.0, 0.0)
        assertEquals(20.0, snapshot.pm10 ?: 0.0, 0.0)
        assertEquals(300.0, snapshot.carbonMonoxide ?: 0.0, 0.0)
        assertEquals(18.0, snapshot.nitrogenDioxide ?: 0.0, 0.0)
        assertEquals(46.0, snapshot.ozone ?: 0.0, 0.0)
    }
}
