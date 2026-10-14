package com.yasserkhayal.androidprojects.airqualitymonitor.core.data

import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.OpenMeteoHistoricalResponse
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.OpenMeteoHourlyMeasurements
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.toDomainPoints
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Verifies mapping of Open-Meteo parallel hourly arrays to usable Domain points.
 */
class OpenMeteoHistoricalMapperTest {
    /** Maps available metrics and leaves omitted array values unavailable. */
    @Test
    fun mapsMetricsByTimestampAndHandlesShortArrays() {
        val response = OpenMeteoHistoricalResponse(
            hourly = OpenMeteoHourlyMeasurements(
                time = listOf("2026-10-13T22:00", "2026-10-13T23:00"),
                europeanAqi = listOf(42),
                pm2_5 = listOf(12.5, 13.0),
                pm10 = null,
                carbonMonoxide = null,
                nitrogenDioxide = listOf(18.0),
                ozone = listOf(46.0, 47.0),
            ),
        )

        val points = response.toDomainPoints()

        assertEquals(2, points.size)
        assertEquals("2026-10-13T22:00", points[0].timestamp)
        assertEquals(42, points[0].europeanAqi)
        assertEquals(12.5, points[0].pm2_5!!, 0.0)
        assertNull(points[1].europeanAqi)
        assertEquals(13.0, points[1].pm2_5!!, 0.0)
    }

    /** Keeps only the newest 24 usable observations in chronological order. */
    @Test
    fun mapsLatestTwentyFourPoints() {
        val timestamps = (0..25).map {
            "2026-10-${if (it < 24) "13" else "14"}T${(it % 24).toString().padStart(2, '0')}:00"
        }
        val response = OpenMeteoHistoricalResponse(
            hourly = OpenMeteoHourlyMeasurements(
                time = timestamps,
                europeanAqi = (0..25).toList(),
                pm2_5 = null,
                pm10 = null,
                carbonMonoxide = null,
                nitrogenDioxide = null,
                ozone = null,
            ),
        )

        val points = response.toDomainPoints()

        assertEquals(24, points.size)
        assertEquals(2, points.first().europeanAqi)
        assertEquals(25, points.last().europeanAqi)
    }

    /** Returns no usable points when response has no hourly measurements. */
    @Test
    fun returnsEmptyListForMissingHourlyResponse() {
        assertEquals(emptyList<Any>(), OpenMeteoHistoricalResponse(hourly = null).toDomainPoints())
    }
}
