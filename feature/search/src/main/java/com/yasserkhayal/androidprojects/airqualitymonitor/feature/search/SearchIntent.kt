package com.yasserkhayal.androidprojects.airqualitymonitor.feature.search

import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location

internal sealed interface SearchIntent {
    data class QueryChanged(val query: String) : SearchIntent
    data object Retry : SearchIntent
    data class LocationSelected(val location: Location) : SearchIntent
}

internal sealed interface SearchEffect {
    data class OpenDashboard(val location: Location) : SearchEffect
}
