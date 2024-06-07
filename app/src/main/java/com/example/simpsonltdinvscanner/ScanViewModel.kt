package com.example.simpsonltdinvscanner

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.symbol.emdk.EMDKManager
import com.symbol.emdk.EMDKResults
import com.symbol.emdk.barcode.BarcodeManager
import com.symbol.emdk.barcode.ScanDataCollection
import com.symbol.emdk.barcode.Scanner
import com.symbol.emdk.barcode.ScannerException
import com.symbol.emdk.barcode.ScannerResults
import android.app.Application
import androidx.lifecycle.AndroidViewModel

class ScanViewModel(application: Application) : AndroidViewModel(application), EMDKManager.EMDKListener, Scanner.DataListener {
    private lateinit var emdkManager: EMDKManager
    private lateinit var barcodeManager: BarcodeManager
    private lateinit var scanner: Scanner

    private val _scanData = MutableLiveData<String>()
    val scanData: LiveData<String> get() = _scanData

    init {
        initEMDK(application)
    }

    private fun initEMDK(application: Application) {
        val context = application.applicationContext
        Log.d("ScanViewModel", "Initializing EMDK")

        val results: EMDKResults = EMDKManager.getEMDKManager(context, object : EMDKManager.EMDKListener {
            override fun onOpened(emdkManager: EMDKManager) {
                Log.d("ScanViewModel", "EMDK Manager opened successfully")
                this@ScanViewModel.onOpened(emdkManager)
            }

            override fun onClosed() {
                Log.d("ScanViewModel", "EMDK Manager closed")
                this@ScanViewModel.onClosed()
            }
        })

        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            Log.e("ScanViewModel", "Failed to get EMDK Manager. Status code: ${results.statusCode}")
        } else {
            Log.d("ScanViewModel", "EMDK Manager initialization started")
        }
    }

    override fun onOpened(emdkManager: EMDKManager) {
        this.emdkManager = emdkManager
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
        } catch (e: ScannerException) {
            Log.e("ScanViewModel", "Error initializing scanner: ${e.message}")
        }
    }

    fun triggerScanner() {
        try {
            if (::scanner.isInitialized) {
                Log.d("ScanViewModel", "Scanner triggered")
                scanner.read()
            } else {
                Log.e("ScanViewModel", "Scanner is not initialized")
            }
        } catch (e: ScannerException) {
            Log.e("ScanViewModel", "Error triggering scanner: ${e.message}")
        }
    }

    override fun onData(scanDataCollection: ScanDataCollection?) {
        if (scanDataCollection != null && scanDataCollection.result == ScannerResults.SUCCESS) {
            for (scanData in scanDataCollection.scanData) {
                _scanData.postValue(scanData.data)
                Log.d("ScanViewModel", "Received scan data: ${scanData.data}")
            }
        }
    }

    override fun onClosed() {
        emdkManager.release()
        Log.d("ScanViewModel", "EMDK Manager released")
    }

    override fun onCleared() {
        super.onCleared()
        if (::barcodeManager.isInitialized) {
            emdkManager.release(EMDKManager.FEATURE_TYPE.BARCODE)
            Log.d("ScanViewModel", "Barcode Manager released")
        }
    }
}

