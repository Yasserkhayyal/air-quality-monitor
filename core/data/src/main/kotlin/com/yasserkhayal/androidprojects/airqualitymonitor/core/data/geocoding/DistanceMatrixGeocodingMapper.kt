package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.geocoding

import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location

internal fun DistanceMatrixGeocodingResultDto.toDomainOrNull(): Location? {
    val address = formattedAddress?.takeIf(String::isNotBlank) ?: return null
    val coordinate = geometry?.location ?: return null
    val latitude = coordinate.lat?.takeIf { it.isFinite() && it in -90.0..90.0 } ?: return null
    val longitude = coordinate.lng?.takeIf { it.isFinite() && it in -180.0..180.0 } ?: return null
    return Location(latitude = latitude, longitude = longitude, address = address)
}
