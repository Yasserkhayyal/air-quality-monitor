package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.geocoding

import com.google.gson.annotations.SerializedName

internal data class DistanceMatrixGeocodingResponseDto(
    val status: String?,
    @SerializedName(value = "result", alternate = ["results"])
    val results: List<DistanceMatrixGeocodingResultDto>?,
    @SerializedName("error_message")
    val errorMessage: String?,
)

internal data class DistanceMatrixGeocodingResultDto(
    @SerializedName("formatted_address")
    val formattedAddress: String?,
    val geometry: DistanceMatrixGeometryDto?,
)

internal data class DistanceMatrixGeometryDto(
    val location: DistanceMatrixCoordinateDto?,
)

internal data class DistanceMatrixCoordinateDto(
    val lat: Double?,
    val lng: Double?,
)
