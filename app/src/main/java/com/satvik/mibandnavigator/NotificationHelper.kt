package com.satvik.mibandnavigator

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.app.NotificationCompat
import kotlin.math.roundToInt

class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "nav_channel"
    private val notificationId = 1001
    private val sharedPrefs = context.getSharedPreferences("NavSettings", Context.MODE_PRIVATE)

    init {
        createChannel()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            channelId,
            "Mi Band Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Sends turn-by-turn alerts to Mi Fitness" }
        notificationManager.createNotificationChannel(channel)
    }

    fun sendToBand(navData: NavData) {
        val text = formatBandText(navData, sharedPrefs.getBoolean("compact_mode", false))
        val icon = navigationIcon(navData.direction)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(icon)
            .setLargeIcon(createLargeIcon(context, icon))
            .setContentTitle("NAVIGATION")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setOnlyAlertOnce(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun clear() {
        notificationManager.cancel(notificationId)
    }
}

private fun createLargeIcon(context: Context, icon: Int): Bitmap {
    val size = (48 * context.resources.displayMetrics.density).roundToInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val drawable = checkNotNull(context.getDrawable(icon))
    drawable.setBounds(0, 0, size, size)
    drawable.draw(Canvas(bitmap))
    return bitmap
}

internal fun navigationIcon(direction: NavDirection): Int = when (direction) {
    NavDirection.LEFT -> R.drawable.ic_nav_left
    NavDirection.RIGHT -> R.drawable.ic_nav_right
    NavDirection.STRAIGHT -> R.drawable.ic_nav_straight
    NavDirection.UTURN -> R.drawable.ic_nav_uturn
    NavDirection.SLIGHT_LEFT -> R.drawable.ic_nav_slight_left
    NavDirection.SLIGHT_RIGHT -> R.drawable.ic_nav_slight_right
    NavDirection.SHARP_LEFT -> R.drawable.ic_nav_left
    NavDirection.SHARP_RIGHT -> R.drawable.ic_nav_right
    NavDirection.ROUNDABOUT -> R.drawable.ic_nav_roundabout
    NavDirection.KEEP_LEFT, NavDirection.EXIT_LEFT -> R.drawable.ic_nav_left
    NavDirection.KEEP_RIGHT, NavDirection.EXIT_RIGHT -> R.drawable.ic_nav_right
    NavDirection.MERGE, NavDirection.EXIT, NavDirection.FERRY, NavDirection.DESTINATION, NavDirection.UNKNOWN -> R.drawable.ic_nav_straight
}

internal fun formatBandText(navData: NavData, isCompact: Boolean): String {
    val distance = bandLine(navData.distance)
    val road = bandLine(navData.roadName)
    val instruction = instructionLabel(navData)
    val tripSummary = tripSummary(navData)

    val lines = if (isCompact) {
        listOf(bandLine("$instruction $distance"), road, tripSummary)
    } else {
        listOf(distance, instruction, road, tripSummary)
    }

    return lines.filter(String::isNotEmpty).joinToString("\n")
}

internal fun directionLabel(direction: NavDirection): String = when (direction) {
    NavDirection.LEFT -> "← TURN LEFT"
    NavDirection.RIGHT -> "→ TURN RIGHT"
    NavDirection.STRAIGHT -> "↑ GO STRAIGHT"
    NavDirection.SLIGHT_LEFT -> "↖ SLIGHT LEFT"
    NavDirection.SLIGHT_RIGHT -> "↗ SLIGHT RIGHT"
    NavDirection.SHARP_LEFT -> "← SHARP LEFT"
    NavDirection.SHARP_RIGHT -> "→ SHARP RIGHT"
    NavDirection.UTURN -> "↩ U-TURN"
    NavDirection.ROUNDABOUT -> "O ROUNDABOUT"
    NavDirection.KEEP_LEFT -> "↖ KEEP LEFT"
    NavDirection.KEEP_RIGHT -> "↗ KEEP RIGHT"
    NavDirection.MERGE -> "⇢ MERGE"
    NavDirection.EXIT -> "⇢ TAKE EXIT"
    NavDirection.EXIT_LEFT -> "↖ EXIT LEFT"
    NavDirection.EXIT_RIGHT -> "↗ EXIT RIGHT"
    NavDirection.FERRY -> "⇢ TAKE FERRY"
    NavDirection.DESTINATION -> "✓ ARRIVED"
    NavDirection.UNKNOWN -> "↑ CONTINUE"
}

private fun instructionLabel(navData: NavData): String = when {
    navData.direction == NavDirection.ROUNDABOUT && navData.roundaboutExit != null -> "O EXIT ${navData.roundaboutExit}"
    navData.direction == NavDirection.EXIT && navData.exitNumber.isNotEmpty() -> "⇢ EXIT ${navData.exitNumber}"
    navData.direction == NavDirection.UNKNOWN && navData.maneuverText.isNotEmpty() -> bandLine(navData.maneuverText)
    else -> directionLabel(navData.direction)
}

private fun tripSummary(navData: NavData): String {
    val eta = bandLine(navData.eta)
    val totalDistance = bandLine(navData.totalDistance)
    val arrivalTime = bandLine(navData.arrivalTime)

    return bandLine(
        when {
            eta.isNotEmpty() && totalDistance.isNotEmpty() -> "$eta | $totalDistance"
            eta.isNotEmpty() -> "ETA $eta"
            arrivalTime.isNotEmpty() && totalDistance.isNotEmpty() -> "$arrivalTime | $totalDistance"
            arrivalTime.isNotEmpty() -> "ARR $arrivalTime"
            totalDistance.isNotEmpty() -> "TOTAL $totalDistance"
            else -> ""
        }
    )
}

private fun bandLine(value: String, maxLength: Int = 24): String {
    val cleanValue = value.replace(Regex("\\s+"), " ").trim()
    if (cleanValue.length <= maxLength) return cleanValue
    return "${cleanValue.take(maxLength - 1).trimEnd()}…"
}
