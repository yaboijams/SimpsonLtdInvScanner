package com.example.simpsonltdinvscanner

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer

class ScanActivity : AppCompatActivity() {
    private val viewModel: ScanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        viewModel.scanData.observe(this, Observer { scanData ->
            Log.d("ScanActivity", "Scanned data observed: $scanData")
            // Update UI with scanned data
            findViewById<TextView>(R.id.scanDataTextView).text = scanData
        })
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Log.d("ScanActivity", "Key down event: keyCode = $keyCode")
        if (keyCode == 103 || keyCode == 10036) {
            viewModel.triggerScanner()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        Log.d("ScanActivity", "Key up event: keyCode = $keyCode")
        if (keyCode == 103 || keyCode == 10036) {
            // Optionally handle key up event
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}
