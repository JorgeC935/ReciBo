package com.example.recibo.qr.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recibo.qr.data.QRCode
import kotlinx.coroutines.launch
import com.example.recibo.qr.data.QRCodeRepository
import com.example.recibo.user.data.UserRepository
import com.google.firebase.auth.FirebaseAuth

class ScannerViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val qrCodeRepository = QRCodeRepository()
    private val userRepository = UserRepository()

    private val _scanResult = MutableLiveData<ScanResult?>()
    val scanResult: LiveData<ScanResult?> = _scanResult

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

        // Procesar QR si es de ReciBo
        if (result.startsWith("RECIBO_QR:")) {
            processReciboQR(result)
        }
    }

    private fun processReciboQR(qrContent: String) {
        viewModelScope.launch {
            try {
                // Extraer el ID del QR del contenido
                val qrId = qrContent.removePrefix("RECIBO_QR:")

                val qrResult = qrCodeRepository.getQRCode(qrContent)
                if (qrResult.isSuccess) {
                    val qrCode = qrResult.getOrNull()
                    if (qrCode != null) {

                        val currentUser = auth.currentUser

                        if (qrCode.scannedBy != null && qrCode.scannedBy == currentUser?.uid) {
                            _scanResult.value = ScanResult.Error("Ya has escaneado este QR anteriormente")
                            return@launch
                        }

                        // Validaciones
                        if (!qrCode.isActive) {
                            _scanResult.value = ScanResult.Error("Este QR ya no está activo")
                            return@launch
                        }


                        if (currentUser == null) {
                            _scanResult.value = ScanResult.Error("Usuario no autenticado")
                            return@launch
                        }

                        if (qrCode.creatorId == currentUser.uid) {
                            _scanResult.value = ScanResult.Error("No puedes escanear tu propio QR")
                            return@launch
                        }

                        if (qrCode.scannedBy != null && qrCode.isSingleUse) {
                            _scanResult.value = ScanResult.Error("Este QR ya fue escaneado")
                            return@launch
                        }

                        // Procesar el escaneo exitoso
                        processSuccessfulScan(qrCode, currentUser.uid)
                    } else {
                        _scanResult.value = ScanResult.Error("QR no encontrado")
                    }
                } else {
                    _scanResult.value = ScanResult.Error("Error al buscar QR: ${qrResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _scanResult.value = ScanResult.Error("Error al procesar QR: ${e.message}")
            }
        }
    }

    private suspend fun processSuccessfulScan(qrCode: QRCode, scannerId: String) {

        if (qrCode.scannedBy != null && qrCode.isSingleUse) {
            _scanResult.value = ScanResult.Error("Este QR ya fue escaneado")
            return
        }
        
        try {
            // Obtener datos del escáner
            val scannerResult = userRepository.getUser(scannerId)
            val scannerName = scannerResult.getOrNull()?.name ?: "Usuario"
            val scannerPoints = scannerResult.getOrNull()?.points ?: 0

            // Actualizar puntos del escáner
            val newScannerPoints = scannerPoints + qrCode.points
            userRepository.updateUserPoints(scannerId, newScannerPoints)
            userRepository.updateTotalPointsEarned(scannerId, qrCode.points)

            // Actualizar QR como escaneado
            val qrUpdates = mutableMapOf<String, Any>(
                "scannedBy" to scannerId,
                "scannedByName" to scannerName,
                "scannedAt" to com.google.firebase.Timestamp.now()
            )

            if (qrCode.isSingleUse) {
                qrUpdates["isActive"] = false
            }

            qrCodeRepository.updateQRCode(qrCode.id, qrUpdates)

            _scanResult.value = ScanResult.Success(qrCode.points, qrCode.creatorName)

        } catch (e: Exception) {
            _scanResult.value = ScanResult.Error("Error al procesar escaneo: ${e.message}")
        }
    }

    sealed class ScanResult {
        data class Success(val pointsEarned: Int, val creatorName: String) : ScanResult()
        data class Error(val message: String) : ScanResult()
    }

    fun clearScanResult() {
        _scanResult.value = null
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