package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.selectedLocation

import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.SelectedLocationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
abstract class SelectedLocationModule {
    @Binds
    @ViewModelScoped
    abstract fun bindSelectedLocationRepository(
        implementation: SelectedLocationRepositoryImpl,
    ): SelectedLocationRepository
}
