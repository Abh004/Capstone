package com.example.testapp

import android.service.notification.NotificationListenerService

/**
 * Required dummy service to satisfy the Android Notification Access requirement.
 * This allows GestureService to access active MediaSessions.
 */
class MediaNotificationListener : NotificationListenerService()