package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/**
 * Reads and replaces cached hourly measurements by location and time window.
 */
@Dao
internal abstract class HistoricalAirQualityDao {
    /**
     * Loads the newest cached hourly observations for one location in time order.
     */
    @Query(
        "SELECT * FROM historical_air_quality WHERE locationKey = :locationKey " +
            "ORDER BY timestamp DESC LIMIT 24",
    )
    abstract suspend fun getLatest(locationKey: String): List<HistoricalAirQualityCacheEntity>

    /**
     * Deletes cached observations for one location inside the fetched time window.
     */
    @Query(
        "DELETE FROM historical_air_quality WHERE locationKey = :locationKey " +
            "AND timestamp >= :startTimestamp AND timestamp <= :endTimestamp",
    )
    protected abstract suspend fun deleteWindow(
        locationKey: String,
        startTimestamp: String,
        endTimestamp: String,
    )

    /**
     * Inserts all retrieved hourly observations, replacing matching location/time keys.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAll(points: List<HistoricalAirQualityCacheEntity>)

    /**
     * Atomically replaces a location's cached rows within the retrieved time window.
     */
    @Transaction
    open suspend fun replaceWindow(
        locationKey: String,
        startTimestamp: String,
        endTimestamp: String,
        points: List<HistoricalAirQualityCacheEntity>,
    ) {
        require(points.isNotEmpty()) { "Historical cache replacement requires usable points" }
        deleteWindow(locationKey, startTimestamp, endTimestamp)
        insertAll(points)
    }
}
