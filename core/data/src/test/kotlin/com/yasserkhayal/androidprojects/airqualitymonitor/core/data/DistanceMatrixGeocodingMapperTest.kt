package com.yasserkhayal.androidprojects.airqualitymonitor.core.data

import com.google.gson.Gson
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.geocoding.DistanceMatrixCoordinateDto
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.geocoding.DistanceMatrixGeocodingResponseDto
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.geocoding.DistanceMatrixGeocodingResultDto
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.geocoding.DistanceMatrixGeometryDto
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.geocoding.toDomainOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DistanceMatrixGeocodingMapperTest {
    @Test
    fun `maps formatted address and coordinates`() {
        val location = result().toDomainOrNull()

        assertEquals(30.0444, location?.latitude ?: -1.0, 0.00001)
        assertEquals(31.2357, location?.longitude ?: -1.0, 0.00001)
        assertEquals("Cairo, Egypt", location?.address)
    }

    @Test
    fun `rejects coordinates outside valid ranges`() {
        val invalid = result(latitude = 91.0)

        assertNull(invalid.toDomainOrNull())
    }

    @Test
    fun `decodes both documented result array names`() {
        val gson = Gson()
        val resultPayload = """{"status":"OK","result":[{"formatted_address":"Cairo","geometry":{"location":{"lat":30.0,"lng":31.0}}}]}"""
        val resultsPayload = """{"status":"OK","results":[{"formatted_address":"Cairo","geometry":{"location":{"lat":30.0,"lng":31.0}}}]}"""

        assertEquals(
            "Cairo",
            gson.fromJson(resultPayload, DistanceMatrixGeocodingResponseDto::class.java)
                .results?.single()?.formattedAddress,
        )
        assertEquals(
            "Cairo",
            gson.fromJson(resultsPayload, DistanceMatrixGeocodingResponseDto::class.java)
                .results?.single()?.formattedAddress,
        )
    }

    private fun result(
        latitude: Double = 30.0444,
        longitude: Double = 31.2357,
    ) = DistanceMatrixGeocodingResultDto(
        formattedAddress = "Cairo, Egypt",
        geometry = DistanceMatrixGeometryDto(
            location = DistanceMatrixCoordinateDto(lat = latitude, lng = longitude),
        ),
    )
}
