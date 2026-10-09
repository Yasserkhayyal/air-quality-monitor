package com.yasserkhayal.androidprojects.airqualitymonitor.feature.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.tools.screenshot.PreviewTest
import com.yasserkhayal.androidprojects.airqualitymonitor.core.designsystem.theme.AirQualityMonitorTheme
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualitySnapshot
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location

@PreviewTest
@PreviewLightDark
@Composable
fun DashboardLoadingScreenshot() {
    DashboardScreenshotContent(DashboardUiState(isLoading = true))
}

@PreviewTest
@PreviewLightDark
@Composable
fun DashboardLocationPermissionDeniedScreenshot() {
    DashboardScreenshotContent(DashboardUiState(error = DashboardError.PermissionDenied))
}

@PreviewTest
@PreviewLightDark
@Composable
fun DashboardLocationUnavailableScreenshot() {
    DashboardScreenshotContent(DashboardUiState(error = DashboardError.LocationUnavailable))
}

@PreviewTest
@PreviewLightDark
@Composable
fun DashboardUnavailableScreenshot() {
    DashboardScreenshotContent(DashboardUiState(error = DashboardError.DataUnavailable))
}

@PreviewTest
@PreviewLightDark
@Composable
fun DashboardRemoteSuccessScreenshot() {
    DashboardScreenshotContent(
        DashboardUiState(
            snapshot = sampleSnapshot(),
            locationAddress = "Cairo, Egypt",
        ),
    )
}

@PreviewTest
@PreviewLightDark
@Composable
fun DashboardFreshCachedFallbackScreenshot() {
    DashboardScreenshotContent(
        DashboardUiState(
            snapshot = sampleSnapshot(),
            locationAddress = "Cairo, Egypt",
            refreshFailed = true,
        ),
    )
}

@PreviewTest
@PreviewLightDark
@Composable
fun DashboardStaleCachedFallbackScreenshot() {
    DashboardScreenshotContent(
        DashboardUiState(
            snapshot = sampleSnapshot(),
            locationAddress = "Cairo, Egypt",
            isStale = true,
            refreshFailed = true,
        ),
    )
}

@PreviewTest
@PreviewLightDark
@Composable
fun DashboardOfflineWithoutCacheScreenshot() {
    DashboardScreenshotContent(
        DashboardUiState(
            refreshFailed = true,
            error = DashboardError.OfflineNoCache,
        ),
    )
}

@Composable
private fun DashboardScreenshotContent(uiState: DashboardUiState) {
    AirQualityMonitorTheme {
        DashboardScreen(
            onOpenSearch = {},
            onOpenFavorites = {},
            onOpenHistory = {},
            uiState = uiState,
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
