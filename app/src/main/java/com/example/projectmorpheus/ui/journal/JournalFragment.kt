package com.example.projectmorpheus.ui.journal

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.projectmorpheus.R
import android.widget.Button
import android.widget.ImageButton
import com.example.projectmorpheus.data.AlarmDatabase
import com.example.projectmorpheus.data.JournalEntry
import com.example.projectmorpheus.databinding.FragmentJournalBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton

class JournalFragment : Fragment() {

    private var _binding: FragmentJournalBinding? = null
    private val binding get() = _binding!!

    private lateinit var journalViewModel: JournalViewModel
    private lateinit var adapter: JournalListAdapter
    private lateinit var listView: ListView
    private lateinit var emptyStateText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJournalBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize database and ViewModel
        val database = AlarmDatabase.getDatabase(requireContext())
        val journalDao = database.journalDao()
        val factory = JournalViewModelFactory(journalDao)
        journalViewModel = ViewModelProvider(this, factory)[JournalViewModel::class.java]

        // Initialize views
        listView = binding.journalListView
        emptyStateText = binding.textJournal
        val fab = binding.addJournalFab

        // Set up adapter
        adapter = JournalListAdapter(
            onItemClick = { entry -> showEntryDialog(entry) },
            onItemLongClick = { entry -> showDeleteConfirmation(entry) }
        )
        listView.adapter = adapter

        // Observe entries
        journalViewModel.entries.observe(viewLifecycleOwner) { entries ->
            adapter.updateEntries(entries)
            // Show/hide empty state
            if (entries.isEmpty()) {
                listView.visibility = View.GONE
                emptyStateText.visibility = View.VISIBLE
            } else {
                listView.visibility = View.VISIBLE
                emptyStateText.visibility = View.GONE
            }
        }

        // FAB click opens create dialog
        fab.setOnClickListener {
            showEntryDialog(null)
        }

        // Auto-open dialog if navigated from alarm dismiss
        if (arguments?.getBoolean("auto_open_entry", false) == true) {
            showEntryDialog(null)
            arguments?.remove("auto_open_entry")
        }

        return root
    }

    private fun showEntryDialog(entry: JournalEntry?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_journal_entry, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.journal_title_input)
        val contentInput = dialogView.findViewById<EditText>(R.id.journal_content_input)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnDelete = dialogView.findViewById<ImageButton>(R.id.btnDelete)

        // Pre-fill if editing
        entry?.let {
            titleInput.setText(it.title)
            contentInput.setText(it.content)
            btnDelete.visibility = View.VISIBLE
        }
        val titleView = dialogView.findViewById<TextView>(R.id.journal_dialog_title)
        val dialogTitle = if (entry == null) "New Journal Entry" else "Edit Journal Entry"
        titleView.text = dialogTitle

        val dialog = AlertDialog.Builder(requireContext())
            //.setTitle(dialogTitle)
            .setView(dialogView)
            /*.setPositiveButton("Save") { _, _ ->
                val title = titleInput.text.toString().trim()
                val content = contentInput.text.toString().trim()

                if (title.isNotEmpty() && content.isNotEmpty()) {
                    if (entry == null) {
                        journalViewModel.createEntry(title, content)
                    } else {
                        journalViewModel.updateEntry(entry.id, title, content, entry.timestamp)
                    }
                }
            }
            .setNegativeButton("Cancel", null) */
            .create()

        btnSave.setOnClickListener {
            val title = titleInput.text.toString().trim()
            val content = contentInput.text.toString().trim()

            if (title.isNotEmpty() && content.isNotEmpty()) {
                if (entry == null) {
                    journalViewModel.createEntry(title, content)
                } else {
                    journalViewModel.updateEntry(entry.id, title, content, entry.timestamp)
                }
                dialog.dismiss()
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // Handle Delete button manually
        btnDelete?.setOnClickListener {
            entry?.let { showDeleteConfirmation(it) }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteConfirmation(entry: JournalEntry) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Entry")
            .setMessage("Are you sure you want to delete \"${entry.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                journalViewModel.deleteEntry(entry)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
