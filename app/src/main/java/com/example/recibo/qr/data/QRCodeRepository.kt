package com.example.recibo.qr.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class QRCodeRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val qrCodesCollection = firestore.collection("qr_codes")

    suspend fun createQRCode(qrCode: QRCode): Result<String> {
        return try {
            val docRef = qrCodesCollection.add(qrCode).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createQRCodeWithId(qrCode: QRCode, id: String): Result<String> {
        return try {
            qrCodesCollection.document(id).set(qrCode).await()
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getQRCode(qrContent: String): Result<QRCode?> {
        return try {
            val query = qrCodesCollection
                .whereEqualTo("qrContent", qrContent)
                .limit(1)
                .get()
                .await()

            if (query.documents.isNotEmpty()) {
                val doc = query.documents.first()
                val qrCode = doc.toObject(QRCode::class.java)?.copy(id = doc.id)
                Result.success(qrCode)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateQRCode(id: String, updates: Map<String, Any>): Result<String> {
        return try {
            qrCodesCollection.document(id).update(updates).await()
            Result.success("QR actualizado exitosamente")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserQRCodes(userId: String): Result<List<QRCode>> {
        return try {
            val query = qrCodesCollection
                .whereEqualTo("creatorId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val qrCodes = query.documents.map { doc ->
                doc.toObject(QRCode::class.java)?.copy(id = doc.id) ?: QRCode()
            }
            Result.success(qrCodes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deactivateQRCode(id: String): Result<String> {
        return updateQRCode(id, mapOf("isActive" to false))
    }

    // Agregar al final de QRCodeRepository.kt:

    suspend fun cancelQRCodeAndRefund(qrCode: QRCode, userRepository: com.example.recibo.user.data.UserRepository): Result<String> {
        return try {
            // Primero verificar que el QR no ha sido escaneado
            if (qrCode.scannedBy != null) {
                return Result.failure(Exception("No se puede cancelar un QR que ya fue escaneado"))
            }

            // Desactivar el QR
            val updateResult = deactivateQRCode(qrCode.id)
            if (!updateResult.isSuccess) {
                return Result.failure(Exception("Error al desactivar QR"))
            }

            // Devolver puntos al creador
            val creatorResult = userRepository.getUser(qrCode.creatorId)
            val creator = creatorResult.getOrNull()
            if (creator != null) {
                val newPoints = creator.points + qrCode.points
                val refundResult = userRepository.updateUserPoints(qrCode.creatorId, newPoints)
                if (!refundResult.isSuccess) {
                    return Result.failure(Exception("Error al devolver puntos"))
                }
            }

            Result.success("QR cancelado y puntos devueltos")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}