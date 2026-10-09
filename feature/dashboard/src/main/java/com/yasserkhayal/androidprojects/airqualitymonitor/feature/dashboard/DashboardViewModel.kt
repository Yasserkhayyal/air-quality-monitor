package com.yasserkhayal.androidprojects.airqualitymonitor.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualityRepository
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualitySnapshot
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataError
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataFreshness
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = false,
    val snapshot: AirQualitySnapshot? = null,
    val locationAddress: String? = null,
    val isStale: Boolean = false,
    val refreshFailed: Boolean = false,
    val error: DashboardError? = null,
)

enum class DashboardError {
    PermissionDenied,
    LocationUnavailable,
    DataUnavailable,
    OfflineNoCache,
}

sealed interface DashboardIntent {
    data object Started : DashboardIntent
    data class OnPermissionResult(val granted: Boolean) : DashboardIntent
    data class OnLocationResult(val location: Location?) : DashboardIntent

    data object Retry : DashboardIntent
}

sealed interface DashboardEffect {
    data object RequestLocationPermission : DashboardEffect
    data object RequestCurrentLocation : DashboardEffect
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: AirQualityRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<DashboardEffect>(Channel.BUFFERED)
    val effects: Flow<DashboardEffect> = _effects.receiveAsFlow()
    internal var lastLocation: Location? = null

    fun onIntent(intent: DashboardIntent) {
        when (intent) {
            DashboardIntent.Started -> requestPermission()

            DashboardIntent.Retry -> lastLocation?.let(::loadAirQuality) ?: requestPermission()

            is DashboardIntent.OnPermissionResult -> {
                if (intent.granted) {
                    emitEffect(DashboardEffect.RequestCurrentLocation)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            snapshot = null,
                            error = DashboardError.PermissionDenied,
                        )
                    }
                }
            }

            is DashboardIntent.OnLocationResult -> {
                lastLocation = intent.location
                intent.location?.let(::loadAirQuality)
                    ?: _uiState.update {
                        it.copy(
                            isLoading = false,
                            snapshot = null,
                            locationAddress = lastLocation?.address,
                            error = DashboardError.LocationUnavailable,
                        )
                    }
            }
        }
    }

    private fun requestPermission() {
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                isStale = false,
                refreshFailed = false,
            )
        }
        emitEffect(DashboardEffect.RequestLocationPermission)
    }

    private fun loadAirQuality(location: Location) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.getCurrentAirQuality(location)
                .onSuccess { result ->
                    _uiState.value = DashboardUiState(
                        snapshot = result.snapshot,
                        locationAddress = location.address ?: result.snapshot.location.address,
                        isStale = result.freshness == DataFreshness.Stale,
                        refreshFailed = result.refreshError != null,
                    )
                }
                .onFailure { error ->
                    _uiState.value = DashboardUiState(
                        isLoading = false,
                        error = if (error is DataError.OfflineNoCache || error is DataError.Local) {
                            DashboardError.OfflineNoCache
                        } else {
                            DashboardError.DataUnavailable
                        },
                    )
                }
        }
    }

    private fun emitEffect(effect: DashboardEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
