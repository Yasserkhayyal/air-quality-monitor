package com.yasserkhayal.androidprojects.airqualitymonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.yasserkhayal.androidprojects.airqualitymonitor.feature.dashboard.DashboardScreen
import com.yasserkhayal.androidprojects.airqualitymonitor.feature.dashboard.HistoricalAirQualityScreen
import com.yasserkhayal.androidprojects.airqualitymonitor.feature.favorites.FavoritesScreen
import com.yasserkhayal.androidprojects.airqualitymonitor.feature.search.LocationSearchScreen
import com.yasserkhayal.androidprojects.airqualitymonitor.ui.theme.AirQualityMonitorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AirQualityMonitorTheme {
                AirQualityApp()
            }
        }
    }
}

private enum class Destination {
    Dashboard,
    Search,
    Favorites,
    History
}

@Composable
private fun AirQualityApp() {
    var destination by rememberSaveable { androidx.compose.runtime.mutableStateOf(Destination.Dashboard) }

    Surface(modifier = Modifier.fillMaxSize()) {
        when (destination) {
            Destination.Dashboard -> DashboardScreen(
                onOpenSearch = { destination = Destination.Search },
                onOpenFavorites = { destination = Destination.Favorites },
                onOpenHistory = { destination = Destination.History }
            )

            Destination.Search -> LocationSearchScreen(
                onBack = { destination = Destination.Dashboard },
                onLocationSelected = { destination = Destination.Dashboard }
            )

            Destination.Favorites -> FavoritesScreen(
                onBack = { destination = Destination.Dashboard },
                onLocationSelected = { destination = Destination.Dashboard },
                onSearch = { destination = Destination.Search }
            )

            Destination.History -> HistoricalAirQualityScreen(
                onBack = { destination = Destination.Dashboard }
            )
        }
    }
}
