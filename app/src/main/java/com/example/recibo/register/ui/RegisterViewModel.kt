package com.example.recibo.register.ui

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.Timestamp
import com.example.recibo.user.data.User
import com.example.recibo.user.data.UserPreferences
import com.example.recibo.user.data.UserRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegisterViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()

    private val _name = MutableLiveData<String>()
    val name: LiveData<String> = _name

    private val _email = MutableLiveData<String>()
    val email: LiveData<String> = _email

    private val _password = MutableLiveData<String>()
    val password: LiveData<String> = _password

    private val _confirmPassword = MutableLiveData<String>()
    val confirmPassword: LiveData<String> = _confirmPassword

    private val _registerEnable = MutableLiveData<Boolean>()
    val registerEnable: LiveData<Boolean> = _registerEnable

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _registerResult = MutableLiveData<RegisterResult>()
    val registerResult: LiveData<RegisterResult> = _registerResult

    fun onRegisterChanged(name: String, email: String, password: String, confirmPassword: String) {
        _name.value = name
        _email.value = email
        _password.value = password
        _confirmPassword.value = confirmPassword
        _registerEnable.value = isValidName(name) && isValidEmail(email) &&
                isValidPassword(password) && isValidConfirmPassword(confirmPassword, password)
    }

    fun onRegisterSelected() {
        val currentName = _name.value ?: ""
        val currentEmail = _email.value ?: ""
        val currentPassword = _password.value ?: ""

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Crear usuario en Firebase Auth
                val authResult = auth.createUserWithEmailAndPassword(currentEmail, currentPassword).await()

                // 2. Actualizar perfil del usuario con el nombre
                val firebaseUser = authResult.user
                firebaseUser?.let { user ->
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(currentName)
                        .build()

                    try {
                        user.updateProfile(profileUpdates).await()
                        user.reload().await() // Recargar el usuario para obtener los datos actualizados
                    } catch (e: Exception) {
                        // Si falla la actualización del perfil, continúa de todos modos
                    }

                    // 3. Crear documento en Firestore con el modelo actualizado
                    val userDocument = User(
                        uid = user.uid,
                        name = currentName,
                        email = currentEmail,
                        points = 0,
                        level = 1,
                        totalReceipts = 0,
                        totalPointsEarned = 0,
                        itemsPurchased = emptyList(),
                        savedItems = emptyList(),
                        achievements = emptyList(),
                        challengesCompleted = emptyList(),
                        createdAt = Timestamp.now(),
                        lastLogin = Timestamp.now(),
                        profileImageUrl = "",
                        preferences = UserPreferences()
                    )

                    val firestoreResult = userRepository.createUser(userDocument)

                    if (firestoreResult.isSuccess) {
                        // 4. Enviar email de verificación (opcional)
                        try {
                            user.sendEmailVerification().await()
                        } catch (e: Exception) {
                            // Si falla el envío del email, no es crítico
                        }

                        _registerResult.value = RegisterResult.Success
                    } else {
                        // Si falla la creación en Firestore, eliminar el usuario de Auth
                        user.delete().await()
                        _registerResult.value = RegisterResult.Error(
                            "Error al crear perfil de usuario: ${firestoreResult.exceptionOrNull()?.message}"
                        )
                    }
                } ?: run {
                    _registerResult.value = RegisterResult.Error("Error al crear usuario")
                }

            } catch (e: Exception) {
                _registerResult.value = RegisterResult.Error(getErrorMessage(e))
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun getErrorMessage(exception: Exception): String {
        return when {
            exception.message?.contains("email address is already in use") == true ->
                "Este email ya está registrado"
            exception.message?.contains("password is invalid") == true ->
                "La contraseña no es válida"
            exception.message?.contains("network error") == true ->
                "Error de conexión. Verifica tu internet"
            exception.message?.contains("email address is badly formatted") == true ->
                "El formato del email no es válido"
            exception.message?.contains("weak-password") == true ->
                "La contraseña es muy débil. Debe tener al menos 6 caracteres"
            else -> "Error al crear la cuenta: ${exception.message}"
        }
    }

    private fun isValidName(name: String): Boolean = name.trim().length >= 2

    private fun isValidEmail(email: String): Boolean =
        email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private fun isValidPassword(password: String): Boolean = password.length >= 6

    private fun isValidConfirmPassword(confirmPassword: String, password: String): Boolean =
        confirmPassword == password && confirmPassword.isNotEmpty()

    sealed class RegisterResult {
        object Success : RegisterResult()
        data class Error(val message: String) : RegisterResult()
    }
}