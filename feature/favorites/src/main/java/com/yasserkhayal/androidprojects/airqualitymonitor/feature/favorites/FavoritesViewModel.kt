package com.yasserkhayal.androidprojects.airqualitymonitor.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.FavoritesRepository
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.SelectedLocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Immutable presentation state for the locally stored favorites.
 */
data class FavoritesUiState(
    val isLoading: Boolean = true,
    val favorites: List<Location> = emptyList(),
    val isSelecting: Boolean = false,
    val removingLocation: Location? = null,
    val error: FavoritesError? = null,
)

/**
 * Errors that have explicit Favorites presentation states.
 */
enum class FavoritesError {
    LoadFailed,
    RemoveFailed,
    SelectionFailed,
}

/**
 * User actions accepted by the Favorites feature.
 */
sealed interface FavoritesIntent {
    data object Retry : FavoritesIntent
    data class LocationSelected(val location: Location) : FavoritesIntent
    data class RemoveFavorite(val location: Location) : FavoritesIntent
}

/**
 * One-time navigation effects emitted by Favorites.
 */
sealed interface FavoritesEffect {
    data class OpenDashboard(val location: Location) : FavoritesEffect
}

/**
 * Coordinates favorite observation, removal, and active-location selection.
 */
@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
    private val selectedLocationRepository: SelectedLocationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<FavoritesEffect>(Channel.BUFFERED)
    val effects: Flow<FavoritesEffect> = _effects.receiveAsFlow()
    private var favoritesObservation: Job? = null

    init {
        observeFavorites()
    }

    /**
     * Processes a Favorites interaction.
     */
    fun onIntent(intent: FavoritesIntent) {
        when (intent) {
            FavoritesIntent.Retry -> retryObservation()
            is FavoritesIntent.LocationSelected -> selectLocation(intent.location)
            is FavoritesIntent.RemoveFavorite -> removeFavorite(intent.location)
        }
    }

    /** Starts collecting persisted favorites and translates observation failures to UI state. */
    private fun observeFavorites() {
        favoritesObservation = viewModelScope.launch {
            favoritesRepository.observeFavorites()
                .catch {
                    _uiState.update {
                        it.copy(isLoading = false, error = FavoritesError.LoadFailed)
                    }
                }
                .collect { favorites ->
                    _uiState.update {
                        it.copy(isLoading = false, favorites = favorites, error = null)
                    }
                }
        }
    }

    /** Restarts observation after the repository flow reports an error. */
    private fun retryObservation() {
        favoritesObservation?.cancel()
        _uiState.update { it.copy(isLoading = true, error = null) }
        observeFavorites()
    }

    /** Removes a location only while it remains present in observed favorites. */
    private fun removeFavorite(location: Location) {
        if (_uiState.value.removingLocation != null || !containsCoordinates(location)) return
        _uiState.update { it.copy(removingLocation = location, error = null) }
        viewModelScope.launch {
            favoritesRepository.removeFavorite(location)
                .onFailure {
                    _uiState.update { state -> state.copy(error = FavoritesError.RemoveFailed) }
                }
            _uiState.update { it.copy(removingLocation = null) }
        }
    }

    /** Persists selected favorite as active before requesting app navigation. */
    private fun selectLocation(location: Location) {
        if (_uiState.value.isSelecting || !containsCoordinates(location)) return
        _uiState.update { it.copy(isSelecting = true, error = null) }
        viewModelScope.launch {
            selectedLocationRepository.saveSelectedLocation(location)
                .onSuccess {
                    _effects.send(FavoritesEffect.OpenDashboard(location))
                }
                .onFailure {
                    _uiState.update { state ->
                        state.copy(isSelecting = false, error = FavoritesError.SelectionFailed)
                    }
                }
            if (_uiState.value.error != FavoritesError.SelectionFailed) {
                _uiState.update { it.copy(isSelecting = false) }
            }
        }
    }

    /** Checks membership by the coordinate-based favorite identity. */
    private fun containsCoordinates(location: Location): Boolean = _uiState.value.favorites.any {
        it.latitude == location.latitude && it.longitude == location.longitude
    }
}
