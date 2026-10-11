package com.yasserkhayal.androidprojects.airqualitymonitor.feature.search

import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataError
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location

internal data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val isSelecting: Boolean = false,
    val results: List<Location> = emptyList(),
    val hasSearched: Boolean = false,
    val error: SearchError? = null,
) {
    val canSearch: Boolean
        get() = query.trim().length >= MIN_SEARCH_QUERY_LENGTH

    val isEmpty: Boolean
        get() = hasSearched && results.isEmpty() && error == null && !isSearching
}

internal sealed class SearchError {
    data class SearchFailed(val dataError: DataError) : SearchError()
    data class SelectionFailed(val dataError: DataError) : SearchError()
}

internal const val MIN_SEARCH_QUERY_LENGTH = 2
