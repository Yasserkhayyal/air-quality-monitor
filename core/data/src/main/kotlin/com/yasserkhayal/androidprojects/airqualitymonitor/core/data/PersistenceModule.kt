package com.yasserkhayal.androidprojects.airqualitymonitor.core.data

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
    ).build()

    @Provides
    fun provideAirQualityDao(database: AirQualityDatabase): AirQualityDao = database.airQualityDao()
}
