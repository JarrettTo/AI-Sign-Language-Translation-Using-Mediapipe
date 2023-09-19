package com.barbenheimer.sign
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.barbenheimer.sign.databinding.ActivityMainBinding
import com.google.common.util.concurrent.ListenableFuture
import com.google.gson.Gson
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult



import org.json.JSONArray
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.reflect.Array.set
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity(), HandLandmarkerHelper.LandmarkerListener {
    private lateinit var handLandmarkerHelper: HandLandmarkerHelper
    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var interpreter: Interpreter
    private lateinit var inputImageBuffer: TensorImage
    private lateinit var outputKeyPointsBuffer: TensorBuffer
    private var imageCapture: ImageCapture? = null
    val executor: ExecutorService = Executors.newFixedThreadPool(4)
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    var bitmap4Save: Bitmap? = null
    /*var poseArrayList = ArrayList<Pose>()
    var poseArrayListCache = ArrayList<Pose>()*/
    var canvas: Canvas? = null
    var mPaint = Paint()
    var isRunning = false
    var bitmapArrayList = ArrayList<Bitmap>()
    var bitmap4DisplayArrayList = ArrayList<Bitmap>()
    var display: Display? = null

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        cameraExecutor = Executors.newSingleThreadExecutor()
        display = viewBinding.displayOverlay
        mPaint.setColor(Color.GREEN);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaint.setStrokeWidth(10F);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        handLandmarkerHelper = HandLandmarkerHelper(
            context = this,
            runningMode = RunningMode.LIVE_STREAM,
            minHandDetectionConfidence = 0.5F,
            minHandTrackingConfidence = 0.5F,
            minHandPresenceConfidence = 0.5F,
            maxNumHands = 2,
            handLandmarkerHelperListener = this
        )
        // Request camera permissions

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                startCamera(cameraProvider)
            } catch (e: ExecutionException) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            } catch (e: InterruptedException) {
            }
        }, ContextCompat.getMainExecutor(this))
        cameraExecutor.execute {
            if (handLandmarkerHelper.isClose()) {
                handLandmarkerHelper.setupHandLandmarker()
            }
        }
        /*if (!allPermissionsGranted()) {
            getRuntimePermissions();
        }*/
    }


    override fun onError(error: String, errorCode: Int) {
        this?.runOnUiThread {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()

        }
    }

    override fun onResults(resultBundle: HandLandmarkerHelper.ResultBundle) {
        TODO("Not yet implemented")
        Log.d("POSE:","SHIT:" + resultBundle.results.first())
    }

    /*private fun transmitPoints(){
        Log.d("TESTING", "WHAT YHE FUCK")
        val volleyQueue = Volley.newRequestQueue(this)
        val last30Elements = poseArrayListCache.subList(poseArrayListCache.size - 30, poseArrayListCache.size)
        val emptyArray = Array(30) { DoubleArray(132) }
        var index =0
        var j = 0
        //TODO: figure out if poseLandmark coordinates are properly represented in comparison to our training data. By this i mean if the landmarks and their coordinates are arranged in the same way. If landmarks are out of frame, how is it represented? (will there be a gap of 0s in the keypoint matrix or will they just be ignored)
        for (pose in last30Elements){
            j=0
            for (poseLandmark in pose.getAllPoseLandmarks()) {
                emptyArray[index][j] = poseLandmark.position3D.x.toDouble()
                emptyArray[index][j+1] = poseLandmark.position3D.y.toDouble()
                emptyArray[index][j+2] = poseLandmark.position3D.z.toDouble()
                j+=3
            }
            index += 1
        }
        val jsonArray = JSONArray()

        // Iterate through the 2D array and add each row to the JSON array
        for (row in emptyArray) {
            val jsonRow = JSONArray()
            for (element in row) {
                jsonRow.put(element)
            }
            jsonArray.put(jsonRow)
        }

        // Create a JSON object to hold the JSON array
        val jsonObject = JSONObject()
        jsonObject.put("data", jsonArray)

        // Print the JSON object
        println(jsonObject.toString())
        val gson = Gson()
        val json = gson.toJson(emptyArray)
        val url = "http://10.0.2.2:5000/pose"
        val jsonObjectRequest = JsonObjectRequest(
            // we are using GET HTTP request method
            com.android.volley.Request.Method.POST,
            // url we want to send the HTTP request to
            url,
            // this parameter is used to send a JSON object
            // to the server, since this is not required in
            // our case, we are keeping it `null`
            jsonObject,

            // lambda function for handling the case
            // when the HTTP request succeeds
            { response ->
                // get the image url from the JSON object
                val msg = response.get("pose_data")
                //TODO: handle displaying of translations on screen
                Log.d("RES MSG", "message: " + msg)
                // load the image into the ImageView using Glide.

            },

            // lambda function for handling the
            // case when the HTTP request fails
            { error ->
                // make a Toast telling the user
                // that something went wrong

                // log the error message in the error stream
                Log.e("MainActivity", "loadDogImage error: ${error.localizedMessage}")
            }
        )
        volleyQueue.add(jsonObjectRequest)

    }*/

    private fun detectHand(imageProxy: ImageProxy) {
        handLandmarkerHelper.detectLiveStream(
            imageProxy = imageProxy,
            isFrontCamera = false
        )
    }
    private fun takePhoto() {}

    private fun captureVideo() {}
    @RequiresApi(Build.VERSION_CODES.R)
    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun startCamera(cameraProvider: ProcessCameraProvider) {
            Log.i("SHIT", "WOA")
            // Preview
            val preview = Preview.Builder()
                .build()

            preview.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider);
            val imageAnalysis = ImageAnalysis.Builder()
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
//              .setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalysis.setAnalyzer(ActivityCompat.getMainExecutor(this), ImageAnalysis.Analyzer { imageProxy ->

            detectHand(imageProxy)

        })

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA




        try {

            // Bind use cases to camera
            cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, imageAnalysis, preview)

        } catch(exc: Exception) {
            Log.e("SHE", "Use case binding failed", exc)
        }

    }


    private fun getRequiredPermissions(): Array<String?>? {
        return try {
            val info = this.packageManager
                .getPackageInfo(this.packageName, PackageManager.GET_PERMISSIONS)
            val ps = info.requestedPermissions
            if (ps != null && ps.size > 0) {
                ps
            } else {
                arrayOfNulls(0)
            }
        } catch (e: java.lang.Exception) {
            arrayOfNulls(0)
        }
    }

    /*private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }*/
    private fun isPermissionGranted(context: Context, permission: String?): Boolean {
        return (ContextCompat.checkSelfPermission(context, permission!!)
                == PackageManager.PERMISSION_GRANTED)
    }

    private fun getRuntimePermissions() {
        val allNeededPermissions: MutableList<String?> = ArrayList()
        for (permission in getRequiredPermissions()!!) {
            if (!isPermissionGranted(this, permission)) {
                allNeededPermissions.add(permission)
            }
        }
        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                this, allNeededPermissions.toTypedArray<String?>(), PackageManager.GET_PERMISSIONS
            )
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }








}

