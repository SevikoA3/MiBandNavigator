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
    private val CHANNEL_ID = "nav_channel"
    private val NOTIFICATION_ID = 1001

    private val sharedPrefs = context.getSharedPreferences("NavSettings", Context.MODE_PRIVATE)

    init {
        createChannel()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Mi Band Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Sends turn-by-turn alerts to Zepp" }
        notificationManager.createNotificationChannel(channel)
    }

    fun sendToBand(navData: NavData) {
        val isCompact = sharedPrefs.getBoolean("compact_mode", false)
        val text = formatBandText(navData, isCompact)
        val icon = navigationIcon(navData.direction)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setLargeIcon(createLargeIcon(context, icon))
            .setContentTitle(" ")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setOnlyAlertOnce(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun clear() {
        notificationManager.cancel(NOTIFICATION_ID)
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
    NavDirection.ROUNDABOUT -> R.drawable.ic_nav_roundabout
    NavDirection.UNKNOWN -> R.drawable.ic_nav_straight
}

internal fun formatBandText(navData: NavData, isCompact: Boolean): String {
    val instruction = when (navData.direction) {
        NavDirection.LEFT -> "<-- LEFT"
        NavDirection.RIGHT -> "RIGHT -->"
        NavDirection.STRAIGHT -> "^ STRAIGHT"
        NavDirection.UTURN -> "U-TURN"
        NavDirection.SLIGHT_LEFT -> "/< SLIGHT LEFT"
        NavDirection.SLIGHT_RIGHT -> ">\\ SLIGHT RIGHT"
        NavDirection.ROUNDABOUT -> "(O) ROUNDABOUT"
        NavDirection.UNKNOWN -> "CONTINUE"
    }

    return if (isCompact) {
        val bottomLine = if (navData.eta.isNotEmpty()) "${navData.eta} • ${navData.totalDistance}" else navData.totalDistance
        "${navData.distance}\n$instruction\n$bottomLine"
    } else {
        val topText = if (navData.eta.isNotEmpty()) "${navData.distance}  •  ${navData.eta}" else navData.distance
        val bottomText = if (navData.totalDistance.isNotEmpty()) "${navData.roadName}    ${navData.totalDistance}" else navData.roadName
        "$topText\n$instruction\n$bottomText"
    }
}
