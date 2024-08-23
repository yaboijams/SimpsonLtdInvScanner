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
import com.google.firebase.Timestamp
import com.symbol.emdk.EMDKManager
import com.symbol.emdk.EMDKResults
import com.symbol.emdk.barcode.BarcodeManager
import com.symbol.emdk.barcode.ScanDataCollection
import com.symbol.emdk.barcode.Scanner
import com.symbol.emdk.barcode.ScannerException
import com.symbol.emdk.barcode.ScannerResults
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class InventoryViewModel(application: Application) : AndroidViewModel(application), EMDKManager.EMDKListener, Scanner.DataListener {
    private lateinit var emdkManager: EMDKManager
    private var barcodeManager: BarcodeManager? = null
    private var scanner: Scanner? = null

    private val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)

    private val _scannedData = MutableLiveData<String>()
    val scannedData: LiveData<String> get() = _scannedData

    private val _invDate = MutableLiveData<String>()
    val invDate: LiveData<String> get() = _invDate

    private val _invLocation = MutableLiveData<String?>()
    val invLocation: MutableLiveData<String?> get() = _invLocation

    private val _previousLocation = MutableLiveData<String>()
    val previousLocation: LiveData<String> get() = _previousLocation

    private val _title = MutableLiveData<String>()
    val title: LiveData<String> get() = _title

    private val _action = MutableLiveData<String>()
    val action: LiveData<String> get() = _action

    private val _caliber = MutableLiveData<String>()
    val caliber: LiveData<String> get() = _caliber

    private val _serialNum = MutableLiveData<String>()
    val serialNum: LiveData<String> get() = _serialNum

    private val _fflType = MutableLiveData<String>()
    val fflType: LiveData<String> get() = _fflType

    private val db = FirebaseFirestore.getInstance()
    private val algoliaCollection = db.collection("Algolia")

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

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
                fetchFirestoreData(scannedData)
            }
        } else {
            playErrorSound()
        }
    }

    private fun fetchFirestoreData(scannedData: String) {
        algoliaCollection.document(scannedData).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    _scannedData.postValue(scannedData)

                    // Handling both Timestamp and String for InvDate
                    val invDate = document.get("InvDate")
                    val formattedDate = when (invDate) {
                        is Timestamp -> {
                            val dateFormat = SimpleDateFormat("MMMM d, yyyy 'at' h:mm:ss a z", Locale.getDefault())
                            dateFormat.timeZone = TimeZone.getDefault() // Use the device's default timezone
                            dateFormat.format(invDate.toDate())
                        }
                        is String -> invDate
                        else -> "Data not currently available"
                    }
                    _invDate.postValue(formattedDate)

                    val invLocation = document.getString("InvLocation") ?: "Data not currently available"
                    _invLocation.postValue(invLocation)

                    Log.d("InventoryViewModel", "InvLocation is set to: ${_invLocation.value}")

                    _previousLocation.postValue(document.getString("PreviousLocation") ?: "Data not currently available")
                    _title.postValue(document.getString("Title") ?: "Data not currently available")
                    _action.postValue(document.getString("Action") ?: "Data not currently available")
                    _caliber.postValue(document.getString("Caliber") ?: "Data not currently available")
                    _serialNum.postValue(document.getString("SerialNum") ?: "Data not currently available")
                    _fflType.postValue(document.getString("FFLType") ?: "Data not currently available")

                } else {
                    _errorMessage.postValue("Item not found")
                    playErrorSound()
                }
                prepareForNextScan()
            }
            .addOnFailureListener { exception ->
                Log.e("InventoryViewModel", "Error fetching Firestore data: ", exception)
                _errorMessage.postValue("Error fetching Firestore data: ${exception.message}")
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
