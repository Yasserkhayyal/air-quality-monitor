package com.yasserkhayal.androidprojects.airqualitymonitor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import com.yasserkhayal.androidprojects.airqualitymonitor.feature.dashboard.DashboardRoute
import com.yasserkhayal.androidprojects.airqualitymonitor.feature.dashboard.HistoricalAirQualityScreen
import com.yasserkhayal.androidprojects.airqualitymonitor.feature.favorites.FavoritesScreen
import com.yasserkhayal.androidprojects.airqualitymonitor.feature.search.LocationSearchScreen
import com.yasserkhayal.androidprojects.airqualitymonitor.ui.theme.AirQualityMonitorTheme
import dagger.hilt.android.AndroidEntryPoint

private const val LOCATION_REQUEST_TIMEOUT_MS = 10_000L
private const val LOCATION_UPDATE_INTERVAL_MS = 1_000L
private const val LOCATION_UPDATE_DISTANCE_METERS = 10f

@AndroidEntryPoint
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
    History,
}

@Composable
private fun AirQualityApp() {
    var destination by rememberSaveable { mutableStateOf(Destination.Dashboard) }
    var permissionCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        permissionCallback?.invoke(
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true,
        )
        permissionCallback = null
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        when (destination) {
            Destination.Dashboard -> DashboardRoute(
                onRequestLocationPermission = { callback ->
                    val hasPermission =
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                            ) == PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        callback(true)
                    } else {
                        permissionCallback = callback
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                    }
                },
                onRequestCurrentLocation = { callback ->
                    requestCurrentLocation(context, callback)
                },
                onOpenSearch = { destination = Destination.Search },
                onOpenFavorites = { destination = Destination.Favorites },
                onOpenHistory = { destination = Destination.History },
            )

            Destination.Search -> LocationSearchScreen(
                onBack = { destination = Destination.Dashboard },
                onLocationSelected = { destination = Destination.Dashboard },
            )

            Destination.Favorites -> FavoritesScreen(
                onBack = { destination = Destination.Dashboard },
                onLocationSelected = { destination = Destination.Dashboard },
                onSearch = { destination = Destination.Search },
            )

            Destination.History -> HistoricalAirQualityScreen(
                onBack = { destination = Destination.Dashboard },
            )
        }
    }
}

private fun requestCurrentLocation(
    context: Context,
    callback: (Location?) -> Unit,
) {
    val hasLocationPermission =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
    if (!hasLocationPermission) {
        callback(null)
        return
    }

    val locationManager = context.getSystemService(LocationManager::class.java)
    val provider = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
        .firstOrNull(locationManager::isProviderEnabled)
    if (provider == null) {
        callback(null)
        return
    }

    val lastKnown = locationManager.getLastKnownLocation(provider)
    if (lastKnown != null) {
        getLocationAddress(context, lastKnown, callback)
        return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val cancellationSignal = CancellationSignal()
        val timeoutHandler = Handler(Looper.getMainLooper())
        val timeout = Runnable {
            cancellationSignal.cancel()
            callback(null)
        }
        timeoutHandler.postDelayed(timeout, LOCATION_REQUEST_TIMEOUT_MS)
        locationManager.getCurrentLocation(
            provider,
            cancellationSignal,
            ContextCompat.getMainExecutor(context),
        ) { location ->
            timeoutHandler.removeCallbacks(timeout)
            location?.let {
                getLocationAddress(context, location, callback)
            } ?: callback(null)
        }
        return
    }

    val timeoutHandler = Handler(Looper.getMainLooper())
    lateinit var timeout: Runnable
    val listener = object : LocationListener {
        override fun onLocationChanged(location: android.location.Location) {
            timeoutHandler.removeCallbacks(timeout)
            locationManager.removeUpdates(this)
            getLocationAddress(context, location, callback)
        }
    }
    timeout = Runnable {
        locationManager.removeUpdates(listener)
        callback(null)
    }
    locationManager.requestLocationUpdates(
        provider,
        LOCATION_UPDATE_INTERVAL_MS,
        LOCATION_UPDATE_DISTANCE_METERS,
        listener,
        Looper.getMainLooper(),
    )
    timeoutHandler.postDelayed(timeout, LOCATION_REQUEST_TIMEOUT_MS)
}

private fun getLocationAddress(
    context: Context,
    location: android.location.Location,
    callback: (Location?) -> Unit,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Geocoder(context).getFromLocation(location.latitude, location.longitude, 1) { addresses ->
            processAddresses(location, addresses, callback)
        }
    } else {
        @Suppress("DEPRECATION")
        Geocoder(context).getFromLocation(
            location.latitude,
            location.longitude,
            1,
        ).let { addresses ->
            processAddresses(location, addresses, callback)
        }
    }
}

private fun processAddresses(
    location: android.location.Location,
    addresses: List<Address>?,
    callback: (Location?) -> Unit,
) {
    val address = addresses?.firstOrNull()?.let { address ->
        val city = address.locality ?: address.adminArea
        val country = address.countryName
        "$city, $country"
    }
    callback(Location(location.latitude, location.longitude, address))
}
