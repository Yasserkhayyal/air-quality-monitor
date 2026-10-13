package com.yasserkhayal.androidprojects.airqualitymonitor.feature.favorites

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.tools.screenshot.PreviewTest
import com.yasserkhayal.androidprojects.airqualitymonitor.core.designsystem.theme.AirQualityMonitorTheme
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location

/** Captures the Favorites empty state. */
@PreviewTest
@PreviewLightDark
@Composable
fun FavoritesEmptyScreenshot() {
    FavoritesScreenshotContent(FavoritesUiState(isLoading = false))
}

/** Captures favorites in oldest-first display order. */
@PreviewTest
@PreviewLightDark
@Composable
fun FavoritesListScreenshot() {
    FavoritesScreenshotContent(
        FavoritesUiState(
            isLoading = false,
            favorites = listOf(
                Location(30.0444, 31.2357, "Cairo, Egypt"),
                Location(51.5079, -0.1276, "London, UK"),
            ),
        ),
    )
}

@PreviewTest
@PreviewLightDark
@Composable
fun FavoritesRemoveFailureScreenshot() {
    FavoritesScreenshotContent(
        FavoritesUiState(
            isLoading = false,
            favorites = listOf(
                Location(30.0444, 31.2357, "Cairo, Egypt"),
                Location(51.5079, -0.1276, "London, UK"),
            ),
            error = FavoritesError.RemoveFailed,
        ),
    )
}

/** Captures user-facing feedback when favorites cannot be loaded. */
@PreviewTest
@PreviewLightDark
@Composable
fun FavoritesLoadFailureScreenshot() {
    FavoritesScreenshotContent(
        FavoritesUiState(
            isLoading = false,
            error = FavoritesError.LoadFailed,
        ),
    )
}

@Composable
private fun FavoritesScreenshotContent(uiState: FavoritesUiState) {
    AirQualityMonitorTheme {
        FavoritesScreen(
            uiState = uiState,
            onBack = {},
            onSearch = {},
            onIntent = {},
        )
    }
}
