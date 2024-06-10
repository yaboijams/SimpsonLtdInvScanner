package com.example.simpsonltdinvscanner

import android.app.Application
import android.util.Log
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

class ScanViewModel(application: Application) : AndroidViewModel(application), EMDKManager.EMDKListener, Scanner.DataListener {
    private lateinit var emdkManager: EMDKManager
    private lateinit var barcodeManager: BarcodeManager
    private lateinit var scanner: Scanner

    private val _locationScanData = MutableLiveData<String>()
    val locationScanData: LiveData<String> get() = _locationScanData

    private val _skuScanData = MutableLiveData<String>()
    val skuScanData: LiveData<String> get() = _skuScanData

    private var isScanningLocation = true
    private var currentLocation: String? = null

    private val db = FirebaseFirestore.getInstance()

    private val locationsCollection = db.collection("Locations")

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    private var isScannerInitialized = false

    init {
        initEMDK(application)
    }

    private fun initEMDK(application: Application) {
        val context = application.applicationContext
        Log.d("ScanViewModel", "Initializing EMDK with context: $context")

        try {
            val results: EMDKResults = EMDKManager.getEMDKManager(context, this)
            if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
                Log.e("ScanViewModel", "Failed to get EMDK Manager. Status code: ${results.statusCode}")
            } else {
                Log.d("ScanViewModel", "EMDK Manager initialization started")
            }
        } catch (e: Exception) {
            Log.e("ScanViewModel", "Exception during EMDK initialization: ${e.message}")
        }
    }


    override fun onOpened(emdkManager: EMDKManager) {
        this.emdkManager = emdkManager
        Log.d("ScanViewModel", "EMDK Manager opened successfully")

        barcodeManager = emdkManager.getInstance(EMDKManager.FEATURE_TYPE.BARCODE) as BarcodeManager
        Log.d("ScanViewModel", "BarcodeManager initialized successfully")
        initScanner()
    }


    private fun initScanner() {
        try {
            Log.d("ScanViewModel", "Initializing scanner")
            if (::scanner.isInitialized) {
                scanner.release()
            }

            scanner = barcodeManager.getDevice(BarcodeManager.DeviceIdentifier.DEFAULT)
            scanner.addDataListener(this)
            scanner.triggerType = Scanner.TriggerType.HARD
            scanner.enable()
            Log.d("ScanViewModel", "Scanner initialized successfully")
            isScannerInitialized = true
        } catch (e: ScannerException) {
            Log.e("ScanViewModel", "Error initializing scanner: ${e.message}")
            isScannerInitialized = false
        }
    }

    fun triggerScanner(isScanningLocation: Boolean) {
        try {
            if (isScannerInitialized) {
                Log.d("ScanViewModel", "Scanner triggered")
                this.isScanningLocation = isScanningLocation
                scanner.read()
            } else {
                Log.e("ScanViewModel", "Scanner is not initialized")
            }
        } catch (e: ScannerException) {
            Log.e("ScanViewModel", "Error triggering scanner: ${e.message}")
        }
    }


    fun prepareForNextScan() {
        if (::scanner.isInitialized) {
            try {
                Log.d("ScanViewModel", "Preparing for next scan")
                scanner.cancelRead()
                scanner.read()
            } catch (e: ScannerException) {
                Log.e("ScanViewModel", "Error preparing for next scan: ${e.message}")
            }
        } else {
            Log.e("ScanViewModel", "Scanner is not initialized")
        }
    }

    override fun onData(scanDataCollection: ScanDataCollection?) {
        if (scanDataCollection != null && scanDataCollection.result == ScannerResults.SUCCESS) {
            for (scanData in scanDataCollection.scanData) {
                val scannedData = scanData.data
                Log.d("ScanViewModel", "Received scan data: $scannedData")
                if (isScanningLocation) {
                    val isValidLocation = isValidLocation(scannedData)
                    if (isValidLocation) {
                        currentLocation = scannedData
                        _locationScanData.postValue(scannedData)
                    } else {
                        Log.d("ScanViewModel", "Invalid location: $scannedData")
                        _errorMessage.postValue("Invalid location: $scannedData")
                    }
                } else {
                    _skuScanData.postValue(scannedData)
                    saveScanDataToFirebase(currentLocation ?: "unknown_location", scannedData)
                }
            }
        }
    }

    private fun isValidLocation(location: String): Boolean {
        var isValid = false
        locationsCollection.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val locCode = document.getString("locCode")
                    if (locCode != null && location.contains(locCode)) {
                        isValid = true
                        break
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ScanViewModel", "Error getting documents: ", exception)
            }
        return isValid
    }

    private fun saveScanDataToFirebase(location: String, sku: String) {
        val scanData = hashMapOf("sku" to sku, "timestamp" to System.currentTimeMillis())
        db.collection("scanner")
            .document(location)
            .collection("skus")
            .add(scanData)
            .addOnSuccessListener { documentReference ->
                Log.d("ScanViewModel", "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w("ScanViewModel", "Error adding document", e)
            }
    }

    override fun onClosed() {
        if (::emdkManager.isInitialized) {
            emdkManager.release()
            Log.d("ScanViewModel", "EMDK Manager released")
        } else {
            Log.e("ScanViewModel", "EMDK Manager was not initialized")
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (::barcodeManager.isInitialized) {
            emdkManager.release(EMDKManager.FEATURE_TYPE.BARCODE)
            Log.d("ScanViewModel", "Barcode Manager released")
        }
    }
}
