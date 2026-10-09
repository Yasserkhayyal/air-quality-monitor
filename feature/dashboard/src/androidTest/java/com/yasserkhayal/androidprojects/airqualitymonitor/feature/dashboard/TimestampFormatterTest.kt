package com.yasserkhayal.androidprojects.airqualitymonitor.feature.dashboard

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimestampFormatterTest {

    @Test
    fun preservesBackwardCompatibleTimestampWithSpaceSeparator() {
        Assert.assertEquals(
            "Friday, September 25, 2026 at 4:00 PM",
            replaceUnicodeSpacesWithNormalSpace(
                formatTimestampForDisplay("2026-09-25 16:00"),
            ),
        )
    }

    @Test
    fun formatsTimestampWithSecondsFromEitherRepresentation() {
        Assert.assertEquals(
            "Friday, September 25, 2026 at 4:00 PM",
            replaceUnicodeSpacesWithNormalSpace(
                formatTimestampForDisplay("2026-09-25 16:00:30"),
            ),
        )
    }

    @Test
    fun returnsOriginalValueWhenTimestampIsMalformed() {
        val timestamp = null
        Assert.assertEquals(
            timestamp,
            replaceUnicodeSpacesWithNormalSpace(
                formatTimestampForDisplay("timestamp"),
            ),
        )
    }

    private fun replaceUnicodeSpacesWithNormalSpace(actualDateTimeString: String?) = actualDateTimeString?.replace(Regex("\\s+"), " ")
}
