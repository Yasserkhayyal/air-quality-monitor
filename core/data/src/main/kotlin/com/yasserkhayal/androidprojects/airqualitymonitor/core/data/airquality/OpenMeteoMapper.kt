package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality

import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualitySnapshot
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location

internal fun OpenMeteoResponse.toDomain(location: Location): AirQualitySnapshot = AirQualitySnapshot(
    location = location,
    timestamp = current.time.replace('T', ' '),
    europeanAqi = current.europeanAqi,
    pm2_5 = current.pm2_5,
    pm10 = current.pm10,
    carbonMonoxide = current.carbonMonoxide,
    nitrogenDioxide = current.nitrogenDioxide,
    ozone = current.ozone,
)
