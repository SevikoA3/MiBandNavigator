package com.satvik.mibandnavigator

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.app.Notification
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NavigationService : NotificationListenerService() {

    private val TAG = "NavService"
    private val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"

    private val parser = NavigationParser()
    private lateinit var notifier: NotificationHelper

    private var lastDirection: NavDirection = NavDirection.UNKNOWN

    private val uiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val dirName = intent?.getStringExtra("test_dir") ?: return
            try {
                val testDirection = NavDirection.valueOf(dirName)
                val testData = NavData(
                    distance = "150 m",
                    roadName = "Test Road",
                    direction = testDirection,
                    eta = "10 min",
                    totalDistance = "4.5 km",
                    roundaboutExit = if (testDirection == NavDirection.ROUNDABOUT) 2 else null
                )

                // Reset state to force the test notification through
                lastDirection = NavDirection.UNKNOWN
                notifier.clear()
                notifier.sendToBand(testData)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing test direction", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notifier = NotificationHelper(this)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) RECEIVER_EXPORTED else 0
        registerReceiver(uiReceiver, IntentFilter("TRIGGER_TEST_NAV"), flags)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(uiReceiver)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn?.packageName != GOOGLE_MAPS_PACKAGE) return

        val extras = sbn.notification.extras
        val sharedPrefs = getSharedPreferences("NavSettings", Context.MODE_PRIVATE)
        val isVibrationEnabled = sharedPrefs.getBoolean("vibrate_turn", true)

        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val subText = extras.getCharSequence("android.subText")?.toString() ?: ""
        val textLines = extras.getCharSequenceArray("android.textLines")?.map { it.toString() }?.toTypedArray()
        val titleBig = extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString() ?: ""
        val infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString() ?: ""
        val tickerText = sbn.notification.tickerText?.toString() ?: ""

        val cleanData = parser.parseMapsData(
            title, text, subText, textLines, titleBig, bigText, summaryText, infoText, tickerText
        )

        // ==========================================
        // VIBRATION FIX: THE "THROTTLE"
        // ==========================================
        // If the direction is exactly the same as the last one we sent, DO NOTHING.
        // This stops Google Maps from buzzing your wrist every time the distance drops.
        if (cleanData.direction == lastDirection) {
            return
        }

        // We have a NEW direction!
        if (cleanData.direction != NavDirection.UNKNOWN) {
            if (isVibrationEnabled) {
                Log.d(TAG, "📳 TURN CHANGED! Forcing new alert.")
                notifier.clear() // Clear the old notification so Zepp recognizes this as a fresh, buzz-worthy alert
            }

            // Send the update to the band
            notifier.sendToBand(cleanData)

            // Save this direction so we don't buzz again until the next turn
            lastDirection = cleanData.direction
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn?.packageName == GOOGLE_MAPS_PACKAGE) {
            notifier.clear()
            lastDirection = NavDirection.UNKNOWN
        }
    }
}
