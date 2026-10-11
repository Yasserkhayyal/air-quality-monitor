package com.yasserkhayal.androidprojects.airqualitymonitor.core.domain

interface SelectedLocationRepository {
    suspend fun getSelectedLocation(): Result<Location?>

    suspend fun saveSelectedLocation(location: Location): Result<Unit>
}
