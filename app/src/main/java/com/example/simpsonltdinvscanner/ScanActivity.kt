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
        })

        viewModel.errorMessage.observe(this, Observer { errorMessage ->
            findViewById<TextView>(R.id.errorMessageTextView).apply {
                text = errorMessage
                visibility = View.VISIBLE
            }
        })

        viewModel.roomDescription.observe(this, Observer { description ->
            findViewById<TextView>(R.id.roomDescriptionTextView).text = description
        })

        viewModel.category.observe(this, Observer { category ->
            findViewById<TextView>(R.id.categoryTextView).text = category
        })

        viewModel.subcategory.observe(this, Observer { subcategory ->
            findViewById<TextView>(R.id.subcategoryTextView).text = subcategory
        })

        viewModel.subcategoryType.observe(this, Observer { subcategoryType ->
            findViewById<TextView>(R.id.subcategoryTypeTextView).text = subcategoryType
        })

        viewModel.action.observe(this, Observer { action ->
            findViewById<TextView>(R.id.actionTextView).text = action
        })

        viewModel.caliber.observe(this, Observer { caliber ->
            findViewById<TextView>(R.id.caliberTextView).text = caliber
        })

        viewModel.license.observe(this, Observer { license ->
            findViewById<TextView>(R.id.licenseTextView).text = license
        })

        viewModel.title.observe(this, Observer { title ->
            findViewById<TextView>(R.id.titleTextView).text = title
        })
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Log.d("ScanActivity", "Key down event: keyCode = $keyCode")
        when (keyCode) {
            103, 10036 -> {
                viewModel.triggerScanner(isScanningLocation)
                return true
            }
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
