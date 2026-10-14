package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality

import com.google.gson.annotations.SerializedName

/**
 * Minimal Open-Meteo response contract for hourly historical measurements.
 */
data class OpenMeteoHistoricalResponse(
    val hourly: OpenMeteoHourlyMeasurements?,
)

/**
 * Parallel hourly arrays returned by Open-Meteo.
 */
data class OpenMeteoHourlyMeasurements(
    val time: List<String>?,
    @SerializedName("european_aqi") val europeanAqi: List<Int?>?,
    @SerializedName("pm2_5") val pm2_5: List<Double?>?,
    val pm10: List<Double?>?,
    @SerializedName("carbon_monoxide") val carbonMonoxide: List<Double?>?,
    @SerializedName("nitrogen_dioxide") val nitrogenDioxide: List<Double?>?,
    val ozone: List<Double?>?,
)
