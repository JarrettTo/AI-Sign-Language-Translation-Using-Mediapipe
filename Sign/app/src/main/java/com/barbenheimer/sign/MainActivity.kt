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
import android.util.Log
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.barbenheimer.sign.databinding.ActivityMainBinding
import com.google.common.util.concurrent.ListenableFuture
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import okhttp3.Call

import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
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
import android.os.Bundle

import android.view.View

import android.widget.Button

import android.widget.EditText

import android.widget.TextView


class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var interpreter: Interpreter
    private lateinit var inputImageBuffer: TensorImage
    private lateinit var outputKeyPointsBuffer: TensorBuffer
    private var imageCapture: ImageCapture? = null
    val executor: ExecutorService = Executors.newFixedThreadPool(4)
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    var bitmap4Save: Bitmap? = null
    var poseArrayList = ArrayList<Pose>()
    var poseArrayListCache = ArrayList<Pose>()
    var canvas: Canvas? = null
    var mPaint = Paint()
    var isRunning = false
    var bitmapArrayList = ArrayList<Bitmap>()
    var bitmap4DisplayArrayList = ArrayList<Bitmap>()
    var options = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
        .build()
    var display: Display? = null
    var poseDetector = PoseDetection.getClient(options)
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>
    var PERMISSION_REQUESTS = 1



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        display = viewBinding.displayOverlay
        mPaint.setColor(Color.GREEN);
        mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mPaint.setStrokeWidth(10F);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        //clear text
        val clearButton = findViewById<Button>(R.id.clearButton)
        clearButton.setOnClickListener {
            clearText(it)


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

        if (!allPermissionsGranted()) {
            getRuntimePermissions();
        }
    }

    fun clearText(view: View) {
        val textView = findViewById<TextView>(R.id.translateText)
        textView.text = ""
    }

    var RunMlkit = Runnable {
        poseDetector.process(InputImage.fromBitmap(bitmapArrayList[0], 0))
            .addOnSuccessListener { pose ->

                if (pose != null) {

                    poseArrayList.add(pose)
                    poseArrayListCache.add(pose)
                    //TODO: figure out why pose estimation has a delay whether or not its cos of hardware or code issue which can affect the real apk
                    Log.d("SIZE", "SIZE:" + poseArrayListCache.size)
                    if(poseArrayListCache.size>=30){
                        transmitPoints();
                    }
                }
            }.addOnFailureListener { TODO("Not yet implemented") }
    }


    private fun transmitPoints(){
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

            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            // insert your code here.
            // after done, release the ImageProxy object
            val byteBuffer = imageProxy.image!!.planes[0].buffer
            byteBuffer.rewind()
            val bitmap = Bitmap.createBitmap(
                imageProxy.width,
                imageProxy.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(byteBuffer)
            val matrix = Matrix()
            matrix.postRotate(270F)
            matrix.postScale(-1F, 1F)
            val rotatedBitmap = Bitmap.createBitmap(
                bitmap,
                0,
                0,
                imageProxy.width,
                imageProxy.height,
                matrix,
                false
            )
            bitmapArrayList.add(rotatedBitmap)
            if (poseArrayList.size >= 1) {
                canvas = Canvas(bitmapArrayList[0])
                for (poseLandmark in poseArrayList[0].allPoseLandmarks) {
                    canvas!!.drawCircle(
                        poseLandmark.position3D.x,
                        poseLandmark.position3D.y,
                        5F,
                        mPaint
                    )
                }

                bitmap4DisplayArrayList.clear()
                bitmap4DisplayArrayList.add(bitmapArrayList[0])
                bitmap4Save = bitmapArrayList[bitmapArrayList.size - 1]
                bitmapArrayList.clear()
                bitmapArrayList.add(bitmap4Save!!)
                poseArrayList.clear()
                isRunning = false
            }
            if (poseArrayList.isEmpty() && bitmapArrayList.size >= 1 && !isRunning) {

                RunMlkit.run()

                isRunning = true
            }
            if (bitmap4DisplayArrayList.size >= 1) {
                display?.getBitmap(bitmap4DisplayArrayList.get(0))
            }
            imageProxy.close()
        })

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA




            try {

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, imageAnalysis, preview)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
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

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
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
                this, allNeededPermissions.toTypedArray<String?>(), PERMISSION_REQUESTS
            )
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "Sign";
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
        private val REQUIRED_PERMISSIONS =
            listOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toList()
    }


    fun Image.toBitmap(): Bitmap {
        val yBuffer = planes[0].buffer // Y
        val vuBuffer = planes[2].buffer // VU

        val ySize = yBuffer.remaining()
        val vuSize = vuBuffer.remaining()

        val nv21 = ByteArray(ySize + vuSize)

        yBuffer.get(nv21, 0, ySize)
        vuBuffer.get(nv21, ySize, vuSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
    fun ImageProxy.toBitmap(): Bitmap? {
        val nv21 = yuv420888ToNv21(this)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        return yuvImage.toBitmap()
    }

    private fun YuvImage.toBitmap(): Bitmap? {
        val out = ByteArrayOutputStream()
        if (!compressToJpeg(Rect(0, 0, width, height), 100, out))
            return null
        val imageBytes: ByteArray = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
        val pixelCount = image.cropRect.width() * image.cropRect.height()
        val pixelSizeBits = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888)
        val outputBuffer = ByteArray(pixelCount * pixelSizeBits / 8)
        imageToByteBuffer(image, outputBuffer, pixelCount)
        return outputBuffer
    }

    private fun imageToByteBuffer(image: ImageProxy, outputBuffer: ByteArray, pixelCount: Int) {
        assert(image.format == ImageFormat.YUV_420_888)

        val imageCrop = image.cropRect
        val imagePlanes = image.planes

        imagePlanes.forEachIndexed { planeIndex, plane ->
            // How many values are read in input for each output value written
            // Only the Y plane has a value for every pixel, U and V have half the resolution i.e.
            //
            // Y Plane            U Plane    V Plane
            // ===============    =======    =======
            // Y Y Y Y Y Y Y Y    U U U U    V V V V
            // Y Y Y Y Y Y Y Y    U U U U    V V V V
            // Y Y Y Y Y Y Y Y    U U U U    V V V V
            // Y Y Y Y Y Y Y Y    U U U U    V V V V
            // Y Y Y Y Y Y Y Y
            // Y Y Y Y Y Y Y Y
            // Y Y Y Y Y Y Y Y
            val outputStride: Int

            // The index in the output buffer the next value will be written at
            // For Y it's zero, for U and V we start at the end of Y and interleave them i.e.
            //
            // First chunk        Second chunk
            // ===============    ===============
            // Y Y Y Y Y Y Y Y    V U V U V U V U
            // Y Y Y Y Y Y Y Y    V U V U V U V U
            // Y Y Y Y Y Y Y Y    V U V U V U V U
            // Y Y Y Y Y Y Y Y    V U V U V U V U
            // Y Y Y Y Y Y Y Y
            // Y Y Y Y Y Y Y Y
            // Y Y Y Y Y Y Y Y
            var outputOffset: Int

            when (planeIndex) {
                0 -> {
                    outputStride = 1
                    outputOffset = 0
                }
                1 -> {
                    outputStride = 2
                    // For NV21 format, U is in odd-numbered indices
                    outputOffset = pixelCount + 1
                }
                2 -> {
                    outputStride = 2
                    // For NV21 format, V is in even-numbered indices
                    outputOffset = pixelCount
                }
                else -> {
                    // Image contains more than 3 planes, something strange is going on
                    return@forEachIndexed
                }
            }

            val planeBuffer = plane.buffer
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride

            // We have to divide the width and height by two if it's not the Y plane
            val planeCrop = if (planeIndex == 0) {
                imageCrop
            } else {
                Rect(
                    imageCrop.left / 2,
                    imageCrop.top / 2,
                    imageCrop.right / 2,
                    imageCrop.bottom / 2
                )
            }

            val planeWidth = planeCrop.width()
            val planeHeight = planeCrop.height()

            // Intermediate buffer used to store the bytes of each row
            val rowBuffer = ByteArray(plane.rowStride)

            // Size of each row in bytes
            val rowLength = if (pixelStride == 1 && outputStride == 1) {
                planeWidth
            } else {
                // Take into account that the stride may include data from pixels other than this
                // particular plane and row, and that could be between pixels and not after every
                // pixel:
                //
                // |---- Pixel stride ----|                    Row ends here --> |
                // | Pixel 1 | Other Data | Pixel 2 | Other Data | ... | Pixel N |
                //
                // We need to get (N-1) * (pixel stride bytes) per row + 1 byte for the last pixel
                (planeWidth - 1) * pixelStride + 1
            }

            for (row in 0 until planeHeight) {
                // Move buffer position to the beginning of this row
                planeBuffer.position(
                    (row + planeCrop.top) * rowStride + planeCrop.left * pixelStride)

                if (pixelStride == 1 && outputStride == 1) {
                    // When there is a single stride value for pixel and output, we can just copy
                    // the entire row in a single step
                    planeBuffer.get(outputBuffer, outputOffset, rowLength)
                    outputOffset += rowLength
                } else {
                    // When either pixel or output have a stride > 1 we must copy pixel by pixel
                    planeBuffer.get(rowBuffer, 0, rowLength)
                    for (col in 0 until planeWidth) {
                        outputBuffer[outputOffset] = rowBuffer[col * pixelStride]
                        outputOffset += outputStride
                    }
                }
            }
        }
    }


}

