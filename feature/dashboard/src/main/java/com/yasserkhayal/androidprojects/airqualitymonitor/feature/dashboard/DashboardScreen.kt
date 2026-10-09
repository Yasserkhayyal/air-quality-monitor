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
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
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
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AqiCategory
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location

const val SEARCH_TOP_BAR_ICON_BUTTON = "search_top_bar_icon_button"
const val FAVORITES_TOP_BAR_ICON_BUTTON = "favorites_top_bar_icon_button"
const val SEARCH_CHIP = "search_chip"
private val AqiCategory.labelRes: Int
    get() = when (this) {
        AqiCategory.GOOD -> R.string.aqi_category_good
        AqiCategory.FAIR -> R.string.aqi_category_fair
        AqiCategory.MODERATE -> R.string.aqi_category_moderate
        AqiCategory.POOR -> R.string.aqi_category_poor
        AqiCategory.VERY_POOR -> R.string.aqi_category_very_poor
    }

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
                        else -> uiState.locationAddress ?: stringResource(R.string.dashboard_location_selected)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            item {
                DashboardContent(
                    uiState = uiState,
                    onOpenHistory = onOpenHistory,
                    onOpenSearch = onOpenSearch,
                    onRetry = { onIntent(DashboardIntent.Retry) },
                )
            }
        }
    }
}

@Composable
fun DashboardRoute(
    onRequestLocationPermission: ((Boolean) -> Unit) -> Unit,
    onRequestCurrentLocation: ((Location?) -> Unit) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.onIntent(DashboardIntent.Started)
        viewModel.effects.collect { effect ->
            when (effect) {
                DashboardEffect.RequestLocationPermission ->
                    onRequestLocationPermission { granted ->
                        viewModel.onIntent(DashboardIntent.OnPermissionResult(granted))
                    }

                DashboardEffect.RequestCurrentLocation ->
                    onRequestCurrentLocation { location ->
                        viewModel.onIntent(DashboardIntent.OnLocationResult(location))
                    }
            }
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
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
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
                    Text(
                        stringResource(R.string.dashboard_refresh_failed),
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
            Text(
                stringResource(R.string.dashboard_offline_no_cache),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        stringResource(R.string.dashboard_aqi),
                        modifier = Modifier.placeholder(
                            visible = uiState.isLoading,
                            color = PlaceholderDefaults.color(
                                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                            highlight = PlaceholderHighlight.shimmer(highlightColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        uiState.snapshot?.europeanAqi?.toString()
                            ?: stringResource(R.string.dashboard_measurement_not_available),
                        modifier = Modifier.placeholder(
                            visible = uiState.isLoading,
                            color = PlaceholderDefaults.color(backgroundColor = MaterialTheme.colorScheme.primaryContainer),
                            highlight = PlaceholderHighlight.shimmer(highlightColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                        ),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        uiState.snapshot?.category?.let { category ->
                            stringResource(category.labelRes)
                        } ?: stringResource(R.string.dashboard_data_unavailable),
                        modifier = Modifier.placeholder(
                            visible = uiState.isLoading,
                            color = PlaceholderDefaults.color(backgroundColor = MaterialTheme.colorScheme.primaryContainer),
                            highlight = PlaceholderHighlight.shimmer(highlightColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                        ),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        uiState.snapshot?.timestamp?.let(::formatTimestampForDisplay)
                            ?: stringResource(R.string.dashboard_timestamp_unavailable),
                        modifier = Modifier.placeholder(
                            visible = uiState.isLoading,
                            color = PlaceholderDefaults.color(backgroundColor = MaterialTheme.colorScheme.primaryContainer),
                            highlight = PlaceholderHighlight.shimmer(highlightColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    if (uiState.isStale) {
                        Text(
                            stringResource(R.string.dashboard_data_may_be_outdated),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }

            Text(
                stringResource(R.string.dashboard_pollutants),
                modifier = Modifier.placeholder(
                    visible = uiState.isLoading,
                    color = PlaceholderDefaults.color(backgroundColor = MaterialTheme.colorScheme.background),
                    highlight = PlaceholderHighlight.shimmer(highlightColor = MaterialTheme.colorScheme.background.copy(alpha = 0.35f)),
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            PollutantGrid(uiState.isLoading, uiState.snapshot)
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
            Button(onClick = onRetry) {
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

@Composable
private fun PollutantGrid(isLoading: Boolean = false, snapshot: AirQualitySnapshot? = null) {
    val pollutants = listOf(
        "PM2.5" to snapshot?.pm2_5,
        "PM10" to snapshot?.pm10,
        "NO2" to snapshot?.nitrogenDioxide,
        "O3" to snapshot?.ozone,
        "CO" to snapshot?.carbonMonoxide,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        pollutants.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { (pollutant, value) ->
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                pollutant,
                                modifier = Modifier.placeholder(
                                    visible = isLoading,
                                    color = PlaceholderDefaults.color(
                                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                                    ),
                                    highlight = PlaceholderHighlight.shimmer(highlightColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                ),
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Text(
                                value?.toString()
                                    ?: stringResource(R.string.dashboard_measurement_not_available),
                                modifier = Modifier.placeholder(
                                    visible = isLoading,
                                    color = PlaceholderDefaults.color(
                                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                                    ),
                                    highlight = PlaceholderHighlight.shimmer(highlightColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
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
