package com.example.recibo.profile.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.example.recibo.user.data.User
import com.example.recibo.user.data.UserRepository
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _updateResult = MutableLiveData<String?>()
    val updateResult: LiveData<String?> = _updateResult

    private val _logoutResult = MutableLiveData<Boolean>()
    val logoutResult: LiveData<Boolean> = _logoutResult

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                _isLoading.value = true
                try {
                    val result = userRepository.getUser(currentUser.uid)
                    if (result.isSuccess) {
                        _user.value = result.getOrNull()
                        _error.value = null
                    } else {
                        _error.value =
                            "Error al cargar perfil: ${result.exceptionOrNull()?.message}"
                    }
                } catch (e: Exception) {
                    _error.value = "Error al cargar perfil: ${e.message}"
                } finally {
                    _isLoading.value = false
                }
            }
        } else {
            _error.value = "Usuario no autenticado"
            _logoutResult.value = true
        }
    }

    fun updateUserName(newName: String) {
        val currentUser = auth.currentUser
        if (currentUser != null && newName.trim().length >= 2) {
            viewModelScope.launch {
                _isLoading.value = true
                try {
                    val result = userRepository.updateUser(
                        currentUser.uid,
                        mapOf("name" to newName.trim())
                    )
                    if (result.isSuccess) {
                        _updateResult.value = "Nombre actualizado correctamente"
                        loadUserProfile() // Recargar el perfil
                    } else {
                        _error.value =
                            "Error al actualizar nombre: ${result.exceptionOrNull()?.message}"
                    }
                } catch (e: Exception) {
                    _error.value = "Error al actualizar nombre: ${e.message}"
                } finally {
                    _isLoading.value = false
                }
            }
        } else {
            _error.value = "El nombre debe tener al menos 2 caracteres"
        }
    }

    fun updatePreferences(notificationsEnabled: Boolean, darkMode: Boolean) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                _isLoading.value = true
                try {
                    val preferences = mapOf(
                        "preferences.notificationsEnabled" to notificationsEnabled,
                        "preferences.darkMode" to darkMode
                    )
                    val result = userRepository.updateUser(currentUser.uid, preferences)
                    if (result.isSuccess) {
                        _updateResult.value = "Preferencias actualizadas"
                        loadUserProfile() // Recargar el perfil
                    } else {
                        _error.value =
                            "Error al actualizar preferencias: ${result.exceptionOrNull()?.message}"
                    }
                } catch (e: Exception) {
                    _error.value = "Error al actualizar preferencias: ${e.message}"
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    fun logout() {
        auth.signOut()
        _user.value = null
        _logoutResult.value = true
    }

    fun clearMessages() {
        _error.value = null
        _updateResult.value = null
    }

    fun getUserStats(): Triple<Int, Int, Int> {
        val currentUser = _user.value
        return Triple(
            currentUser?.points ?: 0,
            currentUser?.level ?: 1,
            currentUser?.totalReceipts ?: 0
        )
    }

    // Métodos adicionales para actualizar estadísticas
    fun addPoints(points: Int) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                try {
                    val currentUserData = _user.value
                    currentUserData?.let { userData ->
                        val newPoints = userData.points + points
                        val newTotalPoints = userData.totalPointsEarned + points
                        val updates = mapOf(
                            "points" to newPoints,
                            "totalPointsEarned" to newTotalPoints
                        )
                        val result = userRepository.updateUser(currentUser.uid, updates)
                        if (result.isSuccess) {
                            loadUserProfile()
                        }
                    }
                } catch (e: Exception) {
                    _error.value = "Error al actualizar puntos: ${e.message}"
                }
            }
        }
    }

    fun incrementReceipts() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                try {
                    val result = userRepository.incrementTotalReceipts(currentUser.uid)
                    if (result.isSuccess) {
                        loadUserProfile()
                    }
                } catch (e: Exception) {
                    _error.value = "Error al actualizar recibos: ${e.message}"
                }
            }
        }
    }
}