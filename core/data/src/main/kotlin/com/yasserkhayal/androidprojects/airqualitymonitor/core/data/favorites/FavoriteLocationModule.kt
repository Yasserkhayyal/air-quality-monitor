package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.favorites

import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.FavoritesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

/** Binds favorite persistence within each ViewModel dependency graph. */
@Module
@InstallIn(ViewModelComponent::class)
abstract class FavoriteLocationModule {
    /**
     * Binds the Room-backed favorite repository to its domain contract.
     */
    @Binds
    @ViewModelScoped
    abstract fun bindFavoritesRepository(
        implementation: FavoriteLocationRepositoryImpl,
    ): FavoritesRepository
}
