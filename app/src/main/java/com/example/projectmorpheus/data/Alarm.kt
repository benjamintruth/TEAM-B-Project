package com.example.projectmorpheus.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val hourOfDay: Int,        // 0-23
    val minute: Int,           // 0-59

    val isEnabled: Boolean = true,

	// DEV: need to move to strings.txt
    val label: String = "Wake up",

    val ringtoneUri: String? = null  // null = default sound
)

// Calculate next alarm time for one-time alarm
fun Alarm.nextAlarmTimeMillis(): Long {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hourOfDay)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)

        // If time has passed today, schedule for tomorrow
        if (timeInMillis < System.currentTimeMillis()) {
            add(Calendar.DAY_OF_MONTH, 1)
        }
    }
    return calendar.timeInMillis
}
