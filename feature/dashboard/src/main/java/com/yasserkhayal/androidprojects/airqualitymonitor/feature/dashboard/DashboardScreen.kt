package com.yasserkhayal.androidprojects.airqualitymonitor.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eygraber.compose.placeholder.PlaceholderDefaults
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.color
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.shimmer
import com.yasserkhayal.androidprojects.airqualitymonitor.core.designsystem.theme.AirQualityMonitorTheme
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualitySnapshot
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataError
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location

/**
 * One labeled pollutant value rendered by the shared pollutant grid.
 */
internal data class PollutantReading(
    val label: String,
    val value: Double?,
)

const val SEARCH_TOP_BAR_ICON_BUTTON = "search_top_bar_icon_button"
const val FAVORITES_TOP_BAR_ICON_BUTTON = "favorites_top_bar_icon_button"
const val ADD_FAVORITE_TOP_BAR_ICON_BUTTON = "add_favorite_top_bar_icon_button"
const val SEARCH_CHIP = "search_chip"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenSearch: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenHistory: () -> Unit,
    uiState: DashboardUiState = DashboardUiState(),
    onIntent: (DashboardIntent) -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_title)) },
                actions = {
                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier.testTag(SEARCH_TOP_BAR_ICON_BUTTON),
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = stringResource(R.string.content_description_search),
                        )
                    }
                    IconButton(
                        onClick = onOpenFavorites,
                        modifier = Modifier.testTag(FAVORITES_TOP_BAR_ICON_BUTTON),
                    ) {
                        Icon(
                            Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(R.string.content_description_favorites),
                        )
                    }
                    IconButton(
                        onClick = { onIntent(DashboardIntent.AddActiveLocationToFavorites) },
                        enabled = uiState.activeLocation != null &&
                            !uiState.isLocationFavorite &&
                            !uiState.isSavingFavorite &&
                            !uiState.favoritesUnavailable,
                        modifier = Modifier.testTag(ADD_FAVORITE_TOP_BAR_ICON_BUTTON),
                    ) {
                        if (uiState.isSavingFavorite) {
                            CircularProgressIndicator()
                        } else {
                            Icon(
                                imageVector = if (uiState.isLocationFavorite) {
                                    Icons.Default.Favorite
                                } else {
                                    Icons.Default.FavoriteBorder
                                },
                                contentDescription = stringResource(
                                    if (uiState.isLocationFavorite) {
                                        R.string.dashboard_location_is_favorite
                                    } else {
                                        R.string.dashboard_add_location_to_favorites
                                    },
                                ),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.dashboard_current_location),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    when (uiState.error) {
                        DashboardError.LocationUnavailable -> stringResource(R.string.dashboard_location_unavailable)

                        DashboardError.PermissionDenied -> stringResource(R.string.dashboard_location_permission_denied)

                        else ->
                            uiState.locationAddress
                                ?: stringResource(R.string.dashboard_location_selected)
                    },
                    modifier = Modifier.placeholder(
                        visible = uiState.isLoading,
                        color = PlaceholderDefaults.color(backgroundColor = MaterialTheme.colorScheme.background),
                        highlight = PlaceholderHighlight.shimmer(
                            highlightColor = MaterialTheme.colorScheme.background.copy(
                                alpha = 0.35f,
                            ),
                        ),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            item {
                DashboardContent(
                    uiState = uiState,
                    onOpenHistory = onOpenHistory,
                    onOpenSearch = onOpenSearch,
                    onRetry = { onIntent(DashboardIntent.Retry) },
                    onUseCurrentLocation = { onIntent(DashboardIntent.UseCurrentLocation) },
                )
            }
        }
    }
}

