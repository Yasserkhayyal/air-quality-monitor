package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.geocoding

import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.LocationSearchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
abstract class LocationSearchModule {
    @Binds
    @ViewModelScoped
    abstract fun bindLocationSearchRepository(
        implementation: DistanceMatrixLocationSearchRepository,
    ): LocationSearchRepository
}
