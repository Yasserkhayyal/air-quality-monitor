package com.yasserkhayal.androidprojects.airqualitymonitor.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yasserkhayal.androidprojects.airqualitymonitor.core.designsystem.theme.AirQualityMonitorTheme
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AqiCategory
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataError
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataFreshness
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.HistoricalAirQualityPoint
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale

const val HISTORICAL_POINT_PREFIX = "historical_point_"

/**
 * Connects history state to app-owned back navigation.
 */
@Composable
fun HistoricalAirQualityRoute(
    onBack: () -> Unit,
    viewModel: HistoricalAirQualityViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.onIntent(HistoricalAirQualityIntent.Started)
    }
    HistoricalAirQualityScreen(
        uiState = uiState,
        onBack = onBack,
        onIntent = viewModel::onIntent,
    )
}

/**
 * Renders hourly historical measurements, cache status, and retry states.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HistoricalAirQualityScreen(
    uiState: HistoricalAirQualityUiState,
    onBack: () -> Unit,
    onIntent: (HistoricalAirQualityIntent) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            uiState.isLoading && uiState.points.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }

            uiState.points.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    stringResource(R.string.history_unavailable_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    when {
                        uiState.locationUnavailable -> stringResource(R.string.history_location_unavailable)
                        uiState.error != null -> historicalErrorMessage(uiState.error)
                        else -> stringResource(R.string.history_unavailable_message)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (uiState.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                if (!uiState.locationUnavailable) {
                    Button(
                        onClick = { onIntent(HistoricalAirQualityIntent.Retry) },
                        modifier = Modifier.testTag(HISTORICAL_RETRY_BUTTON),
                    ) {
                        Text(stringResource(R.string.history_retry))
                    }
                }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        uiState.location?.address ?: stringResource(R.string.history_location_selected),
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
                    }
                    if (uiState.refreshError != null) {
                        Text(
                            stringResource(R.string.history_cached_refresh_failed),
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (uiState.freshness == DataFreshness.Stale) {
                        Text(
                            stringResource(R.string.history_data_may_be_outdated),
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (uiState.error != null) {
                        Text(
                            historicalErrorMessage(uiState.error),
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                items(
                    items = uiState.points,
                    key = HistoricalAirQualityPoint::timestamp,
                ) { point ->
                    HistoricalMeasurement(
                        point = point,
                    )
                }
                if (uiState.points.size < 24) {
                    item {
                        Text(
                            stringResource(R.string.history_partial_data),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

/** Test tag used to identify the historical retry action. */
const val HISTORICAL_RETRY_BUTTON = "historical_retry_button"

/**
 * Renders one compact hourly measurement with AQI and available pollutant readings.
 */
@Composable
private fun HistoricalMeasurement(
    point: HistoricalAirQualityPoint,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(HISTORICAL_POINT_PREFIX + point.timestamp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    formatHistoricalTimestamp(point.timestamp) ?: point.timestamp,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.history_aqi),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        " ${point.europeanAqi?.toString() ?: stringResource(R.string.history_unavailable_value)}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    point.europeanAqi?.let { aqi ->
                        Text(
                            " · ${stringResource(aqiCategoryLabel(AqiCategory.from(aqi)))}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                HistoricalPollutantValue("PM2.5", point.pm2_5)
                HistoricalPollutantValue("PM10", point.pm10)
                HistoricalPollutantValue("NO2", point.nitrogenDioxide)
                HistoricalPollutantValue("O3", point.ozone)
                HistoricalPollutantValue("CO", point.carbonMonoxide)
            }
        }
    }
}

/**
 * Renders a pollutant and its value with compact text styling.
 */
@Composable
private fun HistoricalPollutantValue(label: String, value: Double?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "$label ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value?.toString() ?: stringResource(R.string.history_unavailable_value),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/**
 * Formats a UTC observation timestamp as a compact local date and time.
 */
internal fun formatHistoricalTimestamp(timestamp: String): String? {
    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
    val parsePosition = ParsePosition(0)
    val parsedDate = parser.parse(timestamp, parsePosition)
    if (parsedDate == null || parsePosition.index != timestamp.length) return null
    return java.text.DateFormat.getDateTimeInstance(
        java.text.DateFormat.SHORT,
        java.text.DateFormat.SHORT,
        Locale.getDefault(),
    ).format(parsedDate)
}

/**
 * Maps an AQI category to the localized label owned by this feature.
 */
private fun aqiCategoryLabel(category: AqiCategory): Int = when (category) {
    AqiCategory.GOOD -> R.string.history_aqi_good
    AqiCategory.FAIR -> R.string.history_aqi_fair
    AqiCategory.MODERATE -> R.string.history_aqi_moderate
    AqiCategory.POOR -> R.string.history_aqi_poor
    AqiCategory.VERY_POOR -> R.string.history_aqi_very_poor
}

/**
 * Resolves user-facing copy for historical repository errors.
 */
@Composable
private fun historicalErrorMessage(error: DataError): String = when (error) {
    is DataError.Network -> stringResource(R.string.history_error_network)

    is DataError.Server -> when {
        error.code == 429 -> stringResource(R.string.history_error_rate_limited)
        error.code in 500..599 -> stringResource(R.string.history_error_server)
        else -> stringResource(R.string.history_error_request_rejected)
    }

    is DataError.Configuration -> stringResource(R.string.history_error_configuration)

    is DataError.RateLimited -> stringResource(R.string.history_error_rate_limited)

    is DataError.ServiceUnavailable -> stringResource(R.string.history_error_server)

    is DataError.UnexpectedResponse -> stringResource(R.string.history_error_unexpected)

    is DataError.GenericError -> stringResource(R.string.history_error_unexpected)

    is DataError.OfflineNoCache -> stringResource(R.string.history_error_offline)

    is DataError.Local -> stringResource(R.string.history_error_local)
}

/** Previews a populated historical list with partial metric availability. */
@PreviewLightDark
@Composable
private fun HistoricalAirQualityScreenPreview() {
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
                        pm10 = 20.0,
                        carbonMonoxide = 300.0,
                        nitrogenDioxide = 18.0,
                        ozone = 46.0,
                    ),
                    HistoricalAirQualityPoint(
                        timestamp = "2026-10-13T22:00",
                        europeanAqi = 35,
                        pm2_5 = 10.5,
                        pm10 = null,
                        carbonMonoxide = null,
                        nitrogenDioxide = 15.0,
                        ozone = 40.0,
                    ),
                ),
                freshness = DataFreshness.Fresh,
            ),
            onBack = {},
            onIntent = {},
        )
    }
}
