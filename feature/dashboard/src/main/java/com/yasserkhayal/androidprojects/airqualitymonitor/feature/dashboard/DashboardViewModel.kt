package com.yasserkhayal.androidprojects.airqualitymonitor.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualityRepository
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualitySnapshot
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataError
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataFreshness
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.SelectedLocationRepository
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
    val dataError: DataError? = null,
    val refreshError: DataError? = null,
    val locationPersistenceFailed: Boolean = false,
    val locationRestoreFailed: Boolean = false,
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
    data class OnPermissionResult(val granted: Boolean, val requestId: Long) : DashboardIntent
    data class OnLocationResult(val location: Location?) : DashboardIntent
    data class OnCurrentLocationResult(val location: Location?, val requestId: Long) : DashboardIntent

    data object Retry : DashboardIntent
    data object UseCurrentLocation : DashboardIntent
}

sealed interface DashboardEffect {
    data class RequestLocationPermission(val requestId: Long) : DashboardEffect
    data class RequestCurrentLocation(val requestId: Long) : DashboardEffect
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: AirQualityRepository,
    private val selectedLocationRepository: SelectedLocationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<DashboardEffect>(Channel.BUFFERED)
    val effects: Flow<DashboardEffect> = _effects.receiveAsFlow()
    internal var lastLocation: Location? = null
    private var hasStarted = false
    private var nextLocationRequestId = 0L
    private var activeLocationRequestId: Long? = null

    fun onIntent(intent: DashboardIntent) {
        when (intent) {
            DashboardIntent.Started -> {
                if (!hasStarted) {
                    hasStarted = true
                    restoreSelectedLocation()
                }
            }

            DashboardIntent.Retry -> when {
                lastLocation != null && _uiState.value.locationPersistenceFailed ->
                    persistCurrentLocation(requireNotNull(lastLocation))

                lastLocation != null -> loadAirQuality(requireNotNull(lastLocation))

                _uiState.value.error == DashboardError.DataUnavailable -> restoreSelectedLocation()

                else -> requestPermission()
            }

            DashboardIntent.UseCurrentLocation -> requestPermission()

            is DashboardIntent.OnPermissionResult -> {
                if (activeLocationRequestId == intent.requestId) {
                    if (intent.granted) {
                        emitEffect(DashboardEffect.RequestCurrentLocation(intent.requestId))
                    } else {
                        activeLocationRequestId = null
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                snapshot = null,
                                error = DashboardError.PermissionDenied,
                            )
                        }
                    }
                }
            }

            is DashboardIntent.OnLocationResult -> {
                activeLocationRequestId = null
                hasStarted = true
                lastLocation = intent.location
                _uiState.update { it.copy(locationPersistenceFailed = false) }
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

            is DashboardIntent.OnCurrentLocationResult -> {
                if (activeLocationRequestId == intent.requestId) {
                    activeLocationRequestId = null
                    hasStarted = true
                    intent.location?.let(::persistCurrentLocation)
                        ?: run {
                            lastLocation = null
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    snapshot = null,
                                    locationAddress = null,
                                    error = DashboardError.LocationUnavailable,
                                )
                            }
                        }
                }
            }
        }
    }

    private fun restoreSelectedLocation() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            selectedLocationRepository.getSelectedLocation()
                .onSuccess { location ->
                    if (location == null) {
                        requestPermission()
                    } else {
                        lastLocation = location
                        loadAirQuality(location)
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(locationRestoreFailed = true)
                    }
                    requestPermission()
                }
        }
    }

    private fun requestPermission() {
        val requestId = ++nextLocationRequestId
        activeLocationRequestId = requestId
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                isStale = false,
                refreshFailed = false,
            )
        }
        emitEffect(DashboardEffect.RequestLocationPermission(requestId))
    }

    private fun persistCurrentLocation(location: Location) {
        lastLocation = location
        _uiState.update {
            it.copy(
                isLoading = true,
                isStale = false,
                refreshFailed = false,
                locationPersistenceFailed = false,
                error = null,
            )
        }
        viewModelScope.launch {
            val saveResult = selectedLocationRepository.saveSelectedLocation(location)
            _uiState.update {
                it.copy(
                    locationPersistenceFailed = saveResult.isFailure,
                    locationRestoreFailed = saveResult.isFailure && it.locationRestoreFailed,
                )
            }
            loadAirQuality(location)
        }
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
                        refreshError = result.refreshError,
                        locationPersistenceFailed = _uiState.value.locationPersistenceFailed,
                    )
                }
                .onFailure { error ->
                    val dataError = error.asDataError()
                    _uiState.value = DashboardUiState(
                        isLoading = false,
                        locationPersistenceFailed = _uiState.value.locationPersistenceFailed,
                        dataError = dataError,
                        error = if (dataError is DataError.OfflineNoCache) {
                            DashboardError.OfflineNoCache
                        } else {
                            DashboardError.DataUnavailable
                        },
                    )
                }
        }
    }

    /** Preserves known repository errors and wraps unexpected failures for typed UI handling. */
    private fun Throwable.asDataError(): DataError = this as? DataError ?: DataError.GenericError(this)

    private fun emitEffect(effect: DashboardEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
