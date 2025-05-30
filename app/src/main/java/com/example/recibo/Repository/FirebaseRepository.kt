package com.example.recibo.Repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    // Registro de usuario
    suspend fun registerUser(email: String, password: String, name: String): Result<User> {
        return try {
            // Crear usuario en Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Error al crear usuario")

            // Crear documento del usuario en Firestore
            val user = User(
                uid = firebaseUser.uid,
                name = name,
                email = email,
                points = 0,
                totalPointsEarned = 0,
                itemsPurchased = emptyList(),
                challengesCompleted = emptyList(),
                profileImageUrl = "",
                createdAt = System.currentTimeMillis(),
                lastLogin = System.currentTimeMillis()
            )

            // Guardar en Firestore
            usersCollection.document(firebaseUser.uid).set(user).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Login de usuario
    suspend fun loginUser(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Error al iniciar sesión")

            // Obtener datos del usuario desde Firestore
            val userDoc = usersCollection.document(firebaseUser.uid).get().await()
            val user = userDoc.toObject<User>() ?: throw Exception("Usuario no encontrado en Firestore")

            // Actualizar última conexión
            updateLastLogin(firebaseUser.uid)

            Result.success(user.copy(lastLogin = System.currentTimeMillis()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Obtener usuario actual
    suspend fun getCurrentUser(): User? {
        return try {
            val firebaseUser = auth.currentUser ?: return null
            val userDoc = usersCollection.document(firebaseUser.uid).get().await()
            userDoc.toObject<User>()
        } catch (e: Exception) {
            null
        }
    }

    // Actualizar puntos del usuario
    suspend fun updateUserPoints(uid: String, newPoints: Int): Result<Unit> {
        return try {
            val userRef = usersCollection.document(uid)
            val currentUser = userRef.get().await().toObject<User>()

            if (currentUser != null) {
                val updatedTotalPoints = currentUser.totalPointsEarned + (newPoints - currentUser.points)
                userRef.update(
                    mapOf(
                        "points" to newPoints,
                        "totalPointsEarned" to updatedTotalPoints
                    )
                ).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Agregar item comprado
    suspend fun addPurchasedItem(uid: String, itemId: String, pointsSpent: Int): Result<Unit> {
        return try {
            val userRef = usersCollection.document(uid)
            val currentUser = userRef.get().await().toObject<User>()

            if (currentUser != null) {
                val updatedItems = currentUser.itemsPurchased.toMutableList()
                updatedItems.add(itemId)
                val newPoints = currentUser.points - pointsSpent

                userRef.update(
                    mapOf(
                        "itemsPurchased" to updatedItems,
                        "points" to newPoints
                    )
                ).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Completar desafío
    suspend fun completeChallenge(uid: String, challengeId: String, bonusPoints: Int): Result<Unit> {
        return try {
            val userRef = usersCollection.document(uid)
            val currentUser = userRef.get().await().toObject<User>()

            if (currentUser != null && !currentUser.challengesCompleted.contains(challengeId)) {
                val updatedChallenges = currentUser.challengesCompleted.toMutableList()
                updatedChallenges.add(challengeId)
                val newPoints = currentUser.points + bonusPoints
                val newTotalPoints = currentUser.totalPointsEarned + bonusPoints

                userRef.update(
                    mapOf(
                        "challengesCompleted" to updatedChallenges,
                        "points" to newPoints,
                        "totalPointsEarned" to newTotalPoints
                    )
                ).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Actualizar perfil del usuario
    suspend fun updateUserProfile(uid: String, name: String, profileImageUrl: String = ""): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "name" to name
            )

            if (profileImageUrl.isNotEmpty()) {
                updates["profileImageUrl"] = profileImageUrl
            }

            usersCollection.document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Logout
    fun logout() {
        auth.signOut()
    }

    // Obtener usuario en tiempo real
    fun getUserFlow(uid: String): Flow<User?> = flow {
        try {
            usersCollection.document(uid).addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val user = snapshot?.toObject<User>()
                // emit(user) // No se puede usar emit aquí directamente
            }
        } catch (e: Exception) {
            emit(null)
        }
    }

    // Verificar si el usuario está autenticado
    fun isUserAuthenticated(): Boolean {
        return auth.currentUser != null
    }

    // Obtener UID del usuario actual
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    // Actualizar última conexión
    private suspend fun updateLastLogin(uid: String) {
        try {
            usersCollection.document(uid).update("lastLogin", System.currentTimeMillis()).await()
        } catch (e: Exception) {
            // Log error but don't fail the login process
        }
    }
}