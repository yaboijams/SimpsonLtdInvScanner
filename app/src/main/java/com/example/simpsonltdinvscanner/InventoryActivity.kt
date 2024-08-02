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

        // Observe scanned data
        viewModel.scannedData.observe(this, Observer { scannedData ->
            Log.d("InventoryActivity", "Scanned data observed: $scannedData")
            findViewById<TextView>(R.id.scannedDataTextView).text = scannedData
        })

        // Observe inventory date
        viewModel.invDate.observe(this, Observer { invDate ->
            Log.d("InventoryActivity", "Inventory date observed: $invDate")
            findViewById<TextView>(R.id.invDateTextView).text = invDate
        })

        // Observe inventory location
        viewModel.invLocation.observe(this, Observer { invLocation ->
            Log.d("InventoryActivity", "Inventory location observed: $invLocation")
            findViewById<TextView>(R.id.invLocationTextView).text = invLocation
        })

        // Observe previous location
        viewModel.previousLocation.observe(this, Observer { previousLocation ->
            Log.d("InventoryActivity", "Previous location observed: $previousLocation")
            findViewById<TextView>(R.id.previousLocationTextView).text = previousLocation
        })

        // Observe title
        viewModel.title.observe(this, Observer { title ->
            Log.d("InventoryActivity", "Title observed: $title")
            findViewById<TextView>(R.id.titleTextView).text = title
        })

        // Observe action
        viewModel.action.observe(this, Observer { action ->
            Log.d("InventoryActivity", "Action observed: $action")
            findViewById<TextView>(R.id.actionTextView).text = action
        })

        // Observe caliber
        viewModel.caliber.observe(this, Observer { caliber ->
            Log.d("InventoryActivity", "Caliber observed: $caliber")
            findViewById<TextView>(R.id.caliberTextView).text = caliber
        })

        // Observe serial number
        viewModel.serialNum.observe(this, Observer { serialNum ->
            Log.d("InventoryActivity", "Serial number observed: $serialNum")
            findViewById<TextView>(R.id.serialNumTextView).text = serialNum
        })

        // Observe FFL type
        viewModel.fflType.observe(this, Observer { fflType ->
            Log.d("InventoryActivity", "FFL type observed: $fflType")
            findViewById<TextView>(R.id.fflTypeTextView).text = fflType
        })

        // Observe error messages
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
