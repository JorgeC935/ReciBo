package com.example.recibo.register.ui

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegisterViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

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
                // Crear usuario en Firebase Auth
                val authResult = auth.createUserWithEmailAndPassword(currentEmail, currentPassword).await()

                // Actualizar perfil del usuario con el nombre
                val user = authResult.user
                user?.let {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(currentName)
                        .build()

                    it.updateProfile(profileUpdates).await()

                    // Enviar email de verificación (opcional pero recomendado)
                    it.sendEmailVerification().await()
                }

                _registerResult.value = RegisterResult.Success

            } catch (e: Exception) {
                _registerResult.value = RegisterResult.Error(getErrorMessage(e))
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun getErrorMessage(exception: Exception): String {
        return when (exception.message) {
            "The email address is already in use by another account." ->
                "Este email ya está registrado"
            "The given password is invalid." ->
                "La contraseña no es válida"
            "A network error (such as timeout, interrupted connection or unreachable host) has occurred." ->
                "Error de conexión. Verifica tu internet"
            "The email address is badly formatted." ->
                "El formato del email no es válido"
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