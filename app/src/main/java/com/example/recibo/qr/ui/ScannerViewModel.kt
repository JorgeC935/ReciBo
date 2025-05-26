package com.example.recibo.qr.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ScannerViewModel : ViewModel() {

    private val _scannedResult = MutableLiveData<String>()
    val scannedResult: LiveData<String> = _scannedResult

    private val _isScanning = MutableLiveData<Boolean>()
    val isScanning: LiveData<Boolean> = _isScanning

    private val _hasResult = MutableLiveData<Boolean>()
    val hasResult: LiveData<Boolean> = _hasResult

    init {
        _isScanning.value = true
        _hasResult.value = false
        _scannedResult.value = ""
    }

    fun onQrScanned(result: String) {
        _scannedResult.value = result
        _isScanning.value = false
        _hasResult.value = true
    }

    fun restartScanning() {
        _isScanning.value = true
        _hasResult.value = false
        _scannedResult.value = ""
    }

    fun getLastScannedResult(): String {
        return _scannedResult.value ?: ""
    }
}