package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality

import android.database.sqlite.SQLiteException
import android.util.Log
import com.google.gson.JsonParseException
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualityRepository
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualityResult
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataError
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataFreshness
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataOrigin
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

private const val TAG = "AirQualityRepositoryImpl"

class AirQualityRepositoryImpl @Inject internal constructor(
    private val api: OpenMeteoApi,
    private val dao: AirQualityDao,
) : AirQualityRepository {
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

    private suspend fun fetchRemote(location: Location) = try {
        Result.success(
            api.getCurrentAirQuality(location.latitude, location.longitude).toDomain(location),
        )
    } catch (error: CancellationException) {
        throw error
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
