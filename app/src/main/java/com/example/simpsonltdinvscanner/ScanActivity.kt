package com.example.simpsonltdinvscanner

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer

class ScanActivity : AppCompatActivity() {
    private val viewModel: ScanViewModel by viewModels()

    private var isScanningLocation = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        viewModel.locationScanData.observe(this, Observer { scanData ->
            Log.d("ScanActivity", "Location scanned data observed: $scanData")
            findViewById<TextView>(R.id.actualLocationTextView).text = scanData
            isScanningLocation = false
            viewModel.prepareForNextScan()
        })

        viewModel.skuScanData.observe(this, Observer { scanData ->
            Log.d("ScanActivity", "SKU scanned data observed: $scanData")
            findViewById<TextView>(R.id.scanDataTextView).text = scanData
            isScanningLocation = true
            viewModel.prepareForNextScan()
        })

        viewModel.errorMessage.observe(this, Observer { errorMessage ->
            findViewById<TextView>(R.id.errorMessageTextView).apply {
                text = errorMessage
                visibility = View.VISIBLE
            }
        })
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Log.d("ScanActivity", "Key down event: keyCode = $keyCode")
        if (keyCode == 103 || keyCode == 10036) {
            viewModel.triggerScanner(isScanningLocation)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        Log.d("ScanActivity", "Key up event: keyCode = $keyCode")
        if (keyCode == 103 || keyCode == 10036) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}

