package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.geocoding

import com.google.gson.JsonParseException
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataError
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.LocationSearchRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

private const val MAX_LOCATION_SEARCH_RESULTS = 5
private const val STATUS_OK = "OK"
private const val STATUS_ZERO_RESULTS = "ZERO_RESULTS"
private const val STATUS_REQUEST_DENIED = "REQUEST_DENIED"
private const val STATUS_OVER_QUERY_LIMIT = "OVER_QUERY_LIMIT"
private const val STATUS_UNKNOWN_ERROR = "UNKNOWN_ERROR"

class DistanceMatrixLocationSearchRepository @Inject internal constructor(
    private val api: DistanceMatrixGeocodingApi,
    private val apiKey: String,
) : LocationSearchRepository {
    override suspend fun search(query: String): Result<List<Location>> {
        if (apiKey.isBlank()) {
            return Result.failure(
                DataError.Configuration(IllegalStateException("DistanceMatrix API key is not configured")),
            )
        }
        return try {
            val response = api.search(query = query, apiKey = apiKey)
            when (response.status) {
                STATUS_OK -> {
                    val results = response.results
                        ?: return Result.failure(
                            DataError.UnexpectedResponse(
                                IllegalStateException("Geocoding response omitted results"),
                            ),
                        )
                    val locations = results.mapNotNull(DistanceMatrixGeocodingResultDto::toDomainOrNull)
                        .take(MAX_LOCATION_SEARCH_RESULTS)
                    if (results.isNotEmpty() && locations.isEmpty()) {
                        Result.failure(
                            DataError.UnexpectedResponse(
                                IllegalStateException("Geocoding response contained no valid locations"),
                            ),
                        )
                    } else {
                        Result.success(locations)
                    }
                }

                STATUS_ZERO_RESULTS -> Result.success(emptyList())

                STATUS_REQUEST_DENIED -> Result.failure(
                    DataError.Configuration(
                        IllegalStateException(
                            response.errorMessage ?: "Geocoding request failed: ${response.status}",
                        ),
                    ),
                )

                STATUS_OVER_QUERY_LIMIT -> Result.failure(
                    DataError.RateLimited(
                        IllegalStateException(
                            response.errorMessage ?: "Geocoding request rate limit exceeded",
                        ),
                    ),
                )

                STATUS_UNKNOWN_ERROR -> Result.failure(
                    DataError.ServiceUnavailable(
                        IllegalStateException(
                            response.errorMessage ?: "Geocoding service is temporarily unavailable",
                        ),
                    ),
                )

                null -> Result.failure(
                    DataError.UnexpectedResponse(
                        IllegalStateException("Geocoding response omitted status"),
                    ),
                )

                else -> Result.failure(
                    DataError.GenericError(
                        IllegalStateException(
                            response.errorMessage ?: "Geocoding request failed: ${response.status}",
                        ),
                    ),
                )
            }
        } catch (error: HttpException) {
            Result.failure(DataError.Server(error.code(), error))
        } catch (error: IOException) {
            Result.failure(DataError.Network(error))
        } catch (error: JsonParseException) {
            Result.failure(DataError.UnexpectedResponse(error))
        }
    }
}
