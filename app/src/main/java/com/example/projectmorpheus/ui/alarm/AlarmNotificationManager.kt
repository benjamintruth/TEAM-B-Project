package com.example.projectmorpheus.ui.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.projectmorpheus.R

class AlarmNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "alarm_channel"
        private const val NOTIFICATION_ID_BASE = 1000
    }

    // Generate unique notification ID from alarm ID
    fun getNotificationId(alarmId: Long): Int {
        return NOTIFICATION_ID_BASE + alarmId.toInt()
    }

    fun createAlarmNotification(
        alarmId: Long,
        alarmLabel: String,
        fullScreenIntent: PendingIntent,
        captureIntent: PendingIntent
    ): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Dream Journal")
            .setContentText("$alarmLabel - Tap to capture your dream")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenIntent, true)
            .setContentIntent(captureIntent)
            .addAction(
                R.drawable.ic_menu_book,
                "Capture Dream",
                captureIntent
            )
            .setAutoCancel(false)  // Don't dismiss on tap - we'll do it manually
            .setOngoing(true)      // Keep persistent until manually dismissed
            .build()
    }

    fun dismissNotification(alarmId: Long) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = getNotificationId(alarmId)
        notificationManager.cancel(notificationId)
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Dream Journal Alarms"
            val descriptionText = "Alarms for capturing dreams"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                setSound(
                    alarmSound,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
