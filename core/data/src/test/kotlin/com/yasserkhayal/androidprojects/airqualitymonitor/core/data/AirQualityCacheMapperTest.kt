package com.yasserkhayal.androidprojects.airqualitymonitor.core.data

import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.toCacheEntity
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.toCacheKey
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.toDomain
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualitySnapshot
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import org.junit.Assert.assertEquals
import org.junit.Test

class AirQualityCacheMapperTest {
    @Test
    fun `rounds coordinates to three decimal places for cache key`() {
        assertEquals("51.508:-0.128", Location(51.5079, -0.1276).toCacheKey())
        assertEquals("0.0:0.0", Location(-0.0001, 0.0001).toCacheKey())
    }

    @Test
    fun `maps domain snapshot to entity and back`() {
        val snapshot = AirQualitySnapshot(
            location = Location(51.5079, -0.1276, "London, UK"),
            timestamp = "2026-09-28T12:00",
            europeanAqi = 42,
            pm2_5 = 12.5,
            pm10 = null,
            carbonMonoxide = 300.0,
            nitrogenDioxide = 18.0,
            ozone = 46.0,
        )

        val entity = snapshot.toCacheEntity(retrievedAtEpochMillis = 12_345L)

        assertEquals("51.508:-0.128", entity.locationKey)
        assertEquals(12_345L, entity.retrievedAtEpochMillis)
        assertEquals(snapshot, entity.toDomain())
    }
}
