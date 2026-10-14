package com.yasserkhayal.androidprojects.airqualitymonitor.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.tools.screenshot.PreviewTest
import com.yasserkhayal.androidprojects.airqualitymonitor.core.designsystem.theme.AirQualityMonitorTheme
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataError
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataFreshness
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.HistoricalAirQualityPoint
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import java.io.IOException

/** Captures compact hourly measurements and partial metric availability. */
@PreviewTest
@PreviewLightDark
@Composable
fun HistoricalAirQualityPopulatedScreenshot() {
    HistoryScreenshotContent(
        HistoricalAirQualityUiState(
            isLoading = false,
            location = Location(30.0444, 31.2357, "Cairo, Egypt"),
            points = listOf(
                HistoricalAirQualityPoint(
                    timestamp = "2026-10-13T22:00",
                    europeanAqi = 35,
                    pm2_5 = 10.5,
                    pm10 = 15.0,
                    carbonMonoxide = 250.0,
                    nitrogenDioxide = 15.0,
                    ozone = 40.0,
                ),
                HistoricalAirQualityPoint(
                    timestamp = "2026-10-13T21:00",
                    europeanAqi = 42,
                    pm2_5 = 12.5,
                    pm10 = null,
                    carbonMonoxide = 300.0,
                    nitrogenDioxide = 18.0,
                    ozone = 46.0,
                ),
            ),
            freshness = DataFreshness.Fresh,
        ),
    )
}

/** Captures cached historical data with an outdated-data indication. */
@PreviewTest
@PreviewLightDark
@Composable
fun HistoricalAirQualityStaleCacheScreenshot() {
    HistoryScreenshotContent(
        HistoricalAirQualityUiState(
            isLoading = false,
            location = Location(30.0444, 31.2357, "Cairo, Egypt"),
            points = listOf(
                HistoricalAirQualityPoint(
                    timestamp = "2026-10-13T22:00",
                    europeanAqi = 35,
                    pm2_5 = 10.5,
                    pm10 = null,
                    carbonMonoxide = 250.0,
                    nitrogenDioxide = 15.0,
                    ozone = 40.0,
                ),
            ),
            freshness = DataFreshness.Stale,
            refreshError = DataError.Network(IOException("offline")),
        ),
    )
}

/** Captures unavailable historical data and actionable retry. */
@PreviewTest
@PreviewLightDark
@Composable
fun HistoricalAirQualityUnavailableScreenshot() {
    HistoryScreenshotContent(
        HistoricalAirQualityUiState(
            isLoading = false,
            error = DataError.OfflineNoCache(IOException("offline")),
        ),
    )
}

/** Applies the shared theme to one screenshot state. */
@Composable
private fun HistoryScreenshotContent(uiState: HistoricalAirQualityUiState) {
    AirQualityMonitorTheme {
        HistoricalAirQualityScreen(
            uiState = uiState,
            onBack = {},
            onIntent = {},
        )
    }
}
