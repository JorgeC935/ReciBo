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
            usersCollection.document(uid).update(updates).await()
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
}