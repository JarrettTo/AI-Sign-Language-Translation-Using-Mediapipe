
package com.barbenheimer.sign

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.barbenheimer.sign.databinding.ActivityMainBinding

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
        val translateText = findViewById<TextView>(R.id.translationText)

        clearButton.setOnClickListener {
            translateText.text = ""
        }
        val copyButton = findViewById<Button>(R.id.copyButton)

        copyButton.setOnClickListener {
            copyTextToClipboard(translateText)
        }



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
        val textView = findViewById<TextView>(R.id.translationText)
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
