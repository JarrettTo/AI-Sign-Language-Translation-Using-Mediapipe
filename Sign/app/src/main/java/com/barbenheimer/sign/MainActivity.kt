
package com.barbenheimer.sign

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley

import com.barbenheimer.sign.MainViewModel
import com.barbenheimer.sign.databinding.ActivityMainBinding
import org.json.JSONObject

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.TextView


class MainActivity : AppCompatActivity() {
    private lateinit var activityMainBinding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)


        //declare buttons on create
        val clearButton = findViewById<Button>(R.id.clearButton)
        val translateText = findViewById<TextView>(R.id.translateText)

        clearButton.setOnClickListener {
            translateText.text = ""
        }
        val copyButton = findViewById<Button>(R.id.copyButton)

        copyButton.setOnClickListener {
            copyTextToClipboard(translateText)
        }

            val jsonObject = JSONObject()
            jsonObject.put("data", "WOW")
            val volleyQueue = Volley.newRequestQueue(this)
            val url = "http://10.0.2.2:5000/pose"
            val jsonObjectRequest = JsonObjectRequest(
                // we are using GET HTTP request method
                Request.Method.POST,
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
                    Log.e("MainActivity", "loadDogImage error: ${error.message}")
                }
            )
            volleyQueue.add(jsonObjectRequest)
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_NETWORK_STATE
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission not granted, request it
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.ACCESS_NETWORK_STATE),
                    123
                )
            }

            // Check for INTERNET permission
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission not granted, request it
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.INTERNET),
                    123
                )
            }
        }
    fun clearText(view: View) {
        val textView = findViewById<TextView>(R.id.translateText)
        textView.text = ""
    }
    //copy function
    fun copyTextToClipboard(translateText: TextView) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("label", translateText.text)
        clipboardManager.setPrimaryClip(clipData)
    }


    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
        ) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            if (requestCode == 123) {
                // Check if permissions were granted
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, you can now use the permission
                } else {
                    // Permission denied, handle accordingly
                }
            }
        }

        override fun onBackPressed() {
            finish()
        }
    }
