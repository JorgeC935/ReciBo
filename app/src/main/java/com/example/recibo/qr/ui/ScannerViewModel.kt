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

    // ARCHIVO: ScannerViewModel.kt
// UBICACIÓN: com/example/recibo/qr/ui/ScannerViewModel.kt
// CAMBIOS: Reemplazar el método processReciboQR completo

    private fun processReciboQR(qrContent: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("ScannerVM", "Procesando QR: $qrContent")

                val qrResult = qrCodeRepository.getQRCode(qrContent)
                if (qrResult.isSuccess) {
                    val qrCode = qrResult.getOrNull()
                    if (qrCode != null) {
                        android.util.Log.d("ScannerVM", "QR encontrado: ${qrCode.id}, Puntos: ${qrCode.points}")

                        val currentUser = auth.currentUser
                        if (currentUser == null) {
                            android.util.Log.e("ScannerVM", "Usuario no autenticado")
                            _scanResult.value = ScanResult.Error("Usuario no autenticado")
                            return@launch
                        }

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

                        // NUEVA LÓGICA: Validar según el tipo de QR
                        if (qrCode.isSingleUse) {
                            // QR de uso único: validar que no haya sido usado
                            if (qrCode.scannedBy != null) {
                                _scanResult.value = ScanResult.Error("Este QR de uso único ya fue utilizado")
                                return@launch
                            }
                        } else {
                            // QR de uso múltiple: validar que este usuario específico no lo haya usado
                            if (qrCode.scannedBy == currentUser.uid) {
                                _scanResult.value = ScanResult.Error("Ya has escaneado este QR anteriormente")
                                return@launch
                            }
                        }

                        // Procesar el escaneo exitoso
                        processSuccessfulScan(qrCode, currentUser.uid)
                    } else {
                        android.util.Log.e("ScannerVM", "QR no encontrado en la base de datos")
                        _scanResult.value = ScanResult.Error("QR no encontrado")
                    }
                } else {
                    android.util.Log.e("ScannerVM", "Error al buscar QR: ${qrResult.exceptionOrNull()?.message}")
                    _scanResult.value = ScanResult.Error("Error al buscar QR: ${qrResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("ScannerVM", "Excepción al procesar QR: ${e.message}", e)
                _scanResult.value = ScanResult.Error("Error al procesar QR: ${e.message}")
            }
        }
    }

    // CAMBIOS: Simplificar el método processSuccessfulScan para MVP
    private suspend fun processSuccessfulScan(qrCode: QRCode, scannerId: String) {
        try {
            // 1. Obtener datos actuales del usuario escaneador
            val scannerResult = userRepository.getUser(scannerId)
            if (!scannerResult.isSuccess) {
                android.util.Log.e("ScannerVM", "Error al obtener usuario escaneador: ${scannerResult.exceptionOrNull()?.message}")
                _scanResult.value = ScanResult.Error("Error al obtener datos del usuario")
                return
            }

            val scannerUser = scannerResult.getOrNull()
            if (scannerUser == null) {
                android.util.Log.e("ScannerVM", "Usuario escaneador no encontrado")
                _scanResult.value = ScanResult.Error("Usuario no encontrado")
                return
            }

            android.util.Log.d("ScannerVM", "Puntos actuales del escáner: ${scannerUser.points}")

            // 2. Calcular nuevos puntos del escaneador
            val newScannerPoints = scannerUser.points + qrCode.points
            android.util.Log.d("ScannerVM", "Nuevos puntos del escáner: $newScannerPoints")

            // 3. Actualizar puntos del escaneador
            val scannerUpdateResult = userRepository.updateUserPoints(scannerId, newScannerPoints)
            if (!scannerUpdateResult.isSuccess) {
                android.util.Log.e("ScannerVM", "Error al actualizar puntos del escáner: ${scannerUpdateResult.exceptionOrNull()?.message}")
                _scanResult.value = ScanResult.Error("Error al actualizar puntos del escáner")
                return
            }
            android.util.Log.d("ScannerVM", "✅ Puntos del escáner actualizados exitosamente")

            // 4. Actualizar puntos totales ganados
            val totalPointsResult = userRepository.updateTotalPointsEarned(scannerId, qrCode.points)
            if (!totalPointsResult.isSuccess) {
                android.util.Log.w("ScannerVM", "Advertencia: Error al actualizar puntos totales: ${totalPointsResult.exceptionOrNull()?.message}")
                // No fallar por esto en el MVP
            }

            // 5. Para MVP: Solo restar puntos al creador si tiene suficientes (opcional)
            val creatorResult = userRepository.getUser(qrCode.creatorId)
            if (creatorResult.isSuccess) {
                val creatorUser = creatorResult.getOrNull()
                if (creatorUser != null && creatorUser.points >= qrCode.points) {
                    val newCreatorPoints = creatorUser.points - qrCode.points
                    val creatorUpdateResult = userRepository.updateUserPoints(qrCode.creatorId, newCreatorPoints)
                    if (creatorUpdateResult.isSuccess) {
                        android.util.Log.d("ScannerVM", "✅ Puntos del creador actualizados")
                    } else {
                        android.util.Log.w("ScannerVM", "Advertencia: No se pudieron restar puntos al creador")
                    }
                } else {
                    android.util.Log.w("ScannerVM", "Advertencia: Creador no tiene suficientes puntos, continuando...")
                }
            }

            // 6. NUEVA LÓGICA: Actualizar QR según su tipo
            val qrUpdates = mutableMapOf<String, Any>()

            if (qrCode.isSingleUse) {
                // QR de uso único: marcar como escaneado y desactivar
                qrUpdates["scannedBy"] = scannerId
                qrUpdates["scannedByName"] = scannerUser.name
                qrUpdates["scannedAt"] = com.google.firebase.Timestamp.now()
                qrUpdates["isActive"] = false
                android.util.Log.d("ScannerVM", "QR de uso único marcado como usado y desactivado")
            } else {
                // QR de uso múltiple: solo registrar el último escaneador
                qrUpdates["scannedBy"] = scannerId
                qrUpdates["scannedByName"] = scannerUser.name
                qrUpdates["scannedAt"] = com.google.firebase.Timestamp.now()
                // NO desactivar el QR para permitir múltiples usos
                android.util.Log.d("ScannerVM", "QR de uso múltiple actualizado, permanece activo")
            }

            val qrUpdateResult = qrCodeRepository.updateQRCode(qrCode.id, qrUpdates)
            if (!qrUpdateResult.isSuccess) {
                android.util.Log.w("ScannerVM", "Advertencia: Error al actualizar QR: ${qrUpdateResult.exceptionOrNull()?.message}")
                // No fallar por esto en el MVP, los puntos ya se otorgaron
            } else {
                android.util.Log.d("ScannerVM", "✅ QR actualizado exitosamente")
            }

            // 7. Actualizar progreso de logros (opcional para MVP)
            try {
                userRepository.updateAchievementProgress(scannerId)
            } catch (e: Exception) {
                android.util.Log.w("ScannerVM", "Advertencia: Error al actualizar logros: ${e.message}")
            }

            // 8. ¡ÉXITO!
            android.util.Log.d("ScannerVM", "=== PROCESO COMPLETADO EXITOSAMENTE ===")
            _scanResult.value = ScanResult.Success(qrCode.points, qrCode.creatorName)

        } catch (e: Exception) {
            android.util.Log.e("ScannerVM", "Excepción crítica en processSuccessfulScan: ${e.message}", e)
            _scanResult.value = ScanResult.Error("Error crítico al procesar escaneo: ${e.message}")
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