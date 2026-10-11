package com.yasserkhayal.androidprojects.airqualitymonitor.feature.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yasserkhayal.androidprojects.airqualitymonitor.core.designsystem.theme.AirQualityMonitorTheme
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualitySnapshot
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataError
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tappingInlineRetryEmitsRetryIntent() {
        var retryCount = 0
        composeRule.setContent {
            AirQualityMonitorTheme {
                DashboardScreen(
                    onOpenSearch = {},
                    onOpenFavorites = {},
                    onOpenHistory = {},
                    uiState = DashboardUiState(
                        snapshot = sampleSnapshot(),
                        refreshFailed = true,
                    ),
                    onIntent = { intent ->
                        if (intent == DashboardIntent.Retry) retryCount++
                    },
                )
            }
        }

        composeRule.onAllNodesWithText("Retry")
            .assertCountEquals(1)[0]
            .assertIsDisplayed()
            .performClick()

        assertEquals(1, retryCount)
    }

    @Test
    fun showsWarningWhenSelectedLocationCouldNotBeSaved() {
        composeRule.setContent {
            AirQualityMonitorTheme {
                DashboardScreen(
                    onOpenSearch = {},
                    onOpenFavorites = {},
                    onOpenHistory = {},
                    uiState = DashboardUiState(
                        snapshot = sampleSnapshot(),
                        locationPersistenceFailed = true,
                    ),
                )
            }
        }

        composeRule.onNodeWithText(
            "This location could not be saved and will not be restored after an app restart.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("42").assertIsDisplayed()
    }

    @Test
    fun showsDatabaseWarningWhenSavedLocationCannotBeRestored() {
        composeRule.setContent {
            AirQualityMonitorTheme {
                DashboardScreen(
                    onOpenSearch = {},
                    onOpenFavorites = {},
                    onOpenHistory = {},
                    uiState = DashboardUiState(
                        isLoading = true,
                        locationRestoreFailed = true,
                    ),
                )
            }
        }

        composeRule.onNodeWithText(
            "We couldn’t access your saved location because of a database problem. The app will try your current location.",
        ).assertIsDisplayed()
    }

    /** Server failures have service-specific copy rather than connectivity guidance. */
    @Test
    fun showsServerFailureInsteadOfBlamingConnectivity() {
        composeRule.setContent {
            AirQualityMonitorTheme {
                DashboardScreen(
                    onOpenSearch = {},
                    onOpenFavorites = {},
                    onOpenHistory = {},
                    uiState = DashboardUiState(
                        error = DashboardError.DataUnavailable,
                        dataError = DataError.Server(503, IOException("service unavailable")),
                    ),
                )
            }
        }

        composeRule.onNodeWithText(
            "The air-quality service is temporarily unavailable. Please try again later.",
        ).assertIsDisplayed()
    }

    /** A cache remains visible while the user receives a network-specific refresh message. */
    @Test
    fun showsConnectivityFailureForCachedDataRefresh() {
        composeRule.setContent {
            AirQualityMonitorTheme {
                DashboardScreen(
                    onOpenSearch = {},
                    onOpenFavorites = {},
                    onOpenHistory = {},
                    uiState = DashboardUiState(
                        snapshot = sampleSnapshot(),
                        refreshFailed = true,
                        refreshError = DataError.Network(IOException("offline")),
                    ),
                )
            }
        }

        composeRule.onNodeWithText(
            "Couldn’t connect to the air-quality service. Check your connection and try again.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("42").assertIsDisplayed()
    }

    /** Each typed fetch error selects user-facing service guidance instead of a fallback message. */
    @Test
    fun showsSpecificMessageForEachRemainingDashboardFailureType() {
        val failures = listOf(
            DataError.Server(429, IOException("rate limited")) to
                "The air-quality service is receiving too many requests. Try again later.",
            DataError.Server(400, IOException("bad request")) to
                "The air-quality service couldn’t process this request. Please try again.",
            DataError.Configuration(IllegalStateException("configuration")) to
                "The air-quality service is not configured correctly. Please try again later.",
            DataError.RateLimited(IllegalStateException("rate limited")) to
                "The air-quality service is receiving too many requests. Try again later.",
            DataError.ServiceUnavailable(IllegalStateException("unavailable")) to
                "The air-quality service is temporarily unavailable. Please try again later.",
            DataError.UnexpectedResponse(IllegalStateException("unexpected")) to
                "The air-quality service returned an unexpected response. Please try again later.",
            DataError.GenericError(IllegalStateException("unexpected")) to
                "The air-quality service returned an unexpected response. Please try again later.",
            DataError.OfflineNoCache(IllegalStateException("offline")) to
                "You are offline and no saved measurement is available.",
            DataError.Local(IllegalStateException("local")) to
                "Couldn’t access saved air-quality data on this device. Try again.",
        )
        var uiState by mutableStateOf(
            DashboardUiState(
                error = DashboardError.DataUnavailable,
                dataError = failures.first().first,
            ),
        )

        composeRule.setContent {
            AirQualityMonitorTheme {
                DashboardScreen(
                    onOpenSearch = {},
                    onOpenFavorites = {},
                    onOpenHistory = {},
                    uiState = uiState,
                )
            }
        }

        failures.forEach { (failure, expectedMessage) ->
            composeRule.runOnIdle {
                uiState = DashboardUiState(
                    error = if (failure is DataError.OfflineNoCache) {
                        DashboardError.OfflineNoCache
                    } else {
                        DashboardError.DataUnavailable
                    },
                    dataError = failure,
                )
            }
            composeRule.onNodeWithText(expectedMessage).assertIsDisplayed()
        }
    }

    @Test
    fun dashboardRendersExpectedKeyContentNodesOnAPIResponseSuccessful() {
        var onOpenSearchClicked = false
        var onOpenHistoryClicked = false
        var onOpenFavoritesClicked = false
        composeRule.setContent {
            val onOpenSearch = { onOpenSearchClicked = true }
            val onOpenFavorites = { onOpenFavoritesClicked = true }
            val onOpenHistory = { onOpenHistoryClicked = true }

            AirQualityMonitorTheme {
                DashboardScreen(
                    onOpenSearch = onOpenSearch,
                    onOpenFavorites = onOpenFavorites,
                    onOpenHistory = onOpenHistory,
                    uiState = DashboardUiState(
                        snapshot = sampleSnapshot(),
                        locationAddress = "Cairo, Egypt",
                    ),
                )
            }
        }
        composeRule.onNodeWithTag(SEARCH_TOP_BAR_ICON_BUTTON).assertExists()
            .assertIsDisplayed().performClick()
        assertTrue(onOpenSearchClicked)
        onOpenSearchClicked = false // reset flag
        composeRule.onNodeWithTag(FAVORITES_TOP_BAR_ICON_BUTTON).assertExists()
            .assertIsDisplayed().performClick()
        assertTrue(onOpenFavoritesClicked)
        composeRule.onNodeWithText("Air quality").assertExists().assertIsDisplayed()
        composeRule.onNodeWithText("Current location").assertExists().assertIsDisplayed()
        composeRule.onNodeWithText("Cairo, Egypt").assertExists().assertIsDisplayed()
        composeRule.onNodeWithText("AQI").assertExists().assertIsDisplayed()
        composeRule.onNodeWithText("42").assertExists().assertIsDisplayed()
        composeRule.onNodeWithText("Pollutants").assertExists().assertIsDisplayed()
        composeRule.onNodeWithText("PM2.5").assertExists().assertIsDisplayed()
        composeRule.onNodeWithText("12.5").assertExists().assertIsDisplayed()
        composeRule.onNodeWithText("PM10").assertExists().assertIsDisplayed()
        composeRule.onNodeWithText("20.0").assertExists().assertIsDisplayed()
        composeRule.onNodeWithText("NO2").assertExists().assertIsDisplayed()
        composeRule.onNodeWithText("18.0").assertExists().assertIsDisplayed()
        composeRule.onNodeWithText("O3").assertExists().assertIsDisplayed()
        composeRule.onNodeWithText("46.0").assertExists().assertIsDisplayed()
        composeRule.onNodeWithText("CO").assertExists().assertIsDisplayed()
        composeRule.onNodeWithText("300.0").assertExists().assertIsDisplayed()
        composeRule.onNodeWithText("Last 24 hours").assertExists().assertIsDisplayed()
        composeRule.onNodeWithText("Historical measurements will appear here.").assertExists()
            .assertIsDisplayed()
        composeRule.onNodeWithText("View history", useUnmergedTree = true).assertExists().assertIsDisplayed().apply {
            onParent().performClick()
        }
        composeRule.onNodeWithTag(SEARCH_CHIP).assertExists().assertIsDisplayed().performClick()
        assertTrue(onOpenHistoryClicked)
        assertTrue(onOpenSearchClicked)
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
}
