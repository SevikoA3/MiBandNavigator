package com.satvik.mibandnavigator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    private val parser = NavigationParser()

    @Test
    fun directionsUseMatchingNotificationIcons() {
        assertEquals(R.drawable.ic_nav_left, navigationIcon(NavDirection.LEFT))
        assertEquals(R.drawable.ic_nav_right, navigationIcon(NavDirection.RIGHT))
        assertEquals(R.drawable.ic_nav_straight, navigationIcon(NavDirection.STRAIGHT))
        assertEquals(R.drawable.ic_nav_uturn, navigationIcon(NavDirection.UTURN))
        assertEquals(R.drawable.ic_nav_slight_left, navigationIcon(NavDirection.SLIGHT_LEFT))
        assertEquals(R.drawable.ic_nav_slight_right, navigationIcon(NavDirection.SLIGHT_RIGHT))
        assertEquals(R.drawable.ic_nav_left, navigationIcon(NavDirection.SHARP_LEFT))
        assertEquals(R.drawable.ic_nav_right, navigationIcon(NavDirection.SHARP_RIGHT))
        assertEquals(R.drawable.ic_nav_roundabout, navigationIcon(NavDirection.ROUNDABOUT))
        assertEquals("O ROUNDABOUT", directionLabel(NavDirection.ROUNDABOUT))
        assertEquals(R.drawable.ic_nav_left, navigationIcon(NavDirection.KEEP_LEFT))
        assertEquals(R.drawable.ic_nav_right, navigationIcon(NavDirection.EXIT_RIGHT))
        assertEquals(R.drawable.ic_nav_straight, navigationIcon(NavDirection.DESTINATION))
    }

    @Test
    fun parsesDistanceRoadAndTripSummary() {
        val data = parser.parseMapsData(
            title = "150 m",
            text = "Turn left onto Jalan Sudirman",
            subText = "10 min • 4.5 km • 18:30",
            textLines = null
        )

        assertEquals("150 m", data.distance)
        assertEquals("Jalan Sudirman", data.roadName)
        assertEquals(NavDirection.LEFT, data.direction)
        assertEquals("10 min", data.eta)
        assertEquals("4.5 km", data.totalDistance)
        assertEquals("18:30", data.arrivalTime)
    }

    @Test
    fun parsesExpandedNotificationText() {
        val data = parser.parseMapsData(
            title = "",
            text = "",
            subText = "8 min • 3 km",
            textLines = null,
            bigText = "Turn right onto Jalan Rasuna Said",
            summaryText = "Arrive 18:40"
        )

        assertEquals(NavDirection.RIGHT, data.direction)
        assertEquals("Jalan Rasuna Said", data.roadName)
        assertEquals("8 min", data.eta)
        assertEquals("3 km", data.totalDistance)
        assertEquals("18:40", data.arrivalTime)
    }

    @Test
    fun parsesRoundaboutExitInEnglishAndIndonesian() {
        val english = parser.parseMapsData(
            title = "250 m",
            text = "At the roundabout, take the 2nd exit onto Sudirman Road",
            subText = "10 min • 4.5 km",
            textLines = null
        )
        val indonesian = parser.parseMapsData(
            title = "300 m",
            text = "Di bundaran, ambil pintu keluar ketiga menuju Jalan Merdeka",
            subText = "12 menit • 5 km",
            textLines = null
        )

        assertEquals(NavDirection.ROUNDABOUT, english.direction)
        assertEquals(2, english.roundaboutExit)
        assertEquals("Sudirman Road", english.roadName)
        assertEquals(NavDirection.ROUNDABOUT, indonesian.direction)
        assertEquals(3, indonesian.roundaboutExit)
        assertEquals("Jalan Merdeka", indonesian.roadName)
        assertEquals(
            21,
            parser.parseMapsData("", "At the roundabout, take the 21st exit", "", null).roundaboutExit
        )
    }

    @Test
    fun parsesKeepMergeExitAndArrival() {
        val keep = parser.parseMapsData("500 m", "Keep left to stay on Route 1", "20 min • 18 km", null)
        val merge = parser.parseMapsData("1 km", "Merge onto Jalan Tol Dalam Kota", "30 min • 25 km", null)
        val exit = parser.parseMapsData("800 m", "Take exit 23A toward Airport", "15 min • 12 km", null)
        val arrived = parser.parseMapsData("", "You have arrived at your destination", "", null)

        assertEquals(NavDirection.KEEP_LEFT, keep.direction)
        assertEquals("Route 1", keep.roadName)
        assertEquals(NavDirection.MERGE, merge.direction)
        assertEquals("Jalan Tol Dalam Kota", merge.roadName)
        assertEquals(NavDirection.EXIT, exit.direction)
        assertEquals("23A", exit.exitNumber)
        assertEquals(NavDirection.DESTINATION, arrived.direction)
    }

    @Test
    fun formatsReadableBandInstructions() {
        val full = formatBandText(
            NavData("150 m", "Jalan Sudirman", NavDirection.LEFT, "10 min", "4.5 km"),
            isCompact = false
        )
        val compact = formatBandText(
            NavData("250 m", "Sudirman Road", NavDirection.ROUNDABOUT, "10 min", "4.5 km", roundaboutExit = 2),
            isCompact = true
        )

        assertEquals("150 m\n← TURN LEFT\nJalan Sudirman\n10 min | 4.5 km", full)
        assertEquals("O EXIT 2 250 m\nSudirman Road\n10 min | 4.5 km", compact)
        assertFalse(full.contains("•"))
    }

    @Test
    fun parsesGoogleMapsManeuverVariants() {
        assertEquals(NavDirection.SLIGHT_LEFT, parseDirection("Turn slightly left onto Jalan A"))
        assertEquals(NavDirection.SLIGHT_RIGHT, parseDirection("Turn slight right onto Jalan B"))
        assertEquals(NavDirection.SHARP_LEFT, parseDirection("Turn sharply left onto Jalan C"))
        assertEquals(NavDirection.SHARP_RIGHT, parseDirection("Belok tajam ke kanan menuju Jalan D"))
        assertEquals(NavDirection.UTURN, parseDirection("Make a U-turn"))
        assertEquals(NavDirection.STRAIGHT, parseDirection("Head north on Jalan E"))
        assertEquals(NavDirection.STRAIGHT, parseDirection("Continue onto Jalan F"))
    }

    @Test
    fun parsesAllDocumentedDirectionsApiManeuvers() {
        val maneuvers = mapOf(
            "turn-slight-left" to NavDirection.SLIGHT_LEFT,
            "turn-sharp-left" to NavDirection.SHARP_LEFT,
            "turn-left" to NavDirection.LEFT,
            "turn-slight-right" to NavDirection.SLIGHT_RIGHT,
            "turn-sharp-right" to NavDirection.SHARP_RIGHT,
            "keep-right" to NavDirection.KEEP_RIGHT,
            "keep-left" to NavDirection.KEEP_LEFT,
            "uturn-left" to NavDirection.UTURN,
            "uturn-right" to NavDirection.UTURN,
            "turn-right" to NavDirection.RIGHT,
            "straight" to NavDirection.STRAIGHT,
            "ramp-left" to NavDirection.LEFT,
            "ramp-right" to NavDirection.RIGHT,
            "merge" to NavDirection.MERGE,
            "fork-left" to NavDirection.KEEP_LEFT,
            "fork-right" to NavDirection.KEEP_RIGHT,
            "ferry" to NavDirection.FERRY,
            "ferry-train" to NavDirection.FERRY,
            "roundabout-left" to NavDirection.ROUNDABOUT,
            "roundabout-right" to NavDirection.ROUNDABOUT
        )

        maneuvers.forEach { (maneuver, direction) ->
            assertEquals(maneuver, direction, parseDirection(maneuver))
        }
    }

    @Test
    fun parsesAllDocumentedNavigationSdkManeuvers() {
        assertManeuvers(NavDirection.STRAIGHT, "depart", "name-change", "straight")
        assertManeuvers(NavDirection.DESTINATION, "destination", "destination-left", "destination-right")
        assertManeuvers(NavDirection.FERRY, "ferry-boat", "ferry-train")
        assertManeuvers(NavDirection.KEEP_LEFT, "fork-left", "turn-keep-left", "on-ramp-keep-left", "off-ramp-keep-left")
        assertManeuvers(NavDirection.KEEP_RIGHT, "fork-right", "turn-keep-right", "on-ramp-keep-right", "off-ramp-keep-right")
        assertManeuvers(NavDirection.MERGE, "merge-left", "merge-right", "merge-unspecified", "on-ramp-unspecified")
        assertManeuvers(NavDirection.LEFT, "turn-left", "on-ramp-left", "off-ramp-left")
        assertManeuvers(NavDirection.RIGHT, "turn-right", "on-ramp-right", "off-ramp-right")
        assertManeuvers(NavDirection.SLIGHT_LEFT, "turn-slight-left", "on-ramp-slight-left", "off-ramp-slight-left")
        assertManeuvers(NavDirection.SLIGHT_RIGHT, "turn-slight-right", "on-ramp-slight-right", "off-ramp-slight-right")
        assertManeuvers(NavDirection.SHARP_LEFT, "turn-sharp-left", "on-ramp-sharp-left", "off-ramp-sharp-left")
        assertManeuvers(NavDirection.SHARP_RIGHT, "turn-sharp-right", "on-ramp-sharp-right", "off-ramp-sharp-right")
        assertManeuvers(
            NavDirection.UTURN,
            "turn-u-turn-clockwise", "turn-u-turn-counterclockwise",
            "on-ramp-u-turn-clockwise", "on-ramp-u-turn-counterclockwise",
            "off-ramp-u-turn-clockwise", "off-ramp-u-turn-counterclockwise"
        )
        assertManeuvers(NavDirection.EXIT, "off-ramp-unspecified")
        assertManeuvers(
            NavDirection.ROUNDABOUT,
            "roundabout-clockwise", "roundabout-counterclockwise",
            "roundabout-exit-clockwise", "roundabout-exit-counterclockwise",
            "roundabout-left-clockwise", "roundabout-left-counterclockwise",
            "roundabout-right-clockwise", "roundabout-right-counterclockwise",
            "roundabout-sharp-left-clockwise", "roundabout-sharp-left-counterclockwise",
            "roundabout-sharp-right-clockwise", "roundabout-sharp-right-counterclockwise",
            "roundabout-slight-left-clockwise", "roundabout-slight-left-counterclockwise",
            "roundabout-slight-right-clockwise", "roundabout-slight-right-counterclockwise",
            "roundabout-straight-clockwise", "roundabout-straight-counterclockwise",
            "roundabout-u-turn-clockwise", "roundabout-u-turn-counterclockwise"
        )
        assertEquals(NavDirection.UNKNOWN, parseDirection("unknown"))
    }

    @Test
    fun parsesGoogleMapsHighwayAndFerryManeuvers() {
        assertEquals(NavDirection.EXIT_LEFT, parseDirection("Take the left exit toward Airport"))
        assertEquals(NavDirection.RIGHT, parseDirection("Use the right ramp toward Airport"))
        assertEquals(NavDirection.EXIT, parseDirection("Take the ramp toward Airport"))
        assertEquals(NavDirection.MERGE, parseDirection("On-ramp"))
        assertEquals(NavDirection.EXIT, parseDirection("Off-ramp"))
        assertEquals(NavDirection.KEEP_LEFT, parseDirection("Keep left at the fork"))
        assertEquals(NavDirection.KEEP_RIGHT, parseDirection("Keep right at the fork"))
        assertEquals(NavDirection.MERGE, parseDirection("Merge onto Route 1"))
        assertEquals(NavDirection.FERRY, parseDirection("Take the train ferry"))
        assertEquals(NavDirection.DESTINATION, parseDirection("Your destination is on the left"))
        assertEquals(NavDirection.KEEP_LEFT, parseDirection("Ambil jalan kiri"))
        assertEquals(NavDirection.STRAIGHT, parseDirection("Enter Route 1"))
    }

    private fun parseDirection(instruction: String): NavDirection =
        parser.parseMapsData("", instruction, "", null).direction

    private fun assertManeuvers(expected: NavDirection, vararg maneuvers: String) {
        maneuvers.forEach { maneuver ->
            assertEquals(maneuver, expected, parseDirection(maneuver))
        }
    }

    @Test
    fun truncatesLongBandLines() {
        val text = formatBandText(
            NavData("150 m", "Jalan Profesor Doktor Satrio", NavDirection.LEFT, "10 min", "4.5 km"),
            isCompact = false
        )

        assertTrue(text.lineSequence().all { it.length <= 24 })
    }
}
