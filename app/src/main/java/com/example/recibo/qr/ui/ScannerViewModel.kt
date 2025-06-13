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
                val qrResult = qrCodeRepository.getQRCode(qrContent)
                if (qrResult.isSuccess) {
                    val qrCode = qrResult.getOrNull()
                    if (qrCode != null) {
                        val currentUser = auth.currentUser
                        if (currentUser == null) {
                            android.util.Log.e("ScannerVM", "Usuario no autenticado")
                            _scanResult.value = ScanResult.Error("Usuario no autenticado")
                            return@launch
                        }

                        android.util.Log.d("ScannerVM", "Usuario autenticado: ${currentUser.uid}")

                        // Validar que no es su propio QR
                        if (qrCode.creatorId == currentUser.uid) {
                            _scanResult.value = ScanResult.Error("No puedes escanear tu propio QR")
                            return@launch
                        }

                        // Validar que el QR está activo
                        if (!qrCode.isActive) {
                            _scanResult.value = ScanResult.Error("Este QR ya no está activo")
                            return@launch
                        }

                        // Validar si ya fue escaneado por este usuario
                        if (qrCode.scannedBy != null && qrCode.scannedBy == currentUser.uid) {
                            _scanResult.value = ScanResult.Error("Ya has escaneado este QR anteriormente")
                            return@launch
                        }

                        // Validar si es de uso único y ya fue escaneado
                        if (qrCode.scannedBy != null && qrCode.isSingleUse) {
                            _scanResult.value = ScanResult.Error("Este QR ya fue escaneado por otro usuario")
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
        try {
            // Verificar si ya fue escaneado y es de uso único
            if (qrCode.scannedBy != null && qrCode.isSingleUse) {
                _scanResult.value = ScanResult.Error("Este QR ya fue escaneado")
                return
            }

            // Obtener datos del escáner y del creador
            val scannerResult = userRepository.getUser(scannerId)
            val scannerUser = scannerResult.getOrNull()
            val scannerName = scannerUser?.name ?: "Usuario"
            val scannerPoints = scannerUser?.points ?: 0

            val creatorResult = userRepository.getUser(qrCode.creatorId)
            val creatorUser = creatorResult.getOrNull()
            val creatorPoints = creatorUser?.points ?: 0

            // Verificar que el creador tenga suficientes puntos
            if (creatorPoints < qrCode.points) {
                _scanResult.value = ScanResult.Error("El creador no tiene suficientes puntos")
                return
            }

            // Actualizar puntos: sumar al escáner, restar al creador
            val newScannerPoints = scannerPoints + qrCode.points
            val newCreatorPoints = creatorPoints - qrCode.points

            // Actualizar puntos del escáner
            val scannerUpdateResult = userRepository.updateUserPoints(scannerId, newScannerPoints)
            if (!scannerUpdateResult.isSuccess) {
                _scanResult.value = ScanResult.Error("Error al actualizar puntos del escáner")
                return
            }

            // Actualizar puntos totales ganados del escáner
            userRepository.updateTotalPointsEarned(scannerId, qrCode.points)
            userRepository.updateAchievementProgress(scannerId)

            // Restar puntos al creador
            val creatorUpdateResult = userRepository.updateUserPoints(qrCode.creatorId, newCreatorPoints)
            if (!creatorUpdateResult.isSuccess) {
                _scanResult.value = ScanResult.Error("Error al actualizar puntos del creador")
                return
            }

            // Actualizar QR como escaneado
            val qrUpdates = mutableMapOf<String, Any>(
                "scannedBy" to scannerId,
                "scannedByName" to scannerName,
                "scannedAt" to com.google.firebase.Timestamp.now()
            )

            // Si es de uso único, desactivarlo
            if (qrCode.isSingleUse) {
                qrUpdates["isActive"] = false
            }

            val qrUpdateResult = qrCodeRepository.updateQRCode(qrCode.id, qrUpdates)
            if (!qrUpdateResult.isSuccess) {
                _scanResult.value = ScanResult.Error("Error al actualizar QR")
                return
            }

            _scanResult.value = ScanResult.Success(qrCode.points, qrCode.creatorName)
            userRepository.updateAchievementProgress(scannerId)

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