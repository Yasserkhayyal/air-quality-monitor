package com.yasserkhayal.androidprojects.airqualitymonitor.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualityRepository
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualitySnapshot
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
    val error: DashboardError? = null,
)

enum class DashboardError {
    PermissionDenied,
    LocationUnavailable,
    DataUnavailable,
}

sealed interface DashboardIntent {
    data object Started : DashboardIntent
    data class PermissionResult(val granted: Boolean) : DashboardIntent
    data class LocationResult(val location: Location?) : DashboardIntent

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

    fun onIntent(intent: DashboardIntent) {
        when (intent) {
            DashboardIntent.Started,
            DashboardIntent.Retry,
            -> requestPermission()

            is DashboardIntent.PermissionResult -> {
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

            is DashboardIntent.LocationResult -> {
                intent.location?.let(::loadAirQuality)
                    ?: _uiState.update {
                        it.copy(
                            isLoading = false,
                            snapshot = null,
                            locationAddress = intent.location?.address,
                            error = DashboardError.LocationUnavailable,
                        )
                    }
            }
        }
    }

    private fun requestPermission() {
        _uiState.update { it.copy(isLoading = true, snapshot = null, error = null) }
        emitEffect(DashboardEffect.RequestLocationPermission)
    }

    private fun loadAirQuality(location: Location) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.getCurrentAirQuality(location)
                .onSuccess { snapshot ->
                    _uiState.value = DashboardUiState(snapshot = snapshot, locationAddress = location.address, isLoading = false)
                }
                .onFailure {
                    _uiState.value = DashboardUiState(
                        isLoading = false,
                        snapshot = null,
                        error = DashboardError.DataUnavailable,
                    )
                }
        }
    }

    private fun emitEffect(effect: DashboardEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
