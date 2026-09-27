package com.yasserkhayal.androidprojects.airqualitymonitor.feature.dashboard

import android.location.Geocoder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualitySnapshot
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AqiCategory
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location

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
    onIntent: (DashboardIntent) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_title)) },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.content_description_search))
                    }
                    IconButton(onClick = onOpenFavorites) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = stringResource(R.string.content_description_favorites))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.dashboard_current_location), style = MaterialTheme.typography.titleMedium)
                Text(uiState.locationAddress?: stringResource(R.string.dashboard_location_selected),
                    style = MaterialTheme.typography.bodyMedium)
            }
            item {
                DashboardContent(
                    uiState = uiState,
                    onOpenHistory = onOpenHistory,
                    onRetry = { onIntent(DashboardIntent.Retry) }
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
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.onIntent(DashboardIntent.Started)
        viewModel.effects.collect { effect ->
            when (effect) {
                DashboardEffect.RequestLocationPermission ->
                    onRequestLocationPermission { granted ->
                        viewModel.onIntent(DashboardIntent.PermissionResult(granted))
                    }
                DashboardEffect.RequestCurrentLocation ->
                    onRequestCurrentLocation { location ->
                        viewModel.onIntent(DashboardIntent.LocationResult(location))
                    }
            }
        }
    }

    DashboardScreen(
        onOpenSearch = onOpenSearch,
        onOpenFavorites = onOpenFavorites,
        onOpenHistory = onOpenHistory,
        uiState = uiState,
        onIntent = viewModel::onIntent
    )
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    MaterialTheme {
        DashboardScreen(
            onOpenSearch = {},
            onOpenFavorites = {},
            onOpenHistory = {}
        )
    }
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onOpenHistory: () -> Unit,
    onRetry: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(stringResource(R.string.dashboard_aqi), style = MaterialTheme.typography.labelLarge)
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                }
                Text(
                    uiState.snapshot?.europeanAqi?.toString()
                        ?: stringResource(R.string.dashboard_measurement_not_available),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    uiState.snapshot?.category?.let { category ->
                        stringResource(category.labelRes)
                    } ?: stringResource(
                        if (uiState.isLoading) R.string.dashboard_data_waiting
                        else R.string.dashboard_data_unavailable
                    ),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    uiState.snapshot?.timestamp
                        ?: stringResource(R.string.dashboard_timestamp_unavailable),
                    style = MaterialTheme.typography.labelMedium
                )
            }

        }

        Text(stringResource(R.string.dashboard_pollutants), style = MaterialTheme.typography.titleMedium)
        PollutantGrid(uiState.snapshot)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.dashboard_history_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.dashboard_history_preview), style = MaterialTheme.typography.bodyMedium)
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
                    }
                )
            }
            AssistChip(onClick = {}, label = { Text(stringResource(R.string.dashboard_search)) })
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardContentPreview() {
    MaterialTheme {
        DashboardContent(
            uiState = DashboardUiState(),
            onOpenHistory = {},
            onRetry = {}
        )
    }
}

@Composable
private fun PollutantGrid(snapshot: AirQualitySnapshot? = null) {
    val pollutants = listOf(
        "PM2.5" to snapshot?.pm2_5,
        "PM10" to snapshot?.pm10,
        "NO2" to snapshot?.nitrogenDioxide,
        "O3" to snapshot?.ozone,
        "CO" to snapshot?.carbonMonoxide
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        pollutants.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (pollutant, value) ->
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(pollutant, style = MaterialTheme.typography.labelLarge)
                            Text(
                                value?.toString()
                                    ?: stringResource(R.string.dashboard_measurement_not_available),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PollutantGridPreview() {
    MaterialTheme {
        PollutantGrid()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricalAirQualityScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back)
                        )
                    }
                }

            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.dashboard_location_selected), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.history_unavailable_title), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.history_unavailable_message),
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = {}) { Text(stringResource(R.string.history_retry)) }
        }
    }

}

@Preview(showBackground = true)
@Composable
private fun HistoricalAirQualityScreenPreview() {
    MaterialTheme {
        HistoricalAirQualityScreen(onBack = {})
    }
}
