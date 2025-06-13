package com.example.recibo.qr.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recibo.qr.data.QRCode
import com.example.recibo.qr.data.QRCodeRepository
import com.example.recibo.user.data.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.launch
import java.util.UUID

class CreatorViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val qrCodeRepository = QRCodeRepository()
    private val userRepository = UserRepository()

    private val _userPoints = MutableLiveData<Int>()
    val userPoints: LiveData<Int> = _userPoints

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _success = MutableLiveData<String?>()
    val success: LiveData<String?> = _success

    private val _generatedQR = MutableLiveData<GeneratedQRResult?>()
    val generatedQR: LiveData<GeneratedQRResult?> = _generatedQR

    private val _userQRCodes = MutableLiveData<List<QRCode>>()
    val userQRCodes: LiveData<List<QRCode>> = _userQRCodes

    init {
        loadUserPoints()
        loadUserQRCodes()
    }

    private fun loadUserPoints() {
        viewModelScope.launch {
            auth.currentUser?.let { user ->
                try {
                    val result = userRepository.getUser(user.uid)
                    if (result.isSuccess) {
                        _userPoints.value = result.getOrNull()?.points ?: 0
                    }
                } catch (e: Exception) {
                    _error.value = "Error al cargar puntos: ${e.message}"
                }
            }
        }
    }

    private fun loadUserQRCodes() {
        viewModelScope.launch {
            auth.currentUser?.let { user ->
                try {
                    val result = qrCodeRepository.getUserQRCodes(user.uid)
                    if (result.isSuccess) {
                        _userQRCodes.value = result.getOrNull() ?: emptyList()
                    }
                } catch (e: Exception) {
                    _error.value = "Error al cargar QRs: ${e.message}"
                }
            }
        }
    }


    fun generateQR(points: Int, isSingleUse: Boolean) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _error.value = "Usuario no autenticado"
            return
        }

        val currentPoints = _userPoints.value ?: 0
        if (points > currentPoints) {
            _error.value = "No tienes suficientes puntos"
            return
        }

        if (points < 1) {
            _error.value = "Debes asignar al menos 1 punto"
            return
        }

        if (points > 500) {
            _error.value = "No puedes asignar más de 500 puntos por QR"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Generar ID único para el QR
                val qrId = UUID.randomUUID().toString()
                val qrContent = "RECIBO_QR:$qrId"

                // Obtener datos del usuario
                val userResult = userRepository.getUser(currentUser.uid)
                val userName = userResult.getOrNull()?.name ?: "Usuario"

                // Crear objeto QRCode
                val qrCode = QRCode(
                    id = qrId,  // Asignar ID desde el inicio
                    creatorId = currentUser.uid,
                    creatorName = userName,
                    points = points,
                    isSingleUse = isSingleUse,
                    qrContent = qrContent
                )

                // Guardar en Firestore
                val createResult = qrCodeRepository.createQRCodeWithId(qrCode, qrId)
                if (createResult.isSuccess) {
                    // NO descontamos los puntos aquí - se descuentan cuando alguien escanea
                    // Generar bitmap del QR
                    val bitmap = generateQRBitmap(qrContent, 512, 512)

                    _generatedQR.value = GeneratedQRResult(
                        qrCode = qrCode,
                        bitmap = bitmap,
                        qrContent = qrContent
                    )
                    _success.value = "QR generado exitosamente"
                    loadUserQRCodes()
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // REEMPLAZAR el método cancelQR en CreatorViewModel.kt:

    fun cancelQR(qrCode: QRCode) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Solo se puede cancelar si no ha sido escaneado
                if (qrCode.scannedBy != null) {
                    _error.value = "No se puede cancelar un QR que ya fue escaneado"
                    _isLoading.value = false
                    return@launch
                }

                // Usar la nueva función que devuelve puntos
                val result = qrCodeRepository.cancelQRCodeAndRefund(qrCode, userRepository)
                if (result.isSuccess) {
                    _success.value = "QR cancelado y puntos devueltos exitosamente"
                    loadUserQRCodes()
                    loadUserPoints() // Recargar puntos actualizados
                } else {
                    _error.value = "Error al cancelar QR: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _error.value = "Error al cancelar QR: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearMessages() {
        _error.value = null
        _success.value = null
    }

    fun clearGeneratedQR() {
        _generatedQR.value = null
    }

    data class GeneratedQRResult(
        val qrCode: QRCode,
        val bitmap: Bitmap,
        val qrContent: String
    )

    private fun generateQRBitmap(content: String, width: Int, height: Int): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}