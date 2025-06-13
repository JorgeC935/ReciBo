package com.example.recibo.user.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    suspend fun createUser(user: User): Result<String> {
        return try {
            usersCollection.document(user.uid).set(user).await()
            Result.success("Usuario creado exitosamente")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUser(uid: String): Result<User?> {
        return try {
            val document = usersCollection.document(uid).get().await()
            if (document.exists()) {
                val user = document.toObject(User::class.java)
                Result.success(user)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): Result<User?> {
        val currentUser = auth.currentUser
        return if (currentUser != null) {
            getUser(currentUser.uid)
        } else {
            Result.success(null)
        }
    }

    suspend fun updateUser(uid: String, updates: Map<String, Any>): Result<String> {
        return try {
            // Separar updates de campos anidados
            val flatUpdates = mutableMapOf<String, Any>()
            val nestedUpdates = mutableMapOf<String, Any>()

            updates.forEach { (key, value) ->
                if (key.contains(".")) {
                    nestedUpdates[key] = value
                } else {
                    flatUpdates[key] = value
                }
            }

            // Aplicar updates simples
            if (flatUpdates.isNotEmpty()) {
                usersCollection.document(uid).update(flatUpdates).await()
            }

            // Aplicar updates anidados
            if (nestedUpdates.isNotEmpty()) {
                usersCollection.document(uid).update(nestedUpdates).await()
            }

            Result.success("Usuario actualizado exitosamente")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserPoints(uid: String, newPoints: Int): Result<String> {
        return updateUser(uid, mapOf("points" to newPoints))
    }

    suspend fun updateLastLogin(uid: String): Result<String> {
        return updateUser(uid, mapOf("lastLogin" to Timestamp.now()))
    }

    suspend fun addSavedItem(uid: String, itemId: String): Result<String> {
        return try {
            val userDoc = usersCollection.document(uid).get().await()
            val user = userDoc.toObject(User::class.java)
            user?.let {
                val updatedItems = it.savedItems.toMutableList()
                if (!updatedItems.contains(itemId)) {
                    updatedItems.add(itemId)
                    updateUser(uid, mapOf("savedItems" to updatedItems))
                } else {
                    Result.success("Item ya está guardado")
                }
            } ?: Result.failure(Exception("Usuario no encontrado"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeSavedItem(uid: String, itemId: String): Result<String> {
        return try {
            val userDoc = usersCollection.document(uid).get().await()
            val user = userDoc.toObject(User::class.java)
            user?.let {
                val updatedItems = it.savedItems.toMutableList()
                updatedItems.remove(itemId)
                updateUser(uid, mapOf("savedItems" to updatedItems))
            } ?: Result.failure(Exception("Usuario no encontrado"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun incrementTotalReceipts(uid: String): Result<String> {
        return try {
            val userDoc = usersCollection.document(uid).get().await()
            val user = userDoc.toObject(User::class.java)
            user?.let {
                val newTotal = it.totalReceipts + 1
                updateUser(uid, mapOf("totalReceipts" to newTotal))
            } ?: Result.failure(Exception("Usuario no encontrado"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUser(uid: String): Result<String> {
        return try {
            usersCollection.document(uid).delete().await()
            Result.success("Usuario eliminado exitosamente")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // En UserRepository.kt - Reemplazar método updateTotalPointsEarned
    suspend fun updateTotalPointsEarned(uid: String, additionalPoints: Int): Result<String> {
        return try {
            android.util.Log.d("UserRepository", "Actualizando puntos totales para usuario: $uid, puntos adicionales: $additionalPoints")

            val userDoc = usersCollection.document(uid).get().await()
            val user = userDoc.toObject(User::class.java)

            if (user != null) {
                val newTotal = user.totalPointsEarned + additionalPoints
                android.util.Log.d("UserRepository", "Puntos totales anteriores: ${user.totalPointsEarned}, nuevos: $newTotal")

                val updateResult = updateUser(uid, mapOf("totalPointsEarned" to newTotal))
                if (updateResult.isSuccess) {
                    android.util.Log.d("UserRepository", "Puntos totales actualizados exitosamente")
                } else {
                    android.util.Log.e("UserRepository", "Error al actualizar puntos totales: ${updateResult.exceptionOrNull()?.message}")
                }
                updateResult
            } else {
                android.util.Log.e("UserRepository", "Usuario no encontrado para actualizar puntos totales")
                Result.failure(Exception("Usuario no encontrado"))
            }
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Excepción al actualizar puntos totales: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Agregar este método para validar transacciones de puntos
    suspend fun validateAndUpdatePoints(fromUserId: String, toUserId: String, points: Int): Result<String> {
        return try {
            // Verificar que el usuario origen tenga suficientes puntos
            val fromUserResult = getUser(fromUserId)
            val fromUser = fromUserResult.getOrNull()

            if (fromUser == null) {
                return Result.failure(Exception("Usuario origen no encontrado"))
            }

            if (fromUser.points < points) {
                return Result.failure(Exception("Puntos insuficientes"))
            }

            // Actualizar ambos usuarios
            val newFromPoints = fromUser.points - points
            updateUserPoints(fromUserId, newFromPoints)

            val toUserResult = getUser(toUserId)
            val toUser = toUserResult.getOrNull()
            val newToPoints = (toUser?.points ?: 0) + points
            updateUserPoints(toUserId, newToPoints)
            updateTotalPointsEarned(toUserId, points)

            Result.success("Puntos transferidos exitosamente")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // AGREGAR al final de UserRepository.kt
    suspend fun updateAchievementProgress(userId: String): Result<String> {
        return try {
            val userResult = getUser(userId)
            val user = userResult.getOrNull() ?: return Result.failure(Exception("Usuario no encontrado"))

            val achievementRepository = com.example.recibo.achievement.data.AchievementRepository()

            // Obtener todos los logros
            val achievements = achievementRepository.getAllAchievements().getOrNull() ?: emptyList()

            achievements.forEach { achievement ->
                val currentProgress = when (achievement.category) {
                    "points" -> user.totalPointsEarned
                    "scanner" -> user.totalReceipts
                    "creator" -> 0 // Implementar si necesitas contar QRs creados
                    else -> 0
                }

                if (currentProgress > 0) {
                    // Actualizar progreso
                    achievementRepository.updateUserAchievementProgress(userId, achievement.id, currentProgress)

                    // Si se completó el logro, marcarlo como completado
                    if (currentProgress >= achievement.requiredPoints) {
                        achievementRepository.completeAchievement(userId, achievement.id)
                    }
                }
            }

            Result.success("Progreso de logros actualizado")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}