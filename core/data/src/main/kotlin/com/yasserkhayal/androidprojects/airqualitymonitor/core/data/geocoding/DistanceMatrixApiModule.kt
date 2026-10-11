package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.geocoding

import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DistanceMatrixApiModule {
    @Provides
    @Singleton
    fun provideDistanceMatrixGeocodingApi(
        retrofit: Retrofit,
    ): DistanceMatrixGeocodingApi = retrofit.newBuilder().baseUrl("https://api.distancematrix.ai/")
        .build().create(DistanceMatrixGeocodingApi::class.java)

    @Provides
    @Singleton
    fun provideDistanceMatrixApiKey(): String = BuildConfig.DISTANCE_MATRIX_API_KEY
}
