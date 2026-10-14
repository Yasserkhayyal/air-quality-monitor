package com.yasserkhayal.androidprojects.airqualitymonitor.feature.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eygraber.compose.placeholder.PlaceholderDefaults
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.color
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.shimmer
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AqiCategory

/**
 * Renders the prominent AQI, category, timestamp, and optional stale-data notice.
 */
@Composable
internal fun AqiSummaryCard(
    modifier: Modifier = Modifier,
    europeanAqi: Int?,
    category: AqiCategory?,
    timestamp: String,
    isLoading: Boolean = false,
    isStale: Boolean = false,
) {
    val containerColor = MaterialTheme.colorScheme.primaryContainer
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.dashboard_aqi), style = MaterialTheme.typography.labelLarge)
            Text(
                europeanAqi?.toString() ?: stringResource(R.string.dashboard_measurement_not_available),
                modifier = Modifier.placeholder(
                    visible = isLoading,
                    color = PlaceholderDefaults.color(backgroundColor = containerColor),
                    highlight = PlaceholderHighlight.shimmer(
                        highlightColor = containerColor.copy(alpha = 0.35f),
                    ),
                ),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                category?.let { stringResource(it.labelRes) }
                    ?: stringResource(R.string.dashboard_data_unavailable),
                modifier = Modifier.placeholder(
                    visible = isLoading,
                    color = PlaceholderDefaults.color(backgroundColor = containerColor),
                    highlight = PlaceholderHighlight.shimmer(
                        highlightColor = containerColor.copy(alpha = 0.35f),
                    ),
                ),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                timestamp,
                modifier = Modifier.placeholder(
                    visible = isLoading,
                    color = PlaceholderDefaults.color(backgroundColor = containerColor),
                    highlight = PlaceholderHighlight.shimmer(
                        highlightColor = containerColor.copy(alpha = 0.35f),
                    ),
                ),
                style = MaterialTheme.typography.labelMedium,
            )
            if (isStale) {
                Text(
                    stringResource(R.string.dashboard_data_may_be_outdated),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

/**
 * Resolves the localized label for an AQI category.
 */
internal val AqiCategory.labelRes: Int
    get() = when (this) {
        AqiCategory.GOOD -> R.string.aqi_category_good
        AqiCategory.FAIR -> R.string.aqi_category_fair
        AqiCategory.MODERATE -> R.string.aqi_category_moderate
        AqiCategory.POOR -> R.string.aqi_category_poor
        AqiCategory.VERY_POOR -> R.string.aqi_category_very_poor
    }
