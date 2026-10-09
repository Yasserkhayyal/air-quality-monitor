package com.yasserkhayal.androidprojects.airqualitymonitor.feature.dashboard

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
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
