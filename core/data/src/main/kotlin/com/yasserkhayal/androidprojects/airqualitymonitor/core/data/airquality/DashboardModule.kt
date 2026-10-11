package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality

import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualityRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
abstract class DashboardModule {
    @Binds
    @ViewModelScoped
    abstract fun bindAirQualityRepository(
        implementation: AirQualityRepositoryImpl,
    ): AirQualityRepository
}
