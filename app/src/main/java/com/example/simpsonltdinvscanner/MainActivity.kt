package com.example.simpsonltdinvscanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigationView = findViewById(R.id.nav_view)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_scan -> {
                    // Check and request permission before starting scan activity
                    if (hasScannerPermission()) {
                        startActivity(Intent(this, ScanActivity::class.java))
                    } else {
                        requestScannerPermission()
                    }
                    true
                }
                R.id.navigation_inventory -> {
                    startActivity(Intent(this, InventoryActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun hasScannerPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE)
                == PackageManager.PERMISSION_GRANTED)
                && (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestScannerPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.VIBRATE),
            SCANNER_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            SCANNER_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    startActivity(Intent(this, ScanActivity::class.java))
                } else {
                    // Permission denied, handle accordingly
                    // For example, display a message or disable scanning feature
                }
                return
            }
            // Handle other permission requests if needed
        }
    }

    companion object {
        private const val SCANNER_PERMISSION_REQUEST_CODE = 1001
    }
}
