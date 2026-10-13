package com.yasserkhayal.androidprojects.airqualitymonitor.feature.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.tools.screenshot.PreviewTest
import com.yasserkhayal.androidprojects.airqualitymonitor.core.designsystem.theme.AirQualityMonitorTheme
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualitySnapshot
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataError
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import java.io.IOException

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
fun DashboardLocationRestoreFailedScreenshot() {
    DashboardScreenshotContent(
        DashboardUiState(
            isLoading = true,
            locationRestoreFailed = true,
        ),
    )
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
    DashboardScreenshotContent(
        DashboardUiState(
            error = DashboardError.DataUnavailable,
            dataError = DataError.GenericError(IllegalStateException("unexpected failure")),
        ),
    )
}

/** Captures localized messaging for a remote air-quality service outage. */
@PreviewTest
@PreviewLightDark
@Composable
fun DashboardServerUnavailableScreenshot() {
    DashboardScreenshotContent(
        DashboardUiState(
            error = DashboardError.DataUnavailable,
            dataError = DataError.Server(503, IOException("service unavailable")),
        ),
    )
}

/** Captures user-facing feedback for service configuration failures. */
@PreviewTest
@PreviewLightDark
@Composable
fun DashboardConfigurationErrorScreenshot() {
    DashboardScreenshotContent(
        DashboardUiState(
            error = DashboardError.DataUnavailable,
            dataError = DataError.Configuration(IllegalStateException("configuration")),
        ),
    )
}

/** Captures user-facing feedback when the service rate limits requests. */
@PreviewTest
@PreviewLightDark
@Composable
fun DashboardRateLimitedErrorScreenshot() {
    DashboardScreenshotContent(
        DashboardUiState(
            error = DashboardError.DataUnavailable,
            dataError = DataError.RateLimited(IllegalStateException("rate limited")),
        ),
    )
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

/** Captures the Dashboard action after its active location is saved as a favorite. */
@PreviewTest
@PreviewLightDark
@Composable
fun DashboardLocationFavoritedScreenshot() {
    DashboardScreenshotContent(
        DashboardUiState(
            snapshot = sampleSnapshot(),
            activeLocation = sampleSnapshot().location,
            locationAddress = "Cairo, Egypt",
            isLocationFavorite = true,
        ),
    )
}

@PreviewTest
@PreviewLightDark
@Composable
fun DashboardLocationNotSavedScreenshot() {
    DashboardScreenshotContent(
        DashboardUiState(
            snapshot = sampleSnapshot(),
            locationAddress = "Cairo, Egypt",
            locationPersistenceFailed = true,
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
            refreshError = DataError.Network(IOException("offline")),
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
            refreshError = DataError.Network(IOException("offline")),
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
            dataError = DataError.OfflineNoCache(IOException("offline")),
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
