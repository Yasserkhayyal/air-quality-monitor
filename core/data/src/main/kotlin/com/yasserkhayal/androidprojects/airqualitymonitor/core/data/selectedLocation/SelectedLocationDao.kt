package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.selectedLocation

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
internal interface SelectedLocationDao {
    @Query("SELECT * FROM selected_location WHERE id = 1")
    suspend fun getSelectedLocation(): SelectedLocationEntity?

    @Upsert
    suspend fun saveSelectedLocation(location: SelectedLocationEntity)
}
