package com.example.simpsonltdinvscanner

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.FirebaseFirestore
import com.symbol.emdk.EMDKManager
import com.symbol.emdk.EMDKResults
import com.symbol.emdk.barcode.BarcodeManager
import com.symbol.emdk.barcode.ScanDataCollection
import com.symbol.emdk.barcode.Scanner
import com.symbol.emdk.barcode.ScannerException
import com.symbol.emdk.barcode.ScannerResults

class InventoryViewModel(application: Application) : AndroidViewModel(application), EMDKManager.EMDKListener, Scanner.DataListener {
    private lateinit var emdkManager: EMDKManager
    private var barcodeManager: BarcodeManager? = null
    private var scanner: Scanner? = null

    private val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)

    private val _inventoryLocationData = MutableLiveData<String>()
    val inventoryLocationData: LiveData<String> get() = _inventoryLocationData

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    private val db = FirebaseFirestore.getInstance()
    private val algoliaCollection = db.collection("Algolia")

    private var isScannerInitialized = false

    init {
        initEMDK(application)
    }

    private fun initEMDK(application: Application) {
        val context = application.applicationContext
        Log.d("InventoryViewModel", "Initializing EMDK with context: $context")

        try {
            val results: EMDKResults = EMDKManager.getEMDKManager(context, this)
            if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
                Log.e("InventoryViewModel", "Failed to get EMDK Manager. Status code: ${results.statusCode}")
            } else {
                Log.d("InventoryViewModel", "EMDK Manager initialization started")
            }
        } catch (e: Exception) {
            Log.e("InventoryViewModel", "Exception during EMDK initialization: ${e.message}")
        }
    }

    override fun onOpened(emdkManager: EMDKManager) {
        this.emdkManager = emdkManager
        Log.d("InventoryViewModel", "EMDK Manager opened successfully")

        barcodeManager = emdkManager.getInstance(EMDKManager.FEATURE_TYPE.BARCODE) as BarcodeManager
        Log.d("InventoryViewModel", "BarcodeManager initialized successfully")
        initScanner()
    }

    fun initScanner() {
        try {
            Log.d("InventoryViewModel", "Initializing scanner")
            if (scanner != null) {
                scanner?.release()
            }

            if (barcodeManager == null) {
                Log.e("InventoryViewModel", "BarcodeManager is not initialized")
                return
            }

            scanner = barcodeManager?.getDevice(BarcodeManager.DeviceIdentifier.DEFAULT)
            scanner?.addDataListener(this)
            scanner?.triggerType = Scanner.TriggerType.HARD
            scanner?.enable()
            Log.d("InventoryViewModel", "Scanner initialized successfully")
            isScannerInitialized = true
        } catch (e: ScannerException) {
            Log.e("InventoryViewModel", "Error initializing scanner: ${e.message}")
            isScannerInitialized = false
        }
    }

    fun triggerScanner() {
        try {
            if (isScannerInitialized) {
                Log.d("InventoryViewModel", "Scanner triggered")
                scanner?.read()
            } else {
                Log.e("InventoryViewModel", "Scanner is not initialized")
            }
        } catch (e: ScannerException) {
            Log.e("InventoryViewModel", "Error triggering scanner: ${e.message}")
        }
    }

    fun prepareForNextScan() {
        if (scanner != null) {
            try {
                Log.d("InventoryViewModel", "Preparing for next scan")
                scanner?.cancelRead()
                scanner?.read()
            } catch (e: ScannerException) {
                Log.e("InventoryViewModel", "Error preparing for next scan: ${e.message}")
            }
        } else {
            Log.e("InventoryViewModel", "Scanner is not initialized")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onData(scanDataCollection: ScanDataCollection?) {
        if (scanDataCollection != null && scanDataCollection.result == ScannerResults.SUCCESS) {
            for (scanData in scanDataCollection.scanData) {
                val scannedData = scanData.data
                Log.d("InventoryViewModel", "Received scan data: $scannedData")
                checkInventoryLocation(scannedData)
            }
        } else {
            playErrorSound()
        }
    }

    private fun checkInventoryLocation(scannedData: String) {
        algoliaCollection.document(scannedData).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    _inventoryLocationData.postValue("Item found in location: ${document.getString("location")}")
                } else {
                    _inventoryLocationData.postValue("Item not found")
                }
                prepareForNextScan()
            }
            .addOnFailureListener { exception ->
                Log.e("InventoryViewModel", "Error checking inventory: ", exception)
                _errorMessage.postValue("Error checking inventory: ${exception.message}")
                playErrorSound()
            }
    }

    private fun playErrorSound() {
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200) // Adjust tone type and duration as needed
    }

    override fun onClosed() {
        if (::emdkManager.isInitialized) {
            emdkManager.release()
        }
    }

    fun releaseScanner() {
        if (scanner != null) {
            try {
                scanner?.release()
                Log.d("InventoryViewModel", "Scanner released successfully")
            } catch (e: ScannerException) {
                Log.e("InventoryViewModel", "Error releasing scanner: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        releaseScanner()
        super.onCleared()
        toneGenerator.release()
    }
}
