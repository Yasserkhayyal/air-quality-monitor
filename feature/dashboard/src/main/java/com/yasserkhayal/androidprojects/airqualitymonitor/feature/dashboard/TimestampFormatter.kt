package com.yasserkhayal.androidprojects.airqualitymonitor.feature.dashboard

import android.icu.text.DateFormat
import android.icu.text.SimpleDateFormat
import android.util.Log
import java.util.Locale

private const val DATE_FORMAT = "yyyy-MM-dd HH:mm"
private const val TAG = "TimestampFormatter.kt"

internal fun formatTimestampForDisplay(timestamp: String): String? = runCatching {
    val formattedDate = SimpleDateFormat(DATE_FORMAT, Locale.US).parse(timestamp)
    return DateFormat.getDateTimeInstance(
        DateFormat.FULL,
        DateFormat.SHORT,
        Locale.US,
    ).format(formattedDate)
}.getOrElse { throwable ->
    Log.e(TAG, "Error formatting timestamp: $timestamp", throwable)
    null
}
