package com.example.criminalintent

import android.app.Dialog
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView


private const val PHOTO_PATH_ARG = "photo"
class CrimePhotoDialog : DialogFragment() {
    private lateinit var crimePhoto: SubsamplingScaleImageView
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.photo_dialog, container, false)
        crimePhoto = view.findViewById(R.id.crime_photo) as SubsamplingScaleImageView
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val path = arguments?.getString(PHOTO_PATH_ARG)
        path?.let {
            val bitmap = getScaledBitmap(path, requireActivity())
            crimePhoto.setImage(ImageSource.bitmap(bitmap))
        }
    }

    companion object{
        fun newInstance(path: String): CrimePhotoDialog {
            val args = Bundle().apply {
                putString(PHOTO_PATH_ARG, path)
            }
            return CrimePhotoDialog().apply {
                arguments = args
            }
        }
    }
}