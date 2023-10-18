package com.example.criminalintent

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import java.io.File
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.*

private const val TAG = "CrimeFragment"
private const val ARG_CRIME_ID = "crime_id"
private const val DIALOG_DATE = "dialogDate"
private const val DIALOG_TIME = "dialogTime"
private const val DIALOG_PHOTO = "dialogPhoto"
private const val REQUEST_DATE_PICKER = 0
private const val REQUEST_TIME_PICKER = 1
private const val DATE_FORMAT = "EEE, MMM d, yyy"
private const val TIME_FORMAT = "K:m A"
private const val REQUEST_CONTACT = 2
private const val REQUEST_PHOTO = 3

class CrimeFragment : Fragment(), DatePickerFragment.Callbacks, TimePickerFragment.Callbacks {
    private lateinit var crime: Crime
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private lateinit var crimeTitleTextView: TextView
    private lateinit var crimeDetailsTextView: TextView
    private lateinit var crimeEditText: EditText
    private lateinit var dateButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var timeButton: Button
    private lateinit var reportButton: Button
    private lateinit var suspectButton: Button
    private lateinit var callButton: Button
    private lateinit var crimePhoto: ImageView
    private lateinit var crimeCamera: ImageButton
    private lateinit var crimePhotoProgressBar: ProgressBar
    private var crimePhotoWidth = 0
    private var crimePhotoHeight = 0
    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProvider(this).get(CrimeDetailViewModel::class.java)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION") val crimeId = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        crime = Crime("Title")
        crimeDetailViewModel.loadCrime(crimeId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime, container, false)
        crimeTitleTextView = view.findViewById(R.id.crime_title) as TextView
        crimeEditText = view.findViewById(R.id.crime_title_edit_text) as EditText
        dateButton = view.findViewById(R.id.date_button) as Button
        crimeDetailsTextView = view.findViewById(R.id.details_crime_text_view) as TextView
        solvedCheckBox = view.findViewById(R.id.solved_check_box) as CheckBox
        timeButton = view.findViewById(R.id.time_button) as Button
        reportButton = view.findViewById(R.id.crime_report) as Button
        suspectButton = view.findViewById(R.id.crime_suspect) as Button
        callButton = view.findViewById(R.id.call_suspect) as Button
        crimePhoto = view.findViewById(R.id.crime_photo) as ImageView
        crimeCamera = view.findViewById(R.id.crime_camera) as ImageButton
        crimePhotoProgressBar = view.findViewById(R.id.crime_progress_bar) as ProgressBar
        val observer = crimePhoto.viewTreeObserver
        observer.addOnGlobalLayoutListener {
            crimePhotoWidth = crimePhoto.width
            crimePhotoHeight = crimePhoto.height
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(
            viewLifecycleOwner,
            Observer {crime ->
                crime?.let {
                    this.crime = crime
                    photoFile = crimeDetailViewModel.getPhotoFile(crime)
                    photoUri = FileProvider.getUriForFile(requireActivity(), "com.example.criminalintent.fileprovider",
                        photoFile)
                    updateUI()
                }
            }
        )
    }

    override fun onStart() {
        super.onStart()
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                crime.apply {
                    title = p0.toString()
                }
                crimeTitleTextView.text = crime.title
            }

