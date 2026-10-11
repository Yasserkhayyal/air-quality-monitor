package com.yasserkhayal.androidprojects.airqualitymonitor.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataError
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.LocationSearchRepository
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.SelectedLocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

internal const val SEARCH_DEBOUNCE_MILLIS = 600L

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
internal class SearchViewModel @Inject constructor(
    private val locationSearchRepository: LocationSearchRepository,
    private val selectedLocationRepository: SelectedLocationRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<SearchEffect>(Channel.BUFFERED)
    val effects: Flow<SearchEffect> = _effects.receiveAsFlow()

    private val searchRequests = MutableStateFlow(SearchRequest(query = "", generation = 0, immediate = false))

    init {
        searchRequests
            .onEach { request ->
                _uiState.update {
                    it.copy(
                        query = request.query,
                        isSearching = request.query.trim().length >= MIN_SEARCH_QUERY_LENGTH,
                        isSelecting = false,
                        results = emptyList(),
                        hasSearched = false,
                        error = null,
                    )
                }
            }
            .flatMapLatest { request ->
                val query = request.query.trim()
                if (query.length < MIN_SEARCH_QUERY_LENGTH) {
                    flowOf(SearchOutcome.NoSearch)
                } else {
                    flow<SearchOutcome> {
                        if (!request.immediate) delay(SEARCH_DEBOUNCE_MILLIS.milliseconds)
                        emit(SearchOutcome.Completed(locationSearchRepository.search(query)))
                    }
                }
            }
            .onEach(::applySearchOutcome)
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.QueryChanged -> {
                if (intent.query != searchRequests.value.query) {
                    searchRequests.update {
                        SearchRequest(
                            query = intent.query,
                            generation = it.generation + 1,
                            immediate = false,
                        )
                    }
                }
            }

            SearchIntent.Retry -> {
                val query = _uiState.value.query
                if (query.trim().length >= MIN_SEARCH_QUERY_LENGTH) {
                    searchRequests.update {
                        SearchRequest(
                            query = query,
                            generation = it.generation + 1,
                            immediate = true,
                        )
                    }
                }
            }

            is SearchIntent.LocationSelected -> selectLocation(intent.location)
        }
    }

    private fun applySearchOutcome(outcome: SearchOutcome) {
        when (outcome) {
            SearchOutcome.NoSearch -> _uiState.update {
                it.copy(isSearching = false, results = emptyList(), hasSearched = false, error = null)
            }

            is SearchOutcome.Completed -> _uiState.update { uiState ->
                outcome.result.fold(
                    onSuccess = { results ->
                        uiState.copy(
                            isSearching = false,
                            results = results,
                            hasSearched = true,
                            error = null,
                        )
                    },
                    onFailure = { error ->
                        uiState.copy(
                            isSearching = false,
                            results = emptyList(),
                            hasSearched = true,
                            error = SearchError.SearchFailed(error.asDataError()),
                        )
                    },
                )
            }
        }
    }

    private fun selectLocation(location: Location) {
        if (_uiState.value.isSelecting || location !in _uiState.value.results) return
        _uiState.update { it.copy(isSelecting = true, error = null) }
        viewModelScope.launch {
            val saveResult = selectedLocationRepository.saveSelectedLocation(location)
            saveResult.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSelecting = false, error = null) }
                    _effects.send(SearchEffect.OpenDashboard(location))
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            isSelecting = false,
                            error = SearchError.SelectionFailed(error.asDataError()),
                        )
                    }
                },
            )
        }
    }

    /** Wraps unexpected repository exceptions while preserving known data-error categories. */
    private fun Throwable.asDataError(): DataError = this as? DataError ?: DataError.GenericError(this)

    private data class SearchRequest(
        val query: String,
        val generation: Long,
        val immediate: Boolean,
    )

    private sealed interface SearchOutcome {
        data object NoSearch : SearchOutcome
        data class Completed(val result: Result<List<Location>>) : SearchOutcome
    }
}
