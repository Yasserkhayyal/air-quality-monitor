package com.yasserkhayal.androidprojects.airqualitymonitor.core.data

import com.google.gson.JsonParseException
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.geocoding.DistanceMatrixCoordinateDto
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.geocoding.DistanceMatrixGeocodingApi
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.geocoding.DistanceMatrixGeocodingResponseDto
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.geocoding.DistanceMatrixGeocodingResultDto
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.geocoding.DistanceMatrixGeometryDto
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.geocoding.DistanceMatrixLocationSearchRepository
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class DistanceMatrixLocationSearchRepositoryTest {
    private val api = mockk<DistanceMatrixGeocodingApi>()
    private val repository = DistanceMatrixLocationSearchRepository(api, TEST_API_KEY)

    @Test
    fun `returns valid available matches capped at five`() = runTest {
        coEvery { api.search("Cairo", TEST_API_KEY) } returns response(
            results = (0..6).map { index ->
                DistanceMatrixGeocodingResultDto(
                    formattedAddress = "Cairo district $index, Egypt",
                    geometry = DistanceMatrixGeometryDto(
                        DistanceMatrixCoordinateDto(30.0 + index, 31.0),
                    ),
                )
            },
        )

        val result = repository.search("Cairo")

        assertEquals(5, result.getOrThrow().size)
        assertEquals("Cairo district 0, Egypt", result.getOrThrow().first().address)
        coVerify(exactly = 1) { api.search("Cairo", TEST_API_KEY) }
    }

    @Test
    fun `returns empty list when provider reports zero results`() = runTest {
        coEvery { api.search(any(), TEST_API_KEY) } returns response(
            status = "ZERO_RESULTS",
            results = null,
        )

        assertTrue(repository.search("Unknown").getOrThrow().isEmpty())
    }

    @Test
    fun `maps invalid locations to domain error`() = runTest {
        coEvery { api.search("Cairo", TEST_API_KEY) } returns response(
            results = listOf(
                DistanceMatrixGeocodingResultDto(
                    formattedAddress = "Cairo, Egypt",
                    geometry = DistanceMatrixGeometryDto(
                        DistanceMatrixCoordinateDto(30.1, 181.0),
                    ),
                ),
            ),
        )

        assertTrue(repository.search("Cairo").exceptionOrNull() is DataError.UnexpectedResponse)
    }

    /** Provider access rejection is surfaced as a configuration issue, not a connectivity error. */
    @Test
    fun `maps denied provider request to configuration error`() = runTest {
        coEvery { api.search(any(), TEST_API_KEY) } returns response(
            status = "REQUEST_DENIED",
            results = null,
            errorMessage = "Request denied",
        )

        assertTrue(repository.search("Cairo").exceptionOrNull() is DataError.Configuration)
    }

    /** Provider rate limiting receives a distinct outcome for a useful retry message. */
    @Test
    fun `maps provider rate limit to rate limited error`() = runTest {
        coEvery { api.search(any(), TEST_API_KEY) } returns response(
            status = "OVER_QUERY_LIMIT",
            results = null,
        )

        assertTrue(repository.search("Cairo").exceptionOrNull() is DataError.RateLimited)
    }

    /** The provider's temporary-failure status is not reported as an empty result. */
    @Test
    fun `maps provider temporary failure to service unavailable error`() = runTest {
        coEvery { api.search(any(), TEST_API_KEY) } returns response(
            status = "UNKNOWN_ERROR",
            results = null,
        )

        assertTrue(repository.search("Cairo").exceptionOrNull() is DataError.ServiceUnavailable)
    }

    /** A missing provider status indicates an invalid response, not zero matches. */
    @Test
    fun `maps missing provider status to unexpected response error`() = runTest {
        coEvery { api.search(any(), TEST_API_KEY) } returns response(
            status = null,
            results = null,
        )

        assertTrue(repository.search("Cairo").exceptionOrNull() is DataError.UnexpectedResponse)
    }

    /** Unknown provider statuses remain explicit failures instead of appearing as empty results. */
    @Test
    fun `maps unknown provider status to generic error`() = runTest {
        coEvery { api.search(any(), TEST_API_KEY) } returns response(
            status = "UNKNOWN_STATUS",
            results = null,
        )

        assertTrue(repository.search("Cairo").exceptionOrNull() is DataError.GenericError)
    }

    /** An OK response without its required results field is malformed. */
    @Test
    fun `maps omitted results in successful provider response to unexpected response error`() = runTest {
        coEvery { api.search(any(), TEST_API_KEY) } returns response(
            status = "OK",
            results = null,
        )

        assertTrue(repository.search("Cairo").exceptionOrNull() is DataError.UnexpectedResponse)
    }

    @Test
    fun `maps network failures to domain error`() = runTest {
        coEvery { api.search(any(), TEST_API_KEY) } throws IOException("offline")

        assertTrue(repository.search("Cairo").exceptionOrNull() is DataError.Network)
    }

    @Test
    fun `maps malformed provider response to domain error`() = runTest {
        coEvery { api.search(any(), TEST_API_KEY) } throws JsonParseException("malformed response")

        assertTrue(repository.search("Cairo").exceptionOrNull() is DataError.UnexpectedResponse)
    }

    @Test
    fun `fails when api key is missing without making request`() = runTest {
        val repository = DistanceMatrixLocationSearchRepository(api, "")

        assertTrue(repository.search("Cairo").exceptionOrNull() is DataError.Configuration)
        coVerify(exactly = 0) { api.search(any(), any()) }
    }

    private fun response(
        status: String? = "OK",
        results: List<DistanceMatrixGeocodingResultDto>? = emptyList(),
        errorMessage: String? = null,
    ) = DistanceMatrixGeocodingResponseDto(
        status = status,
        results = results,
        errorMessage = errorMessage,
    )

    private companion object {
        const val TEST_API_KEY = "test-key"
    }
}
