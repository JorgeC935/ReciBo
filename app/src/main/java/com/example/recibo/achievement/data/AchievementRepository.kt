package com.example.recibo.achievement.data

import com.example.recibo.store.data.StoreItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await

class AchievementRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val userAchievementsCollection = firestore.collection("user_achievements")

    suspend fun getAllAchievements(): Result<List<Achievement>> {
        return try {
            val snapshot = firestore.collection("achievements")
                .whereEqualTo("isActive", true)
                .orderBy("requiredPoints")
                .get()
                .await()

            val achievements = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Achievement::class.java)?.copy(id = doc.id)
            }
            Result.success(achievements)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserAchievements(userId: String): Result<List<UserAchievement>> {
        return try {
            val snapshot = userAchievementsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val userAchievements = snapshot.documents.mapNotNull { doc ->
                doc.toObject(UserAchievement::class.java)
            }
            Result.success(userAchievements)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserAchievementProgress(userId: String, achievementId: String, progress: Int): Result<String> {
        return try {
            val docId = "${userId}_${achievementId}"

            // Primero verificar si ya existe
            val existingDoc = userAchievementsCollection.document(docId).get().await()

            if (existingDoc.exists()) {
                // Actualizar solo si el progreso es mayor
                val currentProgress = existingDoc.getLong("currentProgress")?.toInt() ?: 0
                if (progress > currentProgress) {
                    userAchievementsCollection.document(docId).update(
                        mapOf("currentProgress" to progress)
                    ).await()
                }
            } else {
                // Crear nuevo documento
                val userAchievement = UserAchievement(
                    userId = userId,
                    achievementId = achievementId,
                    currentProgress = progress,
                    isCompleted = false,
                    claimed = false
                )
                val achievement = firestore.collection("achievements").document(achievementId).get().await()
                val requiredPoints = achievement.getLong("requiredPoints")?.toInt() ?: 0
                if (progress >= requiredPoints) {
                    completeAchievement(userId, achievementId)
                }
                userAchievementsCollection.document(docId).set(userAchievement).await()
            }

            Result.success("Progreso actualizado")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun completeAchievement(userId: String, achievementId: String): Result<String> {
        return try {
            val docId = "${userId}_${achievementId}"
            val updates = mapOf(
                "isCompleted" to true,
                "completedAt" to Timestamp.now()
            )

            userAchievementsCollection.document(docId).update(updates).await()
            Result.success("Logro completado")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun claimAchievement(userId: String, achievementId: String): Result<String> {
        return try {
            val docId = "${userId}_${achievementId}"
            val updates = mapOf(
                "claimed" to true,
                "claimedAt" to Timestamp.now()
            )

            userAchievementsCollection.document(docId).update(updates).await()
            Result.success("Logro reclamado")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun canClaimAchievement(userId: String, achievementId: String): Result<Boolean> {
        return try {
            val docId = "${userId}_${achievementId}"
            val userAchievementDoc = userAchievementsCollection.document(docId).get().await()

            if (userAchievementDoc.exists()) {
                val userAchievement = userAchievementDoc.toObject(UserAchievement::class.java)
                val canClaim = userAchievement?.isCompleted == true && userAchievement.claimed == false
                Result.success(canClaim)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProgress(userId: String, progressType: String, amount: Int): Result<String> {
        return try {
            // Obtener todos los logros activos
            val achievementsSnapshot = firestore.collection("achievements")
                .whereEqualTo("isActive", true)
                .whereEqualTo("category", progressType)
                .get()
                .await()

            // Actualizar progreso para cada logro relevante
            achievementsSnapshot.documents.forEach { doc ->
                val achievement = doc.toObject(Achievement::class.java)?.copy(id = doc.id)
                achievement?.let {
                    updateUserAchievementProgress(userId, it.id, amount)
                }
            }

            Result.success("Progreso actualizado para todos los logros")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}