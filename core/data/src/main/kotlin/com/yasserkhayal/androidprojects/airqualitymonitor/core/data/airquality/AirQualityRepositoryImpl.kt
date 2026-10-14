package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality

import android.database.sqlite.SQLiteException
import android.util.Log
import com.google.gson.JsonParseException
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualityRepository
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualityResult
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataError
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataFreshness
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataOrigin
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.HistoricalAirQualityResult
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

private const val TAG = "AirQualityRepositoryImpl"

/**
 * Coordinates Open-Meteo current and historical data with Room-backed caching.
 */
class AirQualityRepositoryImpl @Inject internal constructor(
    private val api: OpenMeteoApi,
    private val dao: AirQualityDao,
    private val historicalDao: HistoricalAirQualityDao,
) : AirQualityRepository {
    /**
     * Retrieves current data and falls back to the cached measurement when remote retrieval fails.
     */
    override suspend fun getCurrentAirQuality(location: Location): Result<AirQualityResult> {
        val remoteResult = fetchRemote(location)
        val remoteSnapshot = remoteResult.getOrNull()
        if (remoteSnapshot != null) {
            val retrievedAt = System.currentTimeMillis()
            try {
                dao.upsert(remoteSnapshot.toCacheEntity(retrievedAt))
            } catch (error: SQLiteException) {
                Log.e(TAG, "Unable to persist air-quality cache", error)
            }
            return Result.success(
                AirQualityResult(
                    snapshot = remoteSnapshot,
                    freshness = DataFreshness.Fresh,
                    origin = DataOrigin.Remote,
                ),
            )
        }

        val remoteError = requireNotNull(remoteResult.exceptionOrNull())
        val cachedEntity = try {
            dao.findByLocationKey(location.toCacheKey())
        } catch (error: SQLiteException) {
            return Result.failure(DataError.Local(error))
        }

        if (cachedEntity == null) {
            return Result.failure(
                if (remoteError is DataError.Network) {
                    DataError.OfflineNoCache(remoteError)
                } else {
                    remoteError
                },
            )
        }

        return Result.success(
            AirQualityResult(
                snapshot = cachedEntity.toDomain(),
                freshness = freshnessAt(
                    retrievedAtEpochMillis = cachedEntity.retrievedAtEpochMillis,
                    nowEpochMillis = System.currentTimeMillis(),
                ),
                origin = DataOrigin.Cache,
                refreshError = remoteError as? DataError,
            ),
        )
    }

    /**
     * Retrieves hourly history and falls back to cached points on remote failure or unusable data.
     */
    override suspend fun getHistoricalAirQuality(location: Location): Result<HistoricalAirQualityResult> {
        val remoteResult = fetchHistorical(location)
        val remotePoints = remoteResult.getOrNull()?.asReversed() ?: emptyList()
        if (remotePoints.isNotEmpty()) {
            val locationKey = location.toCacheKey()
            val retrievedAt = System.currentTimeMillis()
            val cachedPoints = remotePoints.map { it.toCacheEntity(location, retrievedAt) }
            try {
                historicalDao.replaceWindow(
                    locationKey = locationKey,
                    startTimestamp = remotePoints.first().timestamp,
                    endTimestamp = remotePoints.last().timestamp,
                    points = cachedPoints,
                )
            } catch (error: SQLiteException) {
                Log.e(TAG, "Unable to persist historical air-quality cache", error)
            }
            return Result.success(
                HistoricalAirQualityResult(
                    points = remotePoints,
                    freshness = DataFreshness.Fresh,
                    origin = DataOrigin.Remote,
                ),
            )
        }

        val remoteError = remoteResult.exceptionOrNull()
            ?: DataError.UnexpectedResponse(IllegalStateException("No usable historical measurements returned"))
        val cachedPoints = try {
            historicalDao.getLatest(location.toCacheKey())
        } catch (error: SQLiteException) {
            return Result.failure(DataError.Local(error))
        }
        if (cachedPoints.isEmpty()) {
            return Result.failure(
                if (remoteError is DataError.Network) {
                    DataError.OfflineNoCache(remoteError)
                } else {
                    remoteError
                },
            )
        }

        val retrievedAt = cachedPoints.minOf(HistoricalAirQualityCacheEntity::retrievedAtEpochMillis)
        val refreshError = remoteError as? DataError
        return Result.success(
            HistoricalAirQualityResult(
                points = cachedPoints.map(HistoricalAirQualityCacheEntity::toDomain),
                freshness = if (remoteResult.isSuccess) {
                    DataFreshness.Stale
                } else {
                    historicalFreshnessAt(retrievedAt, System.currentTimeMillis())
                },
                origin = DataOrigin.Cache,
                refreshError = refreshError,
            ),
        )
    }

    /** Translates remote current-data failures into the Domain error hierarchy. */
    private suspend fun fetchRemote(location: Location) = try {
        Result.success(
            api.getCurrentAirQuality(location.latitude, location.longitude).toDomain(location),
        )
    } catch (error: HttpException) {
        Result.failure(DataError.Server(error.code(), error))
    } catch (error: IOException) {
        Result.failure(DataError.Network(error))
    } catch (error: JsonParseException) {
        Result.failure(DataError.UnexpectedResponse(error))
    } catch (error: Exception) {
        Result.failure(DataError.GenericError(error))
    }

    /** Translates remote historical-data failures into the Domain error hierarchy. */
    private suspend fun fetchHistorical(location: Location) = try {
        Result.success(
            api.getHistoricalAirQuality(location.latitude, location.longitude).toDomainPoints(),
        )
    } catch (error: HttpException) {
        Result.failure(DataError.Server(error.code(), error))
    } catch (error: IOException) {
        Result.failure(DataError.Network(error))
    } catch (error: JsonParseException) {
        Result.failure(DataError.UnexpectedResponse(error))
    } catch (error: Exception) {
        Result.failure(DataError.GenericError(error))
    }
}
