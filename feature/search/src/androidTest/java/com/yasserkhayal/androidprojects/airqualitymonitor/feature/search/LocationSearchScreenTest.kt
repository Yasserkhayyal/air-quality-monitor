package com.yasserkhayal.androidprojects.airqualitymonitor.feature.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yasserkhayal.androidprojects.airqualitymonitor.core.designsystem.theme.AirQualityMonitorTheme
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataError
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class LocationSearchScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectingMatchEmitsLocationSelectedIntent() {
        val location = Location(30.0444, 31.2357, "Cairo, Egypt")
        val intents = mutableListOf<SearchIntent>()
        composeRule.setContent {
            AirQualityMonitorTheme {
                LocationSearchScreen(
                    uiState = SearchUiState(
                        query = "Cairo",
                        hasSearched = true,
                        results = listOf(location),
                    ),
                    onBack = {},
                    onIntent = intents::add,
                )
            }
        }

        composeRule.onNodeWithText("Cairo, Egypt").assertIsDisplayed().performClick()

        assertEquals(listOf(SearchIntent.LocationSelected(location)), intents)
    }

    @Test
    fun enteringTextEmitsQueryChangedIntent() {
        val intents = mutableListOf<SearchIntent>()
        composeRule.setContent {
            AirQualityMonitorTheme {
                LocationSearchScreen(
                    uiState = SearchUiState(),
                    onBack = {},
                    onIntent = intents::add,
                )
            }
        }

        composeRule.onNodeWithText("City or location").performTextInput("Cairo")

        assertEquals(SearchIntent.QueryChanged("Cairo"), intents.last())
    }

    @Test
    fun showsInstructionBeforeMinimumQueryLength() {
        composeRule.setContent {
            AirQualityMonitorTheme {
                LocationSearchScreen(
                    uiState = SearchUiState(),
                    onBack = {},
                    onIntent = {},
                )
            }
        }

        composeRule.onNodeWithText("Enter at least two characters to search for a city or place.")
            .assertIsDisplayed()
    }

    /** Connectivity failures show actionable network guidance and preserve retry. */
    @Test
    fun showsConnectivitySpecificSearchErrorAndRetry() {
        val intents = mutableListOf<SearchIntent>()
        composeRule.setContent {
            AirQualityMonitorTheme {
                LocationSearchScreen(
                    uiState = SearchUiState(
                        query = "Cairo",
                        hasSearched = true,
                        error = SearchError.SearchFailed(DataError.Network(IOException("offline"))),
                    ),
                    onBack = {},
                    onIntent = intents::add,
                )
            }
        }

        composeRule.onNodeWithText(
            "Couldn’t connect to location search. Check your connection and try again.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertIsDisplayed().performClick()
        assertEquals(listOf(SearchIntent.Retry), intents)
    }

    /** Server failures do not incorrectly tell the user to check connectivity. */
    @Test
    fun showsServerErrorWithoutBlamingConnectivity() {
        composeRule.setContent {
            AirQualityMonitorTheme {
                LocationSearchScreen(
                    uiState = SearchUiState(
                        query = "Cairo",
                        hasSearched = true,
                        error = SearchError.SearchFailed(
                            DataError.Server(503, IOException("service unavailable")),
                        ),
                    ),
                    onBack = {},
                    onIntent = {},
                )
            }
        }

        composeRule.onNodeWithText(
            "Location search is temporarily unavailable. Please try again later.",
        ).assertIsDisplayed()
    }

    /** Every remaining typed failure keeps its distinct localized guidance in Search. */
    @Test
    fun showsSpecificMessageForEachRemainingSearchFailureType() {
        val failures = listOf(
            DataError.Server(429, IOException("rate limited")) to
                "Location search is receiving too many requests. Wait a moment and try again.",
            DataError.Server(400, IOException("bad request")) to
                "Location search couldn’t process this request. Please try again.",
            DataError.Configuration(IllegalStateException("configuration")) to
                "Location search is not configured correctly. Please try again later.",
            DataError.RateLimited(IllegalStateException("rate limited")) to
                "Location search is receiving too many requests. Wait a moment and try again.",
            DataError.ServiceUnavailable(IllegalStateException("unavailable")) to
                "Location search is temporarily unavailable. Please try again later.",
            DataError.UnexpectedResponse(IllegalStateException("unexpected")) to
                "Location search returned an unexpected response. Please try again later.",
            DataError.GenericError(IllegalStateException("unexpected")) to
                "Location search returned an unexpected response. Please try again later.",
            DataError.OfflineNoCache(IllegalStateException("offline")) to
                "No network connection is available for location search.",
        )
        var uiState by mutableStateOf(
            SearchUiState(
                query = "Cairo",
                hasSearched = true,
                error = SearchError.SearchFailed(failures.first().first),
            ),
        )

        composeRule.setContent {
            AirQualityMonitorTheme {
                LocationSearchScreen(
                    uiState = uiState,
                    onBack = {},
                    onIntent = {},
                )
            }
        }

        failures.forEach { (failure, expectedMessage) ->
            composeRule.runOnIdle {
                uiState = SearchUiState(
                    query = "Cairo",
                    hasSearched = true,
                    error = SearchError.SearchFailed(failure),
                )
            }
            composeRule.onNodeWithText(expectedMessage).assertIsDisplayed()
        }

        val selectedLocation = Location(30.0444, 31.2357, "Cairo, Egypt")
        composeRule.runOnIdle {
            uiState = SearchUiState(
                query = "Cairo",
                hasSearched = true,
                results = listOf(selectedLocation),
                error = SearchError.SelectionFailed(
                    DataError.Local(IllegalStateException("database unavailable")),
                ),
            )
        }
        composeRule.onNodeWithText(
            "Couldn’t save this location on your device. Try selecting it again.",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Cairo, Egypt").assertIsDisplayed()
    }
}
