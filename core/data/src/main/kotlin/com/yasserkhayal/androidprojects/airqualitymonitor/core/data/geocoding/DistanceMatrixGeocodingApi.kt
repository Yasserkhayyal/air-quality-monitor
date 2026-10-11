package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.geocoding

import retrofit2.http.GET
import retrofit2.http.Query

internal interface DistanceMatrixGeocodingApi {
    @GET("maps/api/geocode/json")
    suspend fun search(
        @Query("address") query: String,
        @Query("key") apiKey: String,
    ): DistanceMatrixGeocodingResponseDto
}
