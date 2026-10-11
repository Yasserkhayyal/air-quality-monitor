package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {
    @GET("v1/air-quality")
    suspend fun getCurrentAirQuality(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String =
            "european_aqi,pm2_5,pm10,carbon_monoxide,nitrogen_dioxide,ozone",
        @Query("timezone") timezone: String = "auto",
    ): OpenMeteoResponse
}
