package com.example.recibo.Auth

// AuthViewModel.kt - ViewModel para manejar autenticación
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val currentUser: User? = null,
    val isAuthenticated: Boolean = false,
    val error: String? = null
)

class AuthViewModel(
    private val repository: FirebaseRepository = FirebaseRepository()
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkAuthenticationState()
    }

    fun registerUser(email: String, password: String, name: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)

            val result = repository.registerUser(email, password, name)
            result.fold(
                onSuccess = { user ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        currentUser = user,
                        isAuthenticated = true,
                        error = null
                    )
                },
                onFailure = { exception ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Error desconocido en el registro"
                    )
                }
            )
        }
    }

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)

            val result = repository.loginUser(email, password)
            result.fold(
                onSuccess = { user ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        currentUser = user,
                        isAuthenticated = true,
                        error = null
                    )
                },
                onFailure = { exception ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Error desconocido en el login"
                    )
                }
            )
        }
    }

    fun logout() {
        repository.logout()
        _authState.value = AuthState()
    }

    fun updateUserPoints(newPoints: Int) {
        viewModelScope.launch {
            val currentUser = _authState.value.currentUser ?: return@launch

            val result = repository.updateUserPoints(currentUser.uid, newPoints)
            result.fold(
                onSuccess = {
                    // Actualizar el estado local
                    _authState.value = _authState.value.copy(
                        currentUser = currentUser.copy(points = newPoints)
                    )
                },
                onFailure = { exception ->
                    _authState.value = _authState.value.copy(
                        error = exception.message ?: "Error al actualizar puntos"
                    )
                }
            )
        }
    }

    fun purchaseItem(itemId: String, pointsRequired: Int) {
        viewModelScope.launch {
            val currentUser = _authState.value.currentUser ?: return@launch

            if (currentUser.points >= pointsRequired) {
                val result = repository.addPurchasedItem(currentUser.uid, itemId, pointsRequired)
                result.fold(
                    onSuccess = {
                        // Actualizar el estado local
                        val updatedItems = currentUser.itemsPurchased.toMutableList()
                        updatedItems.add(itemId)
                        _authState.value = _authState.value.copy(
                            currentUser = currentUser.copy(
                                points = currentUser.points - pointsRequired,
                                itemsPurchased = updatedItems
                            )
                        )
                    },
                    onFailure = { exception ->
                        _authState.value = _authState.value.copy(
                            error = exception.message ?: "Error al comprar item"
                        )
                    }
                )
            } else {
                _authState.value = _authState.value.copy(
                    error = "No tienes suficientes puntos"
                )
            }
        }
    }

    fun completeChallenge(challengeId: String, bonusPoints: Int) {
        viewModelScope.launch {
            val currentUser = _authState.value.currentUser ?: return@launch

            val result = repository.completeChallenge(currentUser.uid, challengeId, bonusPoints)
            result.fold(
                onSuccess = {
                    // Actualizar el estado local
                    val updatedChallenges = currentUser.challengesCompleted.toMutableList()
                    updatedChallenges.add(challengeId)
                    _authState.value = _authState.value.copy(
                        currentUser = currentUser.copy(
                            points = currentUser.points + bonusPoints,
                            totalPointsEarned = currentUser.totalPointsEarned + bonusPoints,
                            challengesCompleted = updatedChallenges
                        )
                    )
                },
                onFailure = { exception ->
                    _authState.value = _authState.value.copy(
                        error = exception.message ?: "Error al completar desafío"
                    )
                }
            )
        }
    }

    fun updateProfile(name: String, profileImageUrl: String = "") {
        viewModelScope.launch {
            val currentUser = _authState.value.currentUser ?: return@launch

            val result = repository.updateUserProfile(currentUser.uid, name, profileImageUrl)
            result.fold(
                onSuccess = {
                    // Actualizar el estado local
                    _authState.value = _authState.value.copy(
                        currentUser = currentUser.copy(
                            name = name,
                            profileImageUrl = if (profileImageUrl.isNotEmpty()) profileImageUrl else currentUser.profileImageUrl
                        )
                    )
                },
                onFailure = { exception ->
                    _authState.value = _authState.value.copy(
                        error = exception.message ?: "Error al actualizar perfil"
                    )
                }
            )
        }
    }

    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }

    private fun checkAuthenticationState() {
        viewModelScope.launch {
            if (repository.isUserAuthenticated()) {
                val user = repository.getCurrentUser()
                _authState.value = _authState.value.copy(
                    currentUser = user,
                    isAuthenticated = user != null
                )
            }
        }
    }

    fun refreshUserData() {
        viewModelScope.launch {
            val user = repository.getCurrentUser()
            _authState.value = _authState.value.copy(
                currentUser = user,
                isAuthenticated = user != null
            )
        }
    }
}