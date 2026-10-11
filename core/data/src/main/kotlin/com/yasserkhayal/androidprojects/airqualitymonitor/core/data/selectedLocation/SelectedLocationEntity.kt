package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.selectedLocation

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location

@Entity(tableName = "selected_location")
internal data class SelectedLocationEntity(
    @PrimaryKey val id: Int = 1,
    val latitude: Double,
    val longitude: Double,
    val address: String?,
)

internal fun SelectedLocationEntity.toDomain() = Location(
    latitude = latitude,
    longitude = longitude,
    address = address,
)

internal fun Location.toSelectedLocationEntity() = SelectedLocationEntity(
    latitude = latitude,
    longitude = longitude,
    address = address,
)
