package com.yasserkhayal.androidprojects.airqualitymonitor.feature.favorites

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yasserkhayal.androidprojects.airqualitymonitor.core.designsystem.theme.AirQualityMonitorTheme
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoritesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    var searchOpened = false

    @After
    fun tearDown() {
        searchOpened = false
    }

    /** Shows the empty state and invokes search action. */
    @Test
    fun showsEmptyStateAndOpensSearch() {
        composeRule.setContent {
            AirQualityMonitorTheme {
                FavoritesScreen(
                    uiState = FavoritesUiState(isLoading = false),
                    onBack = {},
                    onSearch = { searchOpened = true },
                    onIntent = {},
                )
            }
        }

        composeRule.onNodeWithText("No favorite locations yet.").assertIsDisplayed()
        composeRule.onNodeWithText("Search for a location").performClick()

        assertEquals(true, searchOpened)
    }

    /** Shows the favorites load failed error and retry state. */
    @Test
    fun showsFavoritesLoadFailedErrorAndRetryState() {
        val intents = mutableListOf<FavoritesIntent>()
        composeRule.setContent {
            AirQualityMonitorTheme {
                FavoritesScreen(
                    uiState = FavoritesUiState(isLoading = false, error = FavoritesError.LoadFailed),
                    onBack = {},
                    onSearch = { searchOpened = true },
                    onIntent = intents::add,
                )
            }
        }

        composeRule.onNodeWithText("Couldn’t load your favorites. Try again later.").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()
        composeRule.onNodeWithText("Search for a location").performClick()

        assertEquals(
            listOf(
                FavoritesIntent.Retry,
            ),
            intents,
        )
        assertEquals(true, searchOpened)
    }

    /** Selecting and removing a row emit their corresponding feature intents. */
    @Test
    fun selectingAndRemovingFavoriteEmitIntents() {
        val location = Location(30.0444, 31.2357, "Cairo, Egypt")
        val intents = mutableListOf<FavoritesIntent>()
        composeRule.setContent {
            AirQualityMonitorTheme {
                FavoritesScreen(
                    uiState = FavoritesUiState(isLoading = false, favorites = listOf(location)),
                    onBack = {},
                    onSearch = { searchOpened = true },
                    onIntent = intents::add,
                )
            }
        }

        composeRule.onNodeWithTag(FAVORITE_ITEM_PREFIX + "30.0444:31.2357").performClick()
        composeRule.onNodeWithTag(FAVORITE_REMOVE_PREFIX + "30.0444:31.2357").performClick()
        composeRule.onNodeWithText("Search for a location").performClick()

        assertEquals(
            listOf(
                FavoritesIntent.LocationSelected(location),
                FavoritesIntent.RemoveFavorite(location),
            ),
            intents,
        )
        assertEquals(true, searchOpened)
    }
}
