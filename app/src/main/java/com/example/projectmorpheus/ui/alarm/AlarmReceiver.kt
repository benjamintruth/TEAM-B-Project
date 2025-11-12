package com.example.projectmorpheus.ui.alarm

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "alarm_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra("ALARM_ID", -1)
        val alarmLabel = intent.getStringExtra("ALARM_LABEL") ?: "Wake up"
        val shouldVibrate = intent.getBooleanExtra("ALARM_VIBRATE", true)
        val ringtoneUri = intent.getStringExtra("ALARM_RINGTONE_URI")

        Log.d("AlarmReceiver", "✓ ALARM TRIGGERED! ID=$alarmId, Label=$alarmLabel")

        // Create notification manager
        val notificationManager = AlarmNotificationManager(context)
        notificationManager.createNotificationChannel()

        // Vibrate if enabled
        if (shouldVibrate) {
            triggerVibration(context)
        }

        // PATH B: Full-screen alarm activity (immediate capture)
        val fullScreenIntent = Intent(context, AlarmDismissActivity::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", alarmLabel)
            putExtra("ALARM_VIBRATE", shouldVibrate)
            putExtra("ALARM_RINGTONE_URI", ringtoneUri)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // PATH A: Notification action (delayed capture)
        val captureIntent = Intent(context, com.example.projectmorpheus.ui.journal.JournalActivity::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", alarmLabel)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val capturePendingIntent = PendingIntent.getActivity(
            context,
            (alarmId + 100000).toInt(), // Different request code to avoid collision
            captureIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create notification with both paths
        val notification = notificationManager.createAlarmNotification(
            alarmId,
            alarmLabel,
            fullScreenPendingIntent,
            capturePendingIntent
        )

        // Post notification with unique ID
        val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
 
        // Check notification permission (Android 13+)
        val canNotify = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        if (canNotify) {
            systemNotificationManager.notify(
                notificationManager.getNotificationId(alarmId),
                notification
            )
            Log.d("AlarmReceiver", "Notification posted with ID: ${notificationManager.getNotificationId(alarmId)}")
        } else {
            Log.w("AlarmReceiver", "Notification permission not granted on Android 13+.")
        }
    }

    private fun triggerVibration(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Vibrate pattern: wait 0ms, vibrate 1000ms, wait 1000ms, repeat
        val pattern = longArrayOf(0, 1000, 1000)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, 0)
        }
    }
}
