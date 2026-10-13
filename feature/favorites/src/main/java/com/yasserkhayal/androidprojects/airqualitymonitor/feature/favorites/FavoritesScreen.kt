package com.yasserkhayal.androidprojects.airqualitymonitor.feature.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.yasserkhayal.androidprojects.airqualitymonitor.core.designsystem.theme.AirQualityMonitorTheme
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location

const val FAVORITE_ITEM_PREFIX = "favorite_item_"
const val FAVORITE_REMOVE_PREFIX = "favorite_remove_"

/**
 * Connects Favorites state/effects to app-owned navigation callbacks.
 */
@Composable
fun FavoritesRoute(
    onBack: () -> Unit,
    onLocationSelected: (Location) -> Unit,
    onSearch: () -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is FavoritesEffect.OpenDashboard -> onLocationSelected(effect.location)
            }
        }
    }

    FavoritesScreen(
        uiState = uiState,
        onBack = onBack,
        onSearch = onSearch,
        onIntent = viewModel::onIntent,
    )
}

/**
 * Renders loading, empty, populated, and failure states for favorite locations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    uiState: FavoritesUiState,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onIntent: (FavoritesIntent) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.favorites_title)) },
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
        when {
            uiState.isLoading -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }

            uiState.favorites.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (uiState.error == FavoritesError.LoadFailed) {
                    Text(
                        stringResource(R.string.favorites_load_error),
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(
                        onClick = { onIntent(FavoritesIntent.Retry) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.favorites_retry))
                    }
                } else {
                    Text(
                        stringResource(R.string.favorites_empty_title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        stringResource(R.string.favorites_empty_message),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Button(onClick = onSearch, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.favorites_search))
                }
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (uiState.error != null) {
                    item {
                        Text(
                            stringResource(uiState.error.messageResource),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                }
                items(
                    items = uiState.favorites,
                    key = { "${it.latitude}:${it.longitude}" },
                ) { location ->
                    FavoriteLocationRow(
                        location = location,
                        enabled = !uiState.isSelecting && uiState.removingLocation == null,
                        onSelect = { onIntent(FavoritesIntent.LocationSelected(location)) },
                        onRemove = { onIntent(FavoritesIntent.RemoveFavorite(location)) },
                    )
                }

                item {
                    Button(onClick = onSearch, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.favorites_search))
                    }
                }
            }
        }
    }
}

/** Renders a selectable favorite with its explicit remove action. */
@Composable
private fun FavoriteLocationRow(
    location: Location,
    enabled: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        onClick = onSelect,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(FAVORITE_ITEM_PREFIX + location.latitude + ":" + location.longitude),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(
                    location.address ?: stringResource(R.string.favorites_unknown_location),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.favorites_coordinates, location.latitude, location.longitude),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IconButton(
                onClick = onRemove,
                enabled = enabled,
                modifier = Modifier.testTag(FAVORITE_REMOVE_PREFIX + location.latitude + ":" + location.longitude),
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(
                        R.string.favorites_remove,
                        location.address ?: stringResource(R.string.favorites_unknown_location),
                    ),
                )
            }
        }
    }
}

/** Maps each feature error to its localized presentation string resource. */
private val FavoritesError.messageResource: Int
    get() = when (this) {
        FavoritesError.LoadFailed -> R.string.favorites_load_error
        FavoritesError.RemoveFailed -> R.string.favorites_remove_error
        FavoritesError.SelectionFailed -> R.string.favorites_selection_error
    }

/** Previews the empty Favorites state. */
@PreviewLightDark
@Composable
private fun FavoritesEmptyPreview() {
    AirQualityMonitorTheme {
        FavoritesScreen(
            uiState = FavoritesUiState(isLoading = false),
            onBack = {},
            onSearch = {},
            onIntent = {},
        )
    }
}

/** Previews the persisted Favorites list. */
@PreviewLightDark
@Composable
private fun FavoritesListPreview() {
    AirQualityMonitorTheme {
        FavoritesScreen(
            uiState = FavoritesUiState(
                isLoading = false,
                favorites = listOf(Location(30.0444, 31.2357, "Cairo, Egypt")),
            ),
            onBack = {},
            onSearch = {},
            onIntent = {},
        )
    }
}
