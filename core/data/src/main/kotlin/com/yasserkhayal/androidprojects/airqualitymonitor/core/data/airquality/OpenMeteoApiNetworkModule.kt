package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OpenMeteoApiNetworkModule {
    @Provides
    @Singleton
    fun provideOpenMeteoApi(retrofit: Retrofit): OpenMeteoApi = retrofit.newBuilder().baseUrl("https://air-quality-api.open-meteo.com/").build()
        .create(OpenMeteoApi::class.java)
}
