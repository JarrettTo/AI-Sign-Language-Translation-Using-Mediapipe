package com.barbenheimer.sign.fragment

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.barbenheimer.sign.databinding.FragmentCameraBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import com.barbenheimer.sign.MainActivity
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale
import com.barbenheimer.sign.HandLandmarkerHelper
import com.barbenheimer.sign.R
import com.google.mediapipe.tasks.vision.core.RunningMode

typealias LumaListener = (luma: Double) -> Unit

class CameraFragment : Fragment(), HandLandmarkerHelper.LandmarkerListener {
    
    private lateinit var cameraExecutor: ExecutorService

    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    private lateinit var backgroundExecutor: ExecutorService
    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _fragmentCameraBinding =
            FragmentCameraBinding.inflate(inflater, container, false)

        return fragmentCameraBinding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize our background executor
        backgroundExecutor = Executors.newSingleThreadExecutor()

        // Wait for the views to be properly laid out
        fragmentCameraBinding.viewFinder.post {
            // Set up the camera and its use cases
            startCamera()
        }

        // Create the HandLandmarkerHelper that will handle the inference
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(_fragmentCameraBinding?.viewFinder?.surfaceProvider)
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }





    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

//    private val activityResultLauncher =
//        registerForActivityResult(
//            ActivityResultContracts.RequestMultiplePermissions())
//        { permissions ->
//            // Handle Permission granted/rejected
//            var permissionGranted = true
//            permissions.entries.forEach {
//                if (it.key in REQUIRED_PERMISSIONS && it.value == false)
//                    permissionGranted = false
//            }
//            if (!permissionGranted) {
//                Toast.makeText(baseContext,
//                    "Permission request denied",
//                    Toast.LENGTH_SHORT).show()
//            } else {
//                startCamera()
//            }
//        }

    override fun onError(error: String, errorCode: Int) {
//        activity?.runOnUiThread {
//            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
//            if (errorCode == HandLandmarkerHelper.GPU_ERROR) {
//                fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setSelection(
//                    HandLandmarkerHelper.DELEGATE_CPU, false
//                )
//            }
//        }
    }

    override fun onResults(
        resultBundle: HandLandmarkerHelper.ResultBundle
    ) {
//        activity?.runOnUiThread {
//            if (_fragmentCameraBinding != null) {
//                fragmentCameraBinding.bottomSheetLayout.inferenceTimeVal.text =
//                    String.format("%d ms", resultBundle.inferenceTime)
//
//                // Pass necessary information to OverlayView for drawing on the canvas
//                fragmentCameraBinding.overlay.setResults(
//                    resultBundle.results.first(),
//                    resultBundle.inputImageHeight,
//                    resultBundle.inputImageWidth,
//                    RunningMode.LIVE_STREAM
//                )
//
//                // Force a redraw
//                fragmentCameraBinding.overlay.invalidate()
//            }
//        }
    }
}
