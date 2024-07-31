package com.example.simpsonltdinvscanner

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer

class InventoryActivity : AppCompatActivity() {
    private val viewModel: InventoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory)

        viewModel.inventoryLocationData.observe(this, Observer { locationData ->
            Log.d("InventoryActivity", "Inventory location data observed: $locationData")
            findViewById<TextView>(R.id.inventoryLocationTextView).text = locationData
        })

        viewModel.errorMessage.observe(this, Observer { errorMessage ->
            findViewById<TextView>(R.id.errorMessageTextView).apply {
                text = errorMessage
                visibility = View.VISIBLE
            }
        })
    }

    override fun onResume() {
        super.onResume()
        // Reinitialize the scanner
        viewModel.initScanner()
    }

    override fun onPause() {
        super.onPause()
        // Release scanner resources when the activity is paused
        viewModel.releaseScanner()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Log.d("InventoryActivity", "Key down event: keyCode = $keyCode")
        if (keyCode == 103 || keyCode == 10036) {
            viewModel.triggerScanner()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        Log.d("InventoryActivity", "Key up event: keyCode = $keyCode")
        if (keyCode == 103 || keyCode == 10036) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}
