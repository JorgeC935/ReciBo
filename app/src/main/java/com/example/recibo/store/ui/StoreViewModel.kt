package com.example.recibo.store.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recibo.store.data.StoreItem
import com.example.recibo.user.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp

class StoreViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var storeItemsListener: ListenerRegistration? = null
    private var userPointsListener: ListenerRegistration? = null

    private val _storeItems = MutableLiveData<List<StoreItem>>()
    val storeItems: LiveData<List<StoreItem>> = _storeItems

    private val _userPoints = MutableLiveData<Int>()
    val userPoints: LiveData<Int> = _userPoints

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _purchaseResult = MutableLiveData<PurchaseResult?>()
    val purchaseResult: LiveData<PurchaseResult?> = _purchaseResult

    private val _purchaseConfirmation = MutableLiveData<StoreItem?>()
    val purchaseConfirmation: LiveData<StoreItem?> = _purchaseConfirmation

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    init {
        loadStoreItems()
        loadUserPoints()
        loadCurrentUser()
    }

    private fun loadStoreItems() {
        _isLoading.value = true

        storeItemsListener = firestore.collection("store_items")
            .whereEqualTo("isAvailable", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                snapshot?.let { querySnapshot ->
                    val items = querySnapshot.documents.mapNotNull { document ->
                        document.toObject(StoreItem::class.java)?.copy(id = document.id)
                    }
                    _storeItems.value = items
                    _isLoading.value = false
                }
            }
    }

    private fun loadUserPoints() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            userPointsListener = firestore.collection("users")
                .document(currentUser.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }
                    val points = snapshot?.getLong("points")?.toInt() ?: 0
                    _userPoints.value = points
                }
        }
    }

    private fun loadCurrentUser() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            firestore.collection("users")
                .document(currentUser.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    val user = snapshot?.toObject(User::class.java)
                    _currentUser.value = user
                }
        }
    }

    fun showPurchaseConfirmation(item: StoreItem) {
        _purchaseConfirmation.value = item
    }

    fun hidePurchaseConfirmation() {
        _purchaseConfirmation.value = null
    }

    fun purchaseItem(item: StoreItem) {
        val currentPoints = _userPoints.value ?: 0

        if (currentPoints < item.price) {
            _purchaseResult.value = PurchaseResult.InsufficientPoints
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                val currentUser = auth.currentUser ?: return@launch

                // Usar transacción para garantizar consistencia
                firestore.runTransaction { transaction ->
                    val userRef = firestore.collection("users").document(currentUser.uid)
                    val userSnapshot = transaction.get(userRef)

                    val currentUserPoints = userSnapshot.getLong("points")?.toInt() ?: 0
                    val currentItemsPurchased = userSnapshot.get("itemsPurchased") as? List<String> ?: emptyList()

                    if (currentUserPoints < item.price) {
                        throw Exception("Puntos insuficientes")
                    }

                    // Verificar si ya compró el item (evitar duplicados)
                    if (currentItemsPurchased.contains(item.id)) {
                        throw Exception("Ya tienes este artículo")
                    }

                    val newPoints = currentUserPoints - item.price
                    val newItemsPurchased = currentItemsPurchased + item.id

                    // Actualizar usuario
                    transaction.update(userRef, mapOf(
                        "points" to newPoints,
                        "itemsPurchased" to newItemsPurchased
                    ))

                    // Registrar la compra
                    val purchaseRef = firestore.collection("purchases").document()
                    transaction.set(purchaseRef, mapOf(
                        "userId" to currentUser.uid,
                        "itemId" to item.id,
                        "itemName" to item.name,
                        "price" to item.price,
                        "purchaseDate" to Timestamp.now()
                    ))
                }.await()

                _purchaseResult.value = PurchaseResult.Success(item.name)
                _purchaseConfirmation.value = null

            } catch (e: Exception) {
                _purchaseResult.value = PurchaseResult.Error("Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearPurchaseResult() {
        _purchaseResult.value = null
    }


    fun canAffordItem(item: StoreItem): Boolean {
        return (_userPoints.value ?: 0) >= item.price
    }

    fun getMissingPoints(item: StoreItem): Int {
        val currentPoints = _userPoints.value ?: 0
        return if (currentPoints < item.price) {
            item.price - currentPoints
        } else {
            0
        }
    }

    override fun onCleared() {
        super.onCleared()
        storeItemsListener?.remove()
        userPointsListener?.remove()
    }

    sealed class PurchaseResult {
        data class Success(val itemName: String) : PurchaseResult()
        object InsufficientPoints : PurchaseResult()
        data class Error(val message: String) : PurchaseResult()
    }
}