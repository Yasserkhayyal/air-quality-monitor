package com.yasserkhayal.androidprojects.airqualitymonitor.feature.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yasserkhayal.androidprojects.airqualitymonitor.core.designsystem.theme.AirQualityMonitorTheme
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataError
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataFreshness
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.HistoricalAirQualityPoint
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Verifies the history screen's compact hourly presentation and retry behavior.
 */
@RunWith(AndroidJUnit4::class)
class HistoryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    /** Keeps hourly data and unavailable metric values visible in compact rows. */
    @Test
    fun showsHistoricalPointAndUnavailableMetric() {
        composeRule.setContent {
            AirQualityMonitorTheme {
                HistoricalAirQualityScreen(
                    uiState = HistoricalAirQualityUiState(
                        isLoading = false,
                        location = Location(30.0444, 31.2357, "Cairo, Egypt"),
                        points = listOf(
                            HistoricalAirQualityPoint(
                                timestamp = "2026-10-13T23:00",
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
                    onBack = {},
                    onIntent = {},
                )
            }
        }

        composeRule.onNodeWithTag(HISTORICAL_POINT_PREFIX + "2026-10-13T23:00").assertExists()
        composeRule.onNodeWithText("42").assertExists()
        composeRule.onNodeWithText("PM10").assertExists()
        composeRule.onNodeWithText("Unavailable").assertExists()
    }

    /** Emits retry intent when no historical data is available. */
    @Test
    fun retryButtonEmitsHistoricalRetryIntent() {
        var retryCount = 0
        composeRule.setContent {
            AirQualityMonitorTheme {
                HistoricalAirQualityScreen(
                    uiState = HistoricalAirQualityUiState(
                        isLoading = false,
                        error = DataError.Network(IOException("offline")),
                    ),
                    onBack = {},
                    onIntent = { intent ->
                        if (intent == HistoricalAirQualityIntent.Retry) retryCount++
                    },
                )
            }
        }

        composeRule.onNodeWithTag(HISTORICAL_RETRY_BUTTON).assertIsDisplayed().performClick()

        assertEquals(1, retryCount)
    }
}
