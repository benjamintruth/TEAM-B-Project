package com.example.projectmorpheus.ui.journal

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.projectmorpheus.R
import com.example.projectmorpheus.data.AlarmDatabase
import com.example.projectmorpheus.data.JournalEntry
import com.example.projectmorpheus.ui.alarm.AlarmNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class JournalActivity : AppCompatActivity() {

    private var entryId: Long? = null
    private var alarmId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_journal_entry)
        title = "Capture Your Dream"

        // Add back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val titleInput = findViewById<EditText>(R.id.journal_title_input)
        val contentInput = findViewById<EditText>(R.id.journal_content_input)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnCancel = findViewById<Button>(R.id.btnCancel)

        // Get alarm data from intent
        alarmId = intent.getLongExtra("ALARM_ID", -1L)
        val alarmLabel = intent.getStringExtra("ALARM_LABEL") ?: "Dream"

        // Prefill title with alarm label
        titleInput.setText(alarmLabel)

        // Get DB + DAO
        val database = AlarmDatabase.getDatabase(applicationContext)
        val journalDao = database.journalDao()

        // Check if editing an existing dream
        entryId = intent.getLongExtra("ENTRY_ID", -1L).takeIf { it > 0 }

        if (entryId != null) {
            // TODO: Load dream content for editing if needed later
            title = "Edit Dream"
        }

        btnSave.setOnClickListener {
            val titleText = titleInput.text.toString().trim()
            val contentText = contentInput.text.toString().trim()

            if (titleText.isNotEmpty() && contentText.isNotEmpty()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val newEntry = JournalEntry(
                        title = titleText,
                        content = contentText,
                        timestamp = System.currentTimeMillis()
                    )
                    journalDao.insert(newEntry)

                    // Dismiss notification if this was from an alarm
                    if (alarmId > 0) {
                        launch(Dispatchers.Main) {
                            val notificationManager = AlarmNotificationManager(this@JournalActivity)
                            notificationManager.dismissNotification(alarmId)
                        }
                    }
                }

                Toast.makeText(this, "Dream saved successfully!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Please enter both title and content", Toast.LENGTH_SHORT).show()
            }
        }

        // Cancel
        btnCancel.setOnClickListener {
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
