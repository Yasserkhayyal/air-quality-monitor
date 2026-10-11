package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality

import com.google.gson.annotations.SerializedName

data class OpenMeteoResponse(
    val current: OpenMeteoCurrent,
)

data class OpenMeteoCurrent(
    val time: String,
    @SerializedName("european_aqi") val europeanAqi: Int?,
    @SerializedName("pm2_5") val pm2_5: Double?,
    val pm10: Double?,
    @SerializedName("carbon_monoxide") val carbonMonoxide: Double?,
    @SerializedName("nitrogen_dioxide") val nitrogenDioxide: Double?,
    val ozone: Double?,
)
