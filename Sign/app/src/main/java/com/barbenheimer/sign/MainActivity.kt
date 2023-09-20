
package com.barbenheimer.sign

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

import com.barbenheimer.sign.MainViewModel
import com.barbenheimer.sign.databinding.ActivityMainBinding
import com.barbenheimer.sign.fragment.
import android.view.View

import android.widget.Button
import android.widget.EditText
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    private lateinit var activityMainBinding: ActivityMainBinding
    private val viewModel : MainViewModel by viewModels()
    val fragmentManager = supportFragmentManager
    val fragmentTransaction = fragmentManager.beginTransaction()
    val cameraFragment = CameraFragment()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)
        fragmentTransaction.replace(R.id.fragment_container, cameraFragment)
        fragmentTransaction.addToBackStack(null) // Optional: Adds the transaction to the back stack
        fragmentTransaction.commit()

        // Clear text from TextView
        val clearButton = findViewById<Button>(R.id.clearButton)
        val translationText = findViewById<TextView>(R.id.translationText) // Initialize TextView

        clearButton.setOnClickListener {
            translationText.text = "" // Clear the text in the TextView

    override fun onBackPressed() {
       finish()
    }
}