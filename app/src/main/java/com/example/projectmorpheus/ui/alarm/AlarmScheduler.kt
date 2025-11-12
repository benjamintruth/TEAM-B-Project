package com.example.projectmorpheus.ui.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.projectmorpheus.data.Alarm
import com.example.projectmorpheus.data.nextAlarmTimeMillis

interface AlarmScheduler {
    fun schedule(alarm: Alarm)
    fun cancel(alarmId: Long)
    fun canScheduleExactAlarms(): Boolean
}

class AlarmSchedulerImpl(private val context: Context) : AlarmScheduler {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Pre-Android 12 doesn't require permission
        }
    }

    override fun schedule(alarm: Alarm) {
        // Check if we can schedule exact alarms (Android 12+)
        if (!canScheduleExactAlarms()) {
            Log.e("AlarmScheduler", "Cannot schedule exact alarms - permission not granted")
            throw SecurityException("SCHEDULE_EXACT_ALARM permission not granted. Please enable 'Alarms & reminders' in app settings.")
        }

        val triggerTime = alarm.nextAlarmTimeMillis()
        Log.d("AlarmScheduler", "Scheduling alarm ID=${alarm.id} for ${java.util.Date(triggerTime)} (${triggerTime}ms)")

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            putExtra("ALARM_LABEL", alarm.label)
            putExtra("ALARM_RINGTONE_URI", alarm.ringtoneUri)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Use setExactAndAllowWhileIdle for compatibility with Doze mode
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )

        Log.d("AlarmScheduler", "Alarm scheduled successfully")
    }

    override fun cancel(alarmId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}
