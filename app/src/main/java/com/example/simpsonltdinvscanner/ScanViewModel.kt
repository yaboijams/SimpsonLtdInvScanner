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
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId

class ScanViewModel(application: Application) : AndroidViewModel(application), EMDKManager.EMDKListener, Scanner.DataListener {
    private lateinit var emdkManager: EMDKManager
    private var barcodeManager: BarcodeManager? = null
    private var scanner: Scanner? = null

    private val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)

    private val _locationScanData = MutableLiveData<String>()
    val locationScanData: LiveData<String> get() = _locationScanData

    private val _skuScanData = MutableLiveData<String>()
    val skuScanData: LiveData<String> get() = _skuScanData

    private val _roomDescription = MutableLiveData<String>()
    val roomDescription: LiveData<String> get() = _roomDescription

    private val _category = MutableLiveData<String>()
    val category: LiveData<String> get() = _category

    private val _subcategory = MutableLiveData<String>()
    val subcategory: LiveData<String> get() = _subcategory

    private val _subcategoryType = MutableLiveData<String>()
    val subcategoryType: LiveData<String> get() = _subcategoryType

    private val _action = MutableLiveData<String>()
    val action: LiveData<String> get() = _action

    private val _caliber = MutableLiveData<String>()
    val caliber: LiveData<String> get() = _caliber

    private val _license = MutableLiveData<String>()
    val license: LiveData<String> get() = _license

    private val _title = MutableLiveData<String>()
    val title: LiveData<String> get() = _title

    private val _serialnum = MutableLiveData<String>()
    val serialnum: LiveData<String> get() = _serialnum

    private var currentLocation: String? = null

    private val db = FirebaseFirestore.getInstance()
    private val locationsCollection = db.collection("Locations")
    private val algoliaCollection = db.collection("Algolia")
    private val latestScannerInfoCollection = db.collection("LatestScannerInfo")

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

    fun initScanner() {
        try {
            Log.d("ScanViewModel", "Initializing scanner")
            if (scanner != null) {
                scanner?.release()
            }

            if (barcodeManager == null) {
                Log.e("ScanViewModel", "BarcodeManager is not initialized")
                return
            }

            scanner = barcodeManager?.getDevice(BarcodeManager.DeviceIdentifier.DEFAULT)
            scanner?.addDataListener(this)
            scanner?.triggerType = Scanner.TriggerType.HARD
            scanner?.enable()
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
                scanner?.read()
            } else {
                Log.e("ScanViewModel", "Scanner is not initialized")
            }
        } catch (e: ScannerException) {
            Log.e("ScanViewModel", "Error triggering scanner: ${e.message}")
        }
    }

    fun prepareForNextScan() {
        if (scanner != null) {
            try {
                Log.d("ScanViewModel", "Preparing for next scan")
                scanner?.cancelRead()
                scanner?.read()
            } catch (e: ScannerException) {
                Log.e("ScanViewModel", "Error preparing for next scan: ${e.message}")
            }
        } else {
            Log.e("ScanViewModel", "Scanner is not initialized")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onData(scanDataCollection: ScanDataCollection?) {
        if (scanDataCollection != null && scanDataCollection.result == ScannerResults.SUCCESS) {
            for (scanData in scanDataCollection.scanData) {
                val scannedData = scanData.data
                Log.d("ScanViewModel", "Received scan data: $scannedData")
                checkLocationValidityOrSku(scannedData)
            }
        } else {
            playErrorSound()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkLocationValidityOrSku(data: String) {
        locationsCollection.get()
            .addOnSuccessListener { documents ->
                var isLocation = false
                var description = ""
                for (document in documents) {
                    val locCode = document.getString("locCode")
                    val locDescription = document.getString("description")
                    if (locCode != null && data.startsWith(locCode)) {
                        isLocation = true
                        description = locDescription ?: "No description available"
                        currentLocation = data // Update currentLocation when a valid location is found
                        break
                    }
                }
                if (isLocation) {
                    _locationScanData.postValue(data)
                    _roomDescription.postValue(description)
                } else {
                    _skuScanData.postValue(data)
                    fetchAdditionalData(data)
                    saveScanDataToFirebase(data, currentLocation)
                    if (currentLocation.isNullOrEmpty()) {
                        playErrorSound() // Play error sound only if no valid location was set
                    }
                }

                prepareForNextScan()
            }
            .addOnFailureListener { exception ->
                Log.e("ScanViewModel", "Error getting documents: ", exception)
                _errorMessage.postValue("Error checking location: ${exception.message}")
                playErrorSound()
            }
    }

    private fun fetchAdditionalData(sku: String) {
        algoliaCollection.document(sku).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    _category.postValue(document.getString("Category") ?: "Data not available")
                    _subcategory.postValue(document.getString("Subcategory") ?: "Data not available")
                    _subcategoryType.postValue(document.getString("SubcategoryType") ?: "Data not available")
                    _action.postValue(document.getString("Action") ?: "Data not available")
                    _caliber.postValue(document.getString("Caliber") ?: "Data not available")
                    _license.postValue(document.getString("FFLType") ?: "Data not available")
                    _title.postValue(document.getString("Title") ?: "Data not available")
                    _serialnum.postValue(document.getString("SerialNum") ?: "Data not available")
                } else {
                    _category.postValue("Data not available")
                    _subcategory.postValue("Data not available")
                    _subcategoryType.postValue("Data not available")
                    _action.postValue("Data not available")
                    _caliber.postValue("Data not available")
                    _license.postValue("Data not available")
                    _title.postValue("Data not available")
                    _serialnum.postValue("Data not available")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ScanViewModel", "Error fetching additional data: ", exception)
                _category.postValue("Data not available")
                _subcategory.postValue("Data not available")
                _subcategoryType.postValue("Data not available")
                _action.postValue("Data not available")
                _caliber.postValue("Data not available")
                _license.postValue("Data not available")
                _title.postValue("Data not available")
                _serialnum.postValue("Data not available")
                playErrorSound()
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveScanDataToFirebase(sku: String, location: String?) {
        if (location == null) {
            Log.e("ScanViewModel", "Location is null. Cannot save scan data.")
            playErrorSound()
            return
        }

        val docRef = latestScannerInfoCollection.document(sku)
        docRef.get()
            .addOnSuccessListener { document ->
                val previousLocation = document.getString("InvLocation")
                val timestamp = ZonedDateTime.now(ZoneId.of("America/Chicago")).format(DateTimeFormatter.ISO_INSTANT)

                val scannerData = hashMapOf(
                    "InvDate" to timestamp,
                    "InvLocation" to location,
                    "PreviousLocation" to previousLocation
                )

                docRef.set(scannerData)
                    .addOnSuccessListener {
                        Log.d("ScanViewModel", "Data successfully saved for SKU: $sku")
                        triggerScannerSoundTwice() // Trigger the scanner sound twice
                    }
                    .addOnFailureListener { e ->
                        Log.w("ScanViewModel", "Error saving scan data", e)
                        _errorMessage.postValue("Error saving scan data: ${e.message}")
                        playErrorSound()
                    }
            }
            .addOnFailureListener { e ->
                Log.w("ScanViewModel", "Error retrieving existing document", e)
                _errorMessage.postValue("Error retrieving document: ${e.message}")
                playErrorSound()
            }
    }

    private fun triggerScannerSoundTwice() {
        // Assuming that triggering scanner will make the sound, call it twice
        triggerScanner(false)
        triggerScanner(false)
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
                Log.d("ScanViewModel", "Scanner released successfully")
            } catch (e: ScannerException) {
                Log.e("ScanViewModel", "Error releasing scanner: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        releaseScanner()
        super.onCleared()
        toneGenerator.release()
    }
}
