package com.example.projectmorpheus.ui.alarm

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.projectmorpheus.MainActivity
import com.example.projectmorpheus.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AlarmDismissActivity : AppCompatActivity() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var alarmId: Long = -1
    private var alarmLabel: String = "Wake up"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_dismiss)

        // Show activity over lock screen and turn screen on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        // Get alarm data from intent
        alarmId = intent.getLongExtra("ALARM_ID", -1)
        alarmLabel = intent.getStringExtra("ALARM_LABEL") ?: "Wake up"
        val shouldVibrate = intent.getBooleanExtra("ALARM_VIBRATE", true)
        val ringtoneUriString = intent.getStringExtra("ALARM_RINGTONE_URI")

        // Set up UI
        val currentTimeText = findViewById<TextView>(R.id.current_time_text)
        val alarmLabelText = findViewById<TextView>(R.id.alarm_label_text)
        val captureNowButton = findViewById<Button>(R.id.capture_now_button)
        val dismissOnlyButton = findViewById<Button>(R.id.dismiss_only_button)

        // Display current time
        val calendar = Calendar.getInstance()
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        currentTimeText.text = timeFormat.format(calendar.time)

        // Display alarm label
        alarmLabelText.text = alarmLabel

        // Play alarm sound
        playAlarmSound(ringtoneUriString)

        // Continue vibration if enabled (started in AlarmReceiver)
        if (shouldVibrate) {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
        }

        // Capture now button - go to journal immediately and dismiss notification
        captureNowButton.setOnClickListener {
            captureNow()
        }

        // Dismiss only button - stop alarm but keep notification for later
        dismissOnlyButton.setOnClickListener {
            dismissAlarmOnly()
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // Prevent dismissing with back button - user must press a button
        // This ensures they're actually awake
        // Intentionally NOT calling super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSoundAndVibration()
    }

    private fun playAlarmSound(ringtoneUriString: String?) {
        try {
            val alarmUri = if (ringtoneUriString != null) {
                Uri.parse(ringtoneUriString)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            ringtone = RingtoneManager.getRingtone(this, alarmUri)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlarmSoundAndVibration() {
        // Stop ringtone
        ringtone?.stop()
        ringtone = null

        // Stop vibration
        try {
            if (vibrator != null) {
                // Replace repeating vibration with a non-repeating one that immediately ends
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(1, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(1)
                }
                // Also call cancel as backup
                vibrator?.cancel()
                vibrator = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Capture dream immediately - navigate to journal and dismiss notification
     */
    private fun captureNow() {
        // Stop sound and vibration
        stopAlarmSoundAndVibration()

        // Dismiss notification since user is capturing now
        val notificationManager = AlarmNotificationManager(this)
        notificationManager.dismissNotification(alarmId)

        // Navigate to main activity with all alarm data
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("OPEN_JOURNAL_ENTRY", true)
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", alarmLabel)
        }
        startActivity(intent)
        finish()
    }

    /**
     * Dismiss alarm only - stop alarm but KEEP notification for later capture
     */
    private fun dismissAlarmOnly() {
        // Stop sound and vibration
        stopAlarmSoundAndVibration()

        // DO NOT dismiss notification - let it persist as a reminder
        // User can tap notification later to capture dream

        // Just close this activity
        finish()
    }
}