@Composable
fun DashboardRoute(
    initialLocation: Location?,
    onInitialLocationConsumed: () -> Unit,
    onRequestLocationPermission: ((Boolean) -> Unit) -> Unit,
    onRequestCurrentLocation: ((Location?) -> Unit) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DashboardEffect.RequestLocationPermission ->
                    onRequestLocationPermission { granted ->
                        viewModel.onIntent(
                            DashboardIntent.OnPermissionResult(granted, effect.requestId),
                        )
                    }

                is DashboardEffect.RequestCurrentLocation ->
                    onRequestCurrentLocation { location ->
                        viewModel.onIntent(
                            DashboardIntent.OnCurrentLocationResult(location, effect.requestId),
                        )
                    }
            }
        }
    }

    LaunchedEffect(initialLocation) {
        if (initialLocation == null) {
            viewModel.onIntent(DashboardIntent.Started)
        } else {
            viewModel.onIntent(DashboardIntent.OnLocationResult(initialLocation))
            onInitialLocationConsumed()
        }
    }

    DashboardScreen(
        onOpenSearch = onOpenSearch,
        onOpenFavorites = onOpenFavorites,
        onOpenHistory = onOpenHistory,
        uiState = uiState,
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onOpenHistory: () -> Unit,
    onOpenSearch: () -> Unit,
    onRetry: () -> Unit,
    onUseCurrentLocation: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (uiState.locationRestoreFailed) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Text(
                    stringResource(R.string.dashboard_location_restore_failed),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (uiState.locationPersistenceFailed) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Text(
                    stringResource(R.string.dashboard_location_not_saved),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (uiState.favoritesUnavailable) {
            Text(
                stringResource(R.string.dashboard_favorites_unavailable),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else if (uiState.favoriteOperationFailed) {
            Text(
                stringResource(R.string.dashboard_favorite_add_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (uiState.refreshFailed) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val refreshError = uiState.refreshError
                    Text(
                        if (refreshError == null) {
                            stringResource(R.string.dashboard_refresh_failed)
                        } else {
                            dashboardErrorMessage(refreshError)
                        },
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = onRetry) {
                        Text(stringResource(R.string.dashboard_retry))
                    }
                }
            }
        }

        if (uiState.error == DashboardError.OfflineNoCache) {
            val dataError = uiState.dataError
            Text(
                if (dataError == null) {
                    stringResource(R.string.dashboard_offline_no_cache)
                } else {
                    dashboardErrorMessage(dataError)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            if (uiState.error == DashboardError.DataUnavailable) {
                val dataError = uiState.dataError
                Text(
                    if (dataError == null) {
                        stringResource(R.string.dashboard_data_unavailable)
                    } else {
                        dashboardErrorMessage(dataError)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            AqiSummaryCard(
                europeanAqi = uiState.snapshot?.europeanAqi,
                category = uiState.snapshot?.category,
                timestamp = uiState.snapshot?.timestamp?.let(::formatTimestampForDisplay)
                    ?: stringResource(R.string.dashboard_timestamp_unavailable),
                isLoading = uiState.isLoading,
                isStale = uiState.isStale,
            )

            Text(
                stringResource(R.string.dashboard_pollutants),
                style = MaterialTheme.typography.titleMedium,
            )
            PollutantGrid(
                isLoading = uiState.isLoading,
                readings = listOf(
                    PollutantReading("PM2.5", uiState.snapshot?.pm2_5),
                    PollutantReading("PM10", uiState.snapshot?.pm10),
                    PollutantReading("NO2", uiState.snapshot?.nitrogenDioxide),
                    PollutantReading("O3", uiState.snapshot?.ozone),
                    PollutantReading("CO", uiState.snapshot?.carbonMonoxide),
                ),
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.dashboard_history_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.dashboard_history_preview),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onOpenHistory) {
                    Text(stringResource(R.string.dashboard_view_history))
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = if (uiState.error == null) onUseCurrentLocation else onRetry,
            ) {
                Text(
                    if (uiState.error == null) {
                        stringResource(R.string.dashboard_use_current_location)
                    } else {
                        stringResource(R.string.dashboard_retry)
                    },
                )
            }
            AssistChip(
                onClick = onOpenSearch,
                modifier = Modifier.testTag(SEARCH_CHIP),
                label = { Text(stringResource(R.string.dashboard_search)) },
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** Returns localized user-facing copy for a typed data-layer failure. */
@Composable
private fun dashboardErrorMessage(error: DataError): String = when (error) {
    is DataError.Network -> stringResource(R.string.dashboard_error_network)

    is DataError.Server -> when {
        error.code == 429 -> stringResource(R.string.dashboard_error_rate_limited)
        error.code in 500..599 -> stringResource(R.string.dashboard_error_server)
        else -> stringResource(R.string.dashboard_error_request_rejected)
    }

    is DataError.Configuration -> stringResource(R.string.dashboard_error_configuration)

    is DataError.RateLimited -> stringResource(R.string.dashboard_error_rate_limited)

    is DataError.ServiceUnavailable -> stringResource(R.string.dashboard_error_server)

    is DataError.UnexpectedResponse,
    is DataError.GenericError,
    -> stringResource(R.string.dashboard_error_unexpected)

    is DataError.OfflineNoCache -> stringResource(R.string.dashboard_offline_no_cache)

    is DataError.Local -> stringResource(R.string.dashboard_error_local)
}

/** Renders labeled pollutant measurements in a two-column layout. */
@Composable
internal fun PollutantGrid(isLoading: Boolean, readings: List<PollutantReading>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        readings.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { reading ->
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                reading.label,
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Text(
                                reading.value?.toString()
                                    ?: stringResource(R.string.dashboard_measurement_not_available),
                                modifier = Modifier.placeholder(
                                    visible = isLoading,
                                    color = PlaceholderDefaults.color(
                                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                                    ),
                                    highlight = PlaceholderHighlight.shimmer(
                                        highlightColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                            alpha = 0.35f,
                                        ),
                                    ),
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun DashboardScreenPreview() {
    AirQualityMonitorTheme {
        DashboardScreen(
            onOpenSearch = {},
            onOpenFavorites = {},
            onOpenHistory = {},
            uiState = DashboardUiState(
                snapshot = sampleSnapshot(),
                locationAddress = "Cairo, Egypt",
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun DashboardScreenPreviewLoadingState() {
    AirQualityMonitorTheme {
        DashboardScreen(
            onOpenSearch = {},
            onOpenFavorites = {},
            onOpenHistory = {},
            uiState = DashboardUiState(
                isLoading = true,
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun DashboardScreenPreviewErrorStatePermissionDenied() {
    AirQualityMonitorTheme {
        DashboardScreen(
            onOpenSearch = {},
            onOpenFavorites = {},
            onOpenHistory = {},
            uiState = DashboardUiState(
                error = DashboardError.PermissionDenied,
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun DashboardScreenPreviewErrorStateLocationUnavailable() {
    AirQualityMonitorTheme {
        DashboardScreen(
            onOpenSearch = {},
            onOpenFavorites = {},
            onOpenHistory = {},
            uiState = DashboardUiState(
                error = DashboardError.LocationUnavailable,
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun DashboardScreenPreviewErrorStateOffLineNoCache() {
    AirQualityMonitorTheme {
        DashboardScreen(
            onOpenSearch = {},
            onOpenFavorites = {},
            onOpenHistory = {},
            uiState = DashboardUiState(
                error = DashboardError.OfflineNoCache,
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun DashboardScreenPreviewErrorRefreshFailed() {
    AirQualityMonitorTheme {
        DashboardScreen(
            onOpenSearch = {},
            onOpenFavorites = {},
            onOpenHistory = {},
            uiState = DashboardUiState(
                refreshFailed = true,
            ),
        )
    }
}

private fun sampleSnapshot() = AirQualitySnapshot(
    location = Location(30.0444, 31.2357, "Cairo, Egypt"),
    timestamp = "2026-09-28T12:00",
    europeanAqi = 42,
    pm2_5 = 12.5,
    pm10 = 20.0,
    carbonMonoxide = 300.0,
    nitrogenDioxide = 18.0,
    ozone = 46.0,
)
