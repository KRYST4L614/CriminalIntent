package com.example.criminalintent

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import java.util.Date

private const val DATE_ARG = "dateArg"

class TimePickerFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val timeListener = TimePickerDialog.OnTimeSetListener {
                timePicker: TimePicker, hours: Int, minutes: Int ->
            (targetFragment as Callbacks).onTimeSelected(hours, minutes)
        }
        val date: Date = this.arguments?.getSerializable(DATE_ARG) as Date
        val hours = date.hours
        val minutes = date.minutes
        return TimePickerDialog(this.requireContext(), timeListener, hours, minutes, false)
    }

    companion object {
        fun newInstance(date: Date): TimePickerFragment {
            val args = Bundle().apply {
                putSerializable(DATE_ARG, date)
            }
            return TimePickerFragment().apply {
                arguments = args
            }
        }
    }

    interface Callbacks {
        fun onTimeSelected(hours: Int, minutes: Int)
    }

}