            override fun afterTextChanged(p0: Editable?) {
            }

        }
        crimeEditText.addTextChangedListener(textWatcher)
        solvedCheckBox.apply {
            setOnCheckedChangeListener { _, choice ->
                crime.isSolved = choice
            }
        }
        dateButton.setOnClickListener {
            DatePickerFragment.newInstance(crime.date).apply{
                show(this@CrimeFragment.parentFragmentManager, DIALOG_DATE)
                setTargetFragment(this@CrimeFragment, REQUEST_DATE_PICKER)
            }
        }
        timeButton.setOnClickListener {
            TimePickerFragment.newInstance(crime.date).apply {
                show(this@CrimeFragment.parentFragmentManager, DIALOG_TIME)
                setTargetFragment(this@CrimeFragment, REQUEST_TIME_PICKER)
            }
        }
        reportButton.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject))
            }.also {intent->
                val chooserIntent = Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooserIntent)
            }
        }

        suspectButton.apply {
            val pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            setOnClickListener {
                startActivityForResult(pickContactIntent, REQUEST_CONTACT)
            }
        }

        callButton.apply {
            text = crime.telNumberOfSuspect
            setOnClickListener {
                makeCall(crime.telNumberOfSuspect)
            }
            crimeDetailViewModel.crimeLiveData.observe(
                viewLifecycleOwner
            ) {
                text = it?.telNumberOfSuspect
                isEnabled = it?.suspect?.trim() != ""
            }
        }

        crimeCamera.apply {
            val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            setOnClickListener {
                captureImage.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

                val cameraActivities: List<ResolveInfo> =
                    requireActivity().packageManager.queryIntentActivities(
                        captureImage,
                        PackageManager.MATCH_DEFAULT_ONLY
                    )

                for (cameraActivity in cameraActivities) {
                    requireActivity().grantUriPermission(
                        cameraActivity.activityInfo.packageName,
                        photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
                var chooserIntent = Intent.createChooser(captureImage, "Camera")
                startActivityForResult(chooserIntent, REQUEST_PHOTO)
            }
        }

        crimePhoto.setOnClickListener {
            if (photoFile.exists()) {
                CrimePhotoDialog.newInstance(photoFile.path).apply {
                    show(this@CrimeFragment.parentFragmentManager, DIALOG_PHOTO)
                }
            }
        }
    }

    private fun updatePhotoView() {
        viewLifecycleOwner.lifecycleScope.launch {
            if (photoFile.exists()) {
                val bitmap: Deferred<Bitmap> = async{getScaledBitmap(photoFile.path, crimePhotoWidth, crimePhotoHeight)}
                crimePhotoProgressBar.isVisible = true
                crimePhoto.contentDescription = getString(R.string.crime_photo_image_description)
                crimePhoto.setImageBitmap(bitmap.await())
                crimePhotoProgressBar.isVisible = false
            } else {
                crimePhoto.setImageDrawable(null)
                crimePhoto.contentDescription = getString(R.string.crime_photo_no_image_description)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when {
            resultCode != Activity.RESULT_OK -> return

            requestCode == REQUEST_CONTACT && data != null -> {
                val contactUri: Uri? = data.data
                val suspectName = getFromContactUri(contactUri, arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val suspectNumber = getFromContactUri(contactUri, arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER))
                suspectName?.let {
                    crime.suspect = it
                }
                suspectNumber?.let {
                    crime.telNumberOfSuspect = it
                }
                crimeDetailViewModel.saveCrime(crime)
            }

            requestCode == REQUEST_PHOTO -> {
                updatePhotoView()
                requireActivity().revokeUriPermission(photoUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
        }
    }

    private fun getFromContactUri(contactUri: Uri?, queryFields: Array<String>): String? {
        contactUri?.let{uri ->
            // Выполняемый здесь запрос — contactUri похож на предложение "where"
            val cursor = requireActivity().contentResolver.query(uri, queryFields, null, null, null)
            cursor?.use { cur ->
                if (cur.moveToFirst()) {
                    val columnIndex = cur.getColumnIndex(queryFields[0])
                    return cur.getString(columnIndex)
                }
            }
        }
        return null
    }

    private fun makeCall(number: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel: $number"))
        startActivity(intent)
    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().revokeUriPermission(photoUri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    companion object {
        fun newInstance(id: UUID): CrimeFragment {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, id)
            }
            return CrimeFragment().apply {
                arguments = args
            }
        }
    }

    private fun updateUI(){
        crimeTitleTextView.text = crime.title
        val df = DateFormat.getBestDateTimePattern(Locale.getDefault(), DATE_FORMAT)
        dateButton.text = DateFormat.format(df, crime.date)
        timeButton.text = DateFormat.format(TIME_FORMAT, crime.date)
        solvedCheckBox.isChecked = crime.isSolved
        if (crime.suspect.isNotBlank()) {
            suspectButton.text = crime.suspect
        }
        if (crime.telNumberOfSuspect.isNotBlank()) {
            callButton.text = crime.telNumberOfSuspect
            callButton.isEnabled = true
        } else {
            callButton.isEnabled = false
        }
        updatePhotoView()
    }

    override fun onDateSelected(date: Date) {
        crime.date = date
        updateUI()
    }

    override fun onTimeSelected(hours: Int, minutes: Int) {
        crime.date.apply {
            this.hours = hours
            this.minutes = minutes
        }
        updateUI()
    }

    private fun getCrimeReport(): String {
        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }

        val dateString = DateFormat.format(DATE_FORMAT, crime.date).toString()
        val suspect = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }

        return getString(R.string.crime_report, crime.title, dateString, solvedString, suspect)
    }
}