package com.yasserkhayal.androidprojects.airqualitymonitor.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.yasserkhayal.androidprojects.airqualitymonitor.core.designsystem.theme.AirQualityMonitorTheme

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
                            contentDescription = stringResource(R.string.content_description_back),
                        )
                    }
                },

            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.dashboard_location_selected), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.history_unavailable_title), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.history_unavailable_message),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = {}) { Text(stringResource(R.string.history_retry)) }
        }
    }
}

@PreviewLightDark
@Composable
private fun HistoricalAirQualityScreenPreview() {
    AirQualityMonitorTheme {
        HistoricalAirQualityScreen(onBack = {})
    }
}
