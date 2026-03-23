package com.uwb.gesture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log

class GestureService : Service() {

    private val tag = "GestureService"
    private val binder = LocalBinder()

    private lateinit var classifier: UWBClassifier
    private lateinit var audioManager: AudioManager

    private var isRunning = false

    // UI callback — set this from MainActivity
    var onGestureDetected: ((GestureAction) -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): GestureService = this@GestureService
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "onCreate START")

        try {
            Log.d(tag, "Initializing AudioManager...")
            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            Log.d(tag, "AudioManager OK")

            Log.d(tag, "Initializing UWBClassifier...")
            classifier = UWBClassifier(this)
            Log.d(tag, "UWBClassifier OK")

        } catch (e: Exception) {
            Log.e(tag, "CRASH in onCreate: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            stopSelf()
        }

        Log.d(tag, "onCreate COMPLETE")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            isRunning = true
            startForegroundCompat()
            Log.d(tag, "Service started")
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onDestroy() {
        isRunning = false
        classifier.close()
        Log.d(tag, "Service destroyed")
        super.onDestroy()
    }

    // ── Public API (called by MainActivity when sensor data arrives) ─────────

    /**
     * Feed this method with real UWB sensor data when hardware is available.
     *
     * Each array = 8192 floats (128 time-steps × 64 range-bins), row-major.
     *   index = timeStep * 64 + rangeBin
     *
     * For now, MainActivity calls this with simulated data for testing.
     */
    fun processUWBData(left: FloatArray, right: FloatArray, top: FloatArray) {
        if (!isRunning) return
        val gesture = classifier.classify(left, right, top)
        Log.d(tag, "Detected: ${gesture.label}")
        executeAction(gesture)
        onGestureDetected?.invoke(gesture)
    }

    // ── Action Dispatcher ────────────────────────────────────────────────────

    private fun executeAction(gesture: GestureAction) {
        when (gesture) {
            GestureAction.SWIPE_LR -> sendMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_NEXT)
            GestureAction.SWIPE_RL -> sendMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            GestureAction.INWARD_PUSH -> sendMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            GestureAction.CLOCKWISE -> adjustVolume(AudioManager.ADJUST_RAISE)
            GestureAction.ANTICLOCKWISE -> adjustVolume(AudioManager.ADJUST_LOWER)
            GestureAction.SWIPE_UD -> broadcastScroll("SCROLL_UP")
            GestureAction.SWIPE_DU -> broadcastScroll("SCROLL_DOWN")
            GestureAction.EMPTY -> { /* intentional no-op */
            }

            else -> Log.d(tag, "No vehicle action mapped for: $gesture")
        }
    }

    // Replace the sendMediaKey method in GestureService.kt
    private fun sendMediaKey(keyCode: Int) {
        val eventTime = android.os.SystemClock.uptimeMillis()

        // Create the Down event
        val downIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
        downIntent.putExtra(Intent.EXTRA_KEY_EVENT, android.view.KeyEvent(eventTime, eventTime, android.view.KeyEvent.ACTION_DOWN, keyCode, 0))
        sendOrderedBroadcast(downIntent, null)

        // Create the Up event
        val upIntent = Intent(Intent.ACTION_MEDIA_BUTTON)
        upIntent.putExtra(Intent.EXTRA_KEY_EVENT, android.view.KeyEvent(eventTime, eventTime, android.view.KeyEvent.ACTION_UP, keyCode, 0))
        sendOrderedBroadcast(upIntent, null)

        Log.d(tag, "System broadcast media key sent: $keyCode")
    }

    private fun adjustVolume(direction: Int) {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            direction,
            AudioManager.FLAG_SHOW_UI
        )
    }

    private fun broadcastScroll(action: String) {
        sendBroadcast(Intent("com.uwb.gesture.$action"))
    }

    // ── Foreground Notification ──────────────────────────────────────────────

    private fun startForegroundCompat() {
        val channelId = "UWB_GESTURE_CHANNEL"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // NotificationChannel only exists on API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "UWB Gesture Detection",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

        // Notification.Builder with channel ID also requires API 26+
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("UWB Gesture Active")
                .setContentText("Listening for hand gestures...")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build()
        } else {
            // Fallback for API 24-25 — no channel needed
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("UWB Gesture Active")
                .setContentText("Listening for hand gestures...")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(101, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(101, notification)
        }
    }
}
