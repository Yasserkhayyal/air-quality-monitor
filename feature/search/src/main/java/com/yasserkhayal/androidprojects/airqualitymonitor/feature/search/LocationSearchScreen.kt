package com.yasserkhayal.androidprojects.airqualitymonitor.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yasserkhayal.androidprojects.airqualitymonitor.core.designsystem.theme.AirQualityMonitorTheme
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataError
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location

@Composable
fun LocationSearchRoute(
    onBack: () -> Unit,
    onLocationSelected: (Location) -> Unit,
) {
    val viewModel: SearchViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SearchEffect.OpenDashboard -> onLocationSelected(effect.location)
            }
        }
    }

    LocationSearchScreen(
        uiState = uiState,
        onBack = onBack,
        onIntent = viewModel::onIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LocationSearchScreen(
    uiState: SearchUiState,
    onBack: () -> Unit,
    onIntent: (SearchIntent) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_title)) },
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
        val progressDescription = stringResource(
            if (uiState.isSelecting) R.string.search_saving_location else R.string.search_progress,
        )
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = { onIntent(SearchIntent.QueryChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search_field_label)) },
                singleLine = true,
                trailingIcon = {
                    if (uiState.isSearching || uiState.isSelecting) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp)
                                .semantics { contentDescription = progressDescription },
                            strokeWidth = 2.dp,
                        )
                    }
                },
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    !uiState.canSearch -> item {
                        Text(stringResource(R.string.search_hint))
                    }

                    uiState.isSearching || uiState.isSelecting -> Unit

                    uiState.error is SearchError.SearchFailed -> item {
                        val dataError = uiState.error.dataError
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                searchErrorMessage(dataError),
                                color = MaterialTheme.colorScheme.error,
                            )
                            TextButton(onClick = { onIntent(SearchIntent.Retry) }) {
                                Text(stringResource(R.string.search_retry))
                            }
                        }
                    }

                    uiState.isEmpty -> item {
                        Text(stringResource(R.string.search_empty))
                    }

                    else -> {
                        if (uiState.error is SearchError.SelectionFailed) {
                            val dataError = uiState.error.dataError
                            item {
                                Text(
                                    searchErrorMessage(dataError),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        items(
                            items = uiState.results,
                            key = { location -> "${location.latitude}:${location.longitude}:${location.address}" },
                        ) { location ->
                            LocationResultItem(
                                location = location,
                                onClick = { onIntent(SearchIntent.LocationSelected(location)) },
                                enabled = !uiState.isSelecting,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Returns localized user-facing copy for a typed data-layer failure. */
@Composable
private fun searchErrorMessage(error: DataError): String = when (error) {
    is DataError.Network -> stringResource(R.string.search_error_network)

    is DataError.Server -> when {
        error.code == 429 -> stringResource(R.string.search_error_rate_limited)
        error.code in 500..599 -> stringResource(R.string.search_error_server)
        else -> stringResource(R.string.search_error_request_rejected)
    }

    is DataError.Configuration -> stringResource(R.string.search_error_configuration)

    is DataError.RateLimited -> stringResource(R.string.search_error_rate_limited)

    is DataError.ServiceUnavailable -> stringResource(R.string.search_error_server)

    is DataError.UnexpectedResponse,
    is DataError.GenericError,
    -> stringResource(R.string.search_error_unexpected)

    is DataError.OfflineNoCache -> stringResource(R.string.search_error_offline_no_cache)

    is DataError.Local -> stringResource(R.string.search_error_local)
}

@Composable
private fun LocationResultItem(
    location: Location,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                location.address ?: stringResource(R.string.search_unknown_location),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(R.string.search_coordinates, location.latitude, location.longitude),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun LocationSearchScreenPreview() {
    AirQualityMonitorTheme {
        LocationSearchScreen(
            uiState = SearchUiState(),
            onBack = {},
            onIntent = {},
        )
    }
}
