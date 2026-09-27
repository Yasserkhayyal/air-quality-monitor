package com.yasserkhayal.androidprojects.airqualitymonitor.di

import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.AirQualityRepositoryImpl
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.OpenMeteoApi
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualityRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAirQualityRepository(
        implementation: AirQualityRepositoryImpl,
    ): AirQualityRepository
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            },
        )
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://air-quality-api.open-meteo.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideOpenMeteoApi(retrofit: Retrofit): OpenMeteoApi = retrofit.create(OpenMeteoApi::class.java)
}
