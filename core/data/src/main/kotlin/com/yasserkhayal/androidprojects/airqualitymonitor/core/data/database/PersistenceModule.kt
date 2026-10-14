package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.database

import android.content.Context
import androidx.room.Room
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.AirQualityDao
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.HistoricalAirQualityDao
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.favorites.FavoriteLocationDao
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.selectedLocation.SelectedLocationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides the application's Room database and DAOs. */
@Module
@InstallIn(SingletonComponent::class)
internal object PersistenceModule {
    @Provides
    @Singleton
    fun provideAirQualityDatabase(
        @ApplicationContext context: Context,
    ): AirQualityDatabase = Room.databaseBuilder(
        context,
        AirQualityDatabase::class.java,
        "air_quality.db",
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()

    @Provides
    fun provideAirQualityDao(database: AirQualityDatabase): AirQualityDao = database.airQualityDao()

    @Provides
    fun provideSelectedLocationDao(database: AirQualityDatabase): SelectedLocationDao = database.selectedLocationDao()

    /** Provides the database DAO for observing and mutating favorite locations. */
    @Provides
    fun provideFavoriteLocationDao(database: AirQualityDatabase): FavoriteLocationDao = database.favoriteLocationDao()

    /** Provides the DAO for cached hourly air-quality measurements. */
    @Provides
    fun provideHistoricalAirQualityDao(database: AirQualityDatabase): HistoricalAirQualityDao = database.historicalAirQualityDao()
}
