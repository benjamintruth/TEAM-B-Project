package com.example.projectmorpheus.ui.home

import android.os.Bundle
import android.os.Handler
//import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.example.projectmorpheus.data.AlarmDatabase
import androidx.navigation.fragment.findNavController
import com.example.projectmorpheus.databinding.FragmentHomeBinding
import java.util.Calendar
import kotlin.math.min

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Ensures the date shows on the hoem screen
        val currentDate = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date())
        binding.dateText.text = currentDate

        binding.journalCard.setOnClickListener {
            findNavController().navigate(com.example.projectmorpheus.R.id.nav_journal)
        }

        binding.timerCard.setOnClickListener {
            findNavController().navigate(com.example.projectmorpheus.R.id.nav_alarm)
        }

        val hourHand: ImageView = binding.hourhand  // variables for view elements
        val minuteHand: ImageView = binding.minutehand
        val secondHand: ImageView = binding.secondhand
        val progressBars = arrayOf(binding.alarmprogress1, binding.alarmprogress2, binding.alarmprogress3, binding.alarmprogress4)
        val alarmDao = AlarmDatabase.getDatabase(requireContext()).alarmDao()

        //run whenever a second passes (when the view model time is updated)
        homeViewModel.secondDegrees.observe(viewLifecycleOwner) {
            secondHand.rotation = it.toFloat()  // set all the hands to the right time
            minuteHand.rotation = homeViewModel.minuteDegrees.value!!
            hourHand.rotation = homeViewModel.hourDegrees.value!!

            val currentTime = Calendar.getInstance()
            alarmDao.getAllAlarms().asLiveData().observe(viewLifecycleOwner) { alarms -> //update progress bars
                val dayMinute = currentTime.get(Calendar.HOUR_OF_DAY) * 60 + currentTime.get(Calendar.MINUTE) //get the current day time in minutes
                for (bar in progressBars) {
                    bar.visibility = View.INVISIBLE // hide all bars
                }
                for (i in 0 .. min(alarms.size, 4)-1) {
                    val targetMinute = alarms[i].hourOfDay * 60 + alarms[i].minute // get the time from the alarm
                    progressBars[i].progress = (dayMinute - targetMinute).mod(1440) // set times of in-use bars
                    progressBars[i].visibility = View.VISIBLE // show the bars
                }
            }
        }

        //https://stackoverflow.com/questions/55570990/kotlin-call-a-function-every-second
        // update the view model's time every second
        val mainHandler = Handler()
        mainHandler.post(object : Runnable {
            override fun run() {
                mainHandler.postDelayed(this, 1000)
                homeViewModel.updatetime()
            }
        })


        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}