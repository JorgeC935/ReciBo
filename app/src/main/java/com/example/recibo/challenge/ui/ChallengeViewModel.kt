package com.example.recibo.challenge.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recibo.challenge.data.Challenge
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChallengeViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var challengesListener: ListenerRegistration? = null
    private var userPointsListener: ListenerRegistration? = null

    private val _challenges = MutableLiveData<List<Challenge>>()
    val challenges: LiveData<List<Challenge>> = _challenges

    private val _userPoints = MutableLiveData<Int>()
    val userPoints: LiveData<Int> = _userPoints

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _purchaseResult = MutableLiveData<PurchaseResult?>()
    val purchaseResult: LiveData<PurchaseResult?> = _purchaseResult

    private val _purchaseConfirmation = MutableLiveData<Challenge?>()
    val purchaseConfirmation: LiveData<Challenge?> = _purchaseConfirmation

    init {
        loadChallenges()
        loadUserPoints()
    }

    private fun loadChallenges() {
        _isLoading.value = true

        challengesListener = firestore.collection("challenges")
            .whereEqualTo("isAvailable", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                snapshot?.let { querySnapshot ->
                    val items = querySnapshot.documents.mapNotNull { document ->
                        document.toObject(Challenge::class.java)?.copy(id = document.id)
                    }
                    _challenges.value = items
                    _isLoading.value = false
                }
            }
    }

    private fun loadUserPoints() {
        // Por ahora usamos un documento global de puntos
        // En el futuro, esto cambiará para usar el UID del usuario
        val currentUser = auth.currentUser
        val documentId = currentUser?.uid ?: "global_points"

        userPointsListener = firestore.collection("user_points")
            .document(documentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                val points = snapshot?.getLong("points")?.toInt() ?: 0
                _userPoints.value = points
            }
    }

    fun showPurchaseConfirmation(item: Challenge) {
        _purchaseConfirmation.value = item
    }

    fun hidePurchaseConfirmation() {
        _purchaseConfirmation.value = null
    }

    fun purchaseItem(item: Challenge) {
        val currentPoints = _userPoints.value ?: 0

        if (currentPoints < item.requiredPoints) {
            _purchaseResult.value = PurchaseResult.InsufficientPoints
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Actualizar puntos del usuario
                val currentUser = auth.currentUser
                val documentId = currentUser?.uid ?: "global_points"
                val newPoints = currentPoints - item.requiredPoints

                firestore.collection("user_points")
                    .document(documentId)
                    .update("points", newPoints)
                    .await()

                // Registrar la compra (opcional, para historial)
                val purchase = hashMapOf(
                    "userId" to (currentUser?.uid ?: "anonymous"),
                    "itemId" to item.id,
                    "itemName" to item.name,
                    "requiredPoints" to item.requiredPoints,
                    "purchaseDate" to com.google.firebase.Timestamp.now()
                )

                firestore.collection("purchases")
                    .add(purchase)
                    .await()

                _purchaseResult.value = PurchaseResult.Success(item.name)
                _purchaseConfirmation.value = null

            } catch (e: Exception) {
                _purchaseResult.value = PurchaseResult.Error("Error al realizar la compra: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearPurchaseResult() {
        _purchaseResult.value = null
    }


    fun canAffordItem(item: Challenge): Boolean {
        return (_userPoints.value ?: 0) >= item.requiredPoints
    }

    fun getMissingPoints(item: Challenge): Int {
        val currentPoints = _userPoints.value ?: 0
        return if (currentPoints < item.requiredPoints) {
            item.requiredPoints - currentPoints
        } else {
            0
        }
    }

    override fun onCleared() {
        super.onCleared()
        challengesListener?.remove()
        userPointsListener?.remove()
    }

    sealed class PurchaseResult {
        data class Success(val itemName: String) : PurchaseResult()
        object InsufficientPoints : PurchaseResult()
        data class Error(val message: String) : PurchaseResult()
    }
}