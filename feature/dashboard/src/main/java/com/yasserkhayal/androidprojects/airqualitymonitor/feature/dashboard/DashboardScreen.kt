package com.yasserkhayal.androidprojects.airqualitymonitor.feature.dashboard

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenSearch: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenHistory: () -> Unit
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
                Text(stringResource(R.string.dashboard_location_selected), style = MaterialTheme.typography.bodyMedium)
            }
            item { DashboardContent(onOpenHistory = onOpenHistory) }
        }
    }
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
private fun DashboardContent(onOpenHistory: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(stringResource(R.string.dashboard_aqi), style = MaterialTheme.typography.labelLarge)
                Text(
                    stringResource(R.string.dashboard_measurement_not_available),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(stringResource(R.string.dashboard_data_waiting), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.dashboard_timestamp_unavailable), style = MaterialTheme.typography.labelMedium)
            }
        }

        Text(stringResource(R.string.dashboard_pollutants), style = MaterialTheme.typography.titleMedium)
        PollutantGrid()

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.dashboard_history_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.dashboard_history_preview), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onOpenHistory) { Text(stringResource(R.string.dashboard_view_history)) }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {}) { Text(stringResource(R.string.dashboard_use_current_location)) }
            AssistChip(onClick = {}, label = { Text(stringResource(R.string.dashboard_search)) })
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardContentPreview() {
    MaterialTheme {
        DashboardContent(onOpenHistory = {})
    }
}

@Composable
private fun PollutantGrid() {
    val pollutants = listOf("PM2.5", "PM10", "NO2", "O3", "CO")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        pollutants.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { pollutant ->
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(pollutant, style = MaterialTheme.typography.labelLarge)
                            Text(stringResource(R.string.dashboard_measurement_not_available), style = MaterialTheme.typography.bodyMedium)
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
