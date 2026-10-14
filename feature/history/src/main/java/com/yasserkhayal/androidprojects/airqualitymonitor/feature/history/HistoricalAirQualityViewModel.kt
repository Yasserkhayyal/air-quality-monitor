package com.yasserkhayal.androidprojects.airqualitymonitor.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualityRepository
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataError
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataFreshness
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.HistoricalAirQualityPoint
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.SelectedLocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Immutable state required to render the historical air-quality screen.
 */
data class HistoricalAirQualityUiState(
    val isLoading: Boolean = true,
    val location: Location? = null,
    val points: List<HistoricalAirQualityPoint> = emptyList(),
    val freshness: DataFreshness? = null,
    val refreshError: DataError? = null,
    val error: DataError? = null,
    val locationUnavailable: Boolean = false,
)

/**
 * User actions accepted by the historical air-quality screen.
 */
sealed interface HistoricalAirQualityIntent {
    data object Started : HistoricalAirQualityIntent
    data object Retry : HistoricalAirQualityIntent
}

/**
 * Loads saved-location history and exposes remote/cache outcomes to the UI.
 */
@HiltViewModel
class HistoricalAirQualityViewModel @Inject constructor(
    private val repository: AirQualityRepository,
    private val selectedLocationRepository: SelectedLocationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoricalAirQualityUiState())

    /** Observable history presentation state. */
    val uiState = _uiState.asStateFlow()

    /**
     * Processes an interaction with the historical screen.
     */
    fun onIntent(intent: HistoricalAirQualityIntent) {
        when (intent) {
            HistoricalAirQualityIntent.Started -> restoreLocationAndLoad()
            HistoricalAirQualityIntent.Retry -> retry()
        }
    }

    /** Retries history retrieval for the known location or restores it when unavailable. */
    private fun retry() {
        val location = _uiState.value.location
        if (location == null) {
            restoreLocationAndLoad()
        } else {
            loadHistory(location)
        }
    }

    /** Restores the persisted active location before requesting its hourly history. */
    private fun restoreLocationAndLoad() {
        _uiState.update {
            it.copy(
                isLoading = true,
                location = null,
                points = emptyList(),
                freshness = null,
                refreshError = null,
                error = null,
                locationUnavailable = false,
            )
        }
        viewModelScope.launch {
            selectedLocationRepository.getSelectedLocation()
                .onSuccess { location ->
                    if (location == null) {
                        _uiState.update {
                            it.copy(isLoading = false, locationUnavailable = true)
                        }
                    } else {
                        loadHistory(location)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.asDataError())
                    }
                }
        }
    }

    /** Loads history and translates repository results into immutable screen state. */
    private fun loadHistory(location: Location) {
        _uiState.update {
            val locationChanged = it.location != location
            it.copy(
                isLoading = true,
                location = location,
                points = if (locationChanged) emptyList() else it.points,
                freshness = if (locationChanged) null else it.freshness,
                refreshError = if (locationChanged) null else it.refreshError,
                error = null,
                locationUnavailable = false,
            )
        }
        viewModelScope.launch {
            repository.getHistoricalAirQuality(location)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            location = location,
                            points = result.points,
                            freshness = result.freshness,
                            refreshError = result.refreshError,
                            error = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, location = location, error = error.asDataError())
                    }
                }
        }
    }

    /** Preserves known domain errors and wraps unexpected failures for explicit UI handling. */
    private fun Throwable.asDataError(): DataError = this as? DataError ?: DataError.GenericError(this)
}
