package com.yasserkhayal.androidprojects.airqualitymonitor.feature.search

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.tools.screenshot.PreviewTest
import com.yasserkhayal.androidprojects.airqualitymonitor.core.designsystem.theme.AirQualityMonitorTheme
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataError
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import java.io.IOException

@PreviewTest
@PreviewLightDark
@Composable
fun LocationSearchInitialScreenshot() {
    SearchScreenshotContent(SearchUiState())
}

@PreviewTest
@PreviewLightDark
@Composable
fun LocationSearchLoadingScreenshot() {
    SearchScreenshotContent(SearchUiState(query = "Cairo", isSearching = true))
}

@PreviewTest
@PreviewLightDark
@Composable
fun LocationSearchResultsScreenshot() {
    SearchScreenshotContent(
        SearchUiState(
            query = "Cairo",
            hasSearched = true,
            results = listOf(
                Location(30.0444, 31.2357, "Cairo, Egypt"),
                Location(37.0053, -89.1765, "Cairo, Illinois, United States"),
            ),
        ),
    )
}

@PreviewTest
@PreviewLightDark
@Composable
fun LocationSearchEmptyScreenshot() {
    SearchScreenshotContent(SearchUiState(query = "No place", hasSearched = true))
}

@PreviewTest
@PreviewLightDark
@Composable
fun LocationSearchErrorScreenshot() {
    SearchScreenshotContent(
        SearchUiState(
            query = "Cairo",
            hasSearched = true,
            error = SearchError.SearchFailed(DataError.Network(IOException("offline"))),
        ),
    )
}

/** Captures localized feedback for a failed server response. */
@PreviewTest
@PreviewLightDark
@Composable
fun LocationSearchServerErrorScreenshot() {
    SearchScreenshotContent(
        SearchUiState(
            query = "Cairo",
            hasSearched = true,
            error = SearchError.SearchFailed(
                DataError.Server(503, IOException("service unavailable")),
            ),
        ),
    )
}

/** Captures localized feedback when search service configuration is unavailable. */
@PreviewTest
@PreviewLightDark
@Composable
fun LocationSearchConfigurationErrorScreenshot() {
    SearchScreenshotContent(
        SearchUiState(
            query = "Cairo",
            hasSearched = true,
            error = SearchError.SearchFailed(
                DataError.Configuration(IllegalStateException("configuration")),
            ),
        ),
    )
}

/** Captures localized feedback when search requests are rate limited. */
@PreviewTest
@PreviewLightDark
@Composable
fun LocationSearchRateLimitedErrorScreenshot() {
    SearchScreenshotContent(
        SearchUiState(
            query = "Cairo",
            hasSearched = true,
            error = SearchError.SearchFailed(
                DataError.RateLimited(IllegalStateException("rate limited")),
            ),
        ),
    )
}

/** Captures localized feedback for an invalid geocoding response. */
@PreviewTest
@PreviewLightDark
@Composable
fun LocationSearchUnexpectedResponseScreenshot() {
    SearchScreenshotContent(
        SearchUiState(
            query = "Cairo",
            hasSearched = true,
            error = SearchError.SearchFailed(
                DataError.UnexpectedResponse(IllegalStateException("unexpected response")),
            ),
        ),
    )
}

@PreviewTest
@PreviewLightDark
@Composable
fun LocationSearchSelectionErrorScreenshot() {
    SearchScreenshotContent(
        SearchUiState(
            query = "Cairo",
            hasSearched = true,
            results = listOf(Location(30.0444, 31.2357, "Cairo, Egypt")),
            error = SearchError.SelectionFailed(
                DataError.Local(IOException("database unavailable")),
            ),
        ),
    )
}

@Composable
private fun SearchScreenshotContent(uiState: SearchUiState) {
    AirQualityMonitorTheme {
        LocationSearchScreen(
            uiState = uiState,
            onBack = {},
            onIntent = {},
        )
    }
}
