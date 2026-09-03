package com.satvik.mibandnavigator

import org.junit.Test

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun directionsUseMatchingNotificationIcons() {
        assertEquals(R.drawable.ic_nav_left, navigationIcon(NavDirection.LEFT))
        assertEquals(R.drawable.ic_nav_right, navigationIcon(NavDirection.RIGHT))
        assertEquals(R.drawable.ic_nav_straight, navigationIcon(NavDirection.STRAIGHT))
        assertEquals(R.drawable.ic_nav_uturn, navigationIcon(NavDirection.UTURN))
        assertEquals(R.drawable.ic_nav_slight_left, navigationIcon(NavDirection.SLIGHT_LEFT))
        assertEquals(R.drawable.ic_nav_slight_right, navigationIcon(NavDirection.SLIGHT_RIGHT))
        assertEquals(R.drawable.ic_nav_roundabout, navigationIcon(NavDirection.ROUNDABOUT))
        assertEquals(R.drawable.ic_nav_straight, navigationIcon(NavDirection.UNKNOWN))
    }

    @Test
    fun directionsUseAsciiInstructions() {
        val directions = mapOf(
            NavDirection.LEFT to "<-- LEFT",
            NavDirection.RIGHT to "RIGHT -->",
            NavDirection.STRAIGHT to "^ STRAIGHT",
            NavDirection.UTURN to "U-TURN",
            NavDirection.SLIGHT_LEFT to "/< SLIGHT LEFT",
            NavDirection.SLIGHT_RIGHT to ">\\ SLIGHT RIGHT",
            NavDirection.ROUNDABOUT to "(O) ROUNDABOUT",
            NavDirection.UNKNOWN to "CONTINUE"
        )

        listOf(false, true).forEach { isCompact ->
            directions.forEach { (direction, instruction) ->
                val text = formatBandText(
                    NavData("150 m", "Test Road", direction, "10 min", "4.5 km"),
                    isCompact
                )

                val expected = if (isCompact) {
                    "150 m\n$instruction\n10 min • 4.5 km"
                } else {
                    "150 m  •  10 min\n$instruction\nTest Road    4.5 km"
                }
                assertEquals(expected, text)
                assertFalse(text.contains("• •"))
            }
        }
    }
}
