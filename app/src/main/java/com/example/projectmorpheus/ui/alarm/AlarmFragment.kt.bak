package com.example.projectmorpheus.ui.alarm

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ListView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.projectmorpheus.R
import com.example.projectmorpheus.data.Alarm
import com.example.projectmorpheus.data.AlarmDatabase
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.util.Calendar

class AlarmFragment : Fragment() {

    private lateinit var alarmViewModel: AlarmViewModel
    private lateinit var adapter: AlarmListAdapter
    private lateinit var listView: ListView
    private lateinit var emptyStateText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_alarm, container, false)

        // Initialize database and scheduler
        val database = AlarmDatabase.getDatabase(requireContext())
        val alarmDao = database.alarmDao()
        val alarmScheduler = AlarmSchedulerImpl(requireContext())

        // Create ViewModel with factory
        val factory = AlarmViewModelFactory(alarmDao, alarmScheduler)
        alarmViewModel = ViewModelProvider(this, factory)[AlarmViewModel::class.java]

        // Initialize views
        listView = root.findViewById(R.id.alarm_list_view)
        emptyStateText = root.findViewById(R.id.empty_state_text)
        val fab = root.findViewById<FloatingActionButton>(R.id.add_alarm_fab)
        val testButton = root.findViewById<MaterialButton>(R.id.btnTestCaptureDream)

        // Set up adapter
        adapter = AlarmListAdapter(
            onToggleAlarm = { alarm -> alarmViewModel.toggleAlarm(alarm) },
            onDeleteAlarm = { alarm -> alarmViewModel.deleteAlarm(alarm) }
        )
        listView.adapter = adapter

        // Observe alarm list
        alarmViewModel.alarms.observe(viewLifecycleOwner) { alarms ->
            adapter.updateAlarms(alarms)
            // Show/hide empty state
            if (alarms.isEmpty()) {
                listView.visibility = View.GONE
                emptyStateText.visibility = View.VISIBLE
            } else {
                listView.visibility = View.VISIBLE
                emptyStateText.visibility = View.GONE
            }
        }

        // Observe error messages (e.g., permission issues)
        alarmViewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                showPermissionErrorDialog(it)
                alarmViewModel.clearError()
            }
        }

        // FAB click opens time picker
        fab.setOnClickListener {
            showTimePickerDialog()
        }

        // TEST button: open the compose screen (JournalActivity)
        testButton.setOnClickListener {
            startActivity(
                Intent(requireContext(), com.example.projectmorpheus.ui.journal.JournalActivity::class.java)
                    .putExtra("ALARM_LABEL", "Dream — Test")
            )
        }

        return root
    }

    private fun showTimePickerDialog() {
        // Get current time as default
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        // Create MaterialTimePicker
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(currentHour)
            .setMinute(currentMinute)
            .setTitleText("Select alarm time")
            .build()

        // Show time picker and handle selection
        timePicker.addOnPositiveButtonClickListener {
            val selectedHour = timePicker.hour
            val selectedMinute = timePicker.minute
            showAlarmConfigDialog(selectedHour, selectedMinute)
        }

        timePicker.show(parentFragmentManager, "TIME_PICKER")
    }

    private fun showAlarmConfigDialog(hour: Int, minute: Int) {
        // Create custom dialog layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_alarm_config, null)
        val labelInput = dialogView.findViewById<EditText>(R.id.alarm_label_input)
        val vibrateSwitch = dialogView.findViewById<Switch>(R.id.vibrate_switch)

        // Set default values
        labelInput.setText("Wake up")
        vibrateSwitch.isChecked = true

        // Build and show dialog
        AlertDialog.Builder(requireContext())
            .setTitle("Alarm Settings")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val label = labelInput.text.toString().ifBlank { "Wake up" }
                val vibrate = vibrateSwitch.isChecked

                // Create alarm
                val alarm = com.example.projectmorpheus.data.Alarm(
                    hourOfDay = hour,
                    minute = minute,
                    isEnabled = true,
                    label = label,
                    vibrate = vibrate,
                    ringtoneUri = null // Use default sound for now
                )

                Log.d("AlarmFragment", "Creating alarm for $hour:${minute.toString().padStart(2, '0')} - $label")
                alarmViewModel.createAlarm(alarm)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPermissionErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Permission Required")
            .setMessage(message)
            .setPositiveButton("Open Settings") { _, _ ->
                // Open system settings to grant SCHEDULE_EXACT_ALARM permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:${requireContext().packageName}")
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(
                            requireContext(),
                            "Unable to open settings. Please enable 'Alarms & reminders' manually.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
