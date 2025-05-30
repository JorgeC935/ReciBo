package com.example.recibo.login.ui

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.example.recibo.user.data.UserRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()

    private val _email = MutableLiveData<String>()
    val email: LiveData<String> = _email

    private val _password = MutableLiveData<String>()
    val password: LiveData<String> = _password

    private val _loginEnable = MutableLiveData<Boolean>()
    val loginEnable: LiveData<Boolean> = _loginEnable

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    init {
        // Verificar si el usuario ya está logueado
        checkCurrentUser()
    }

    fun onLoginChanged(email: String, password: String) {
        _email.value = email
        _password.value = password
        _loginEnable.value = isValidEmail(email) && isValidPassword(password)
    }

    fun onLoginSelected() {
        val currentEmail = _email.value ?: ""
        val currentPassword = _password.value ?: ""

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Intentar hacer login con Firebase Auth
                val authResult = auth.signInWithEmailAndPassword(currentEmail, currentPassword).await()

                // Verificar si el usuario existe y está autenticado
                authResult.user?.let { user ->
                    if (user.isEmailVerified || !isEmailVerificationRequired()) {
                        // Actualizar último login en Firestore
                        try {
                            userRepository.updateLastLogin(user.uid)
                        } catch (e: Exception) {
                            // Si falla la actualización del último login, no es crítico
                            // Solo continuar con el login
                        }

                        _loginResult.value = LoginResult.Success
                    } else {
                        _loginResult.value = LoginResult.EmailNotVerified
                    }
                } ?: run {
                    _loginResult.value = LoginResult.Error("Error desconocido")
                }

            } catch (e: Exception) {
                _loginResult.value = LoginResult.Error(getErrorMessage(e))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resendVerificationEmail() {
        auth.currentUser?.let { user ->
            viewModelScope.launch {
                try {
                    user.sendEmailVerification().await()
                    _loginResult.value = LoginResult.VerificationEmailSent
                } catch (e: Exception) {
                    _loginResult.value = LoginResult.Error("Error al enviar email de verificación")
                }
            }
        }
    }

    fun logout() {
        auth.signOut()
    }

    private fun checkCurrentUser() {
        val currentUser = auth.currentUser
        if (currentUser != null && (currentUser.isEmailVerified || !isEmailVerificationRequired())) {
            // Actualizar último login si el usuario ya está logueado
            viewModelScope.launch {
                try {
                    userRepository.updateLastLogin(currentUser.uid)
                } catch (e: Exception) {
                    // Error no crítico
                }
            }
            _loginResult.value = LoginResult.Success
        }
    }

    private fun isEmailVerificationRequired(): Boolean {
        // Puedes cambiar esto a true si quieres requerir verificación de email
        return false
    }

    private fun getErrorMessage(exception: Exception): String {
        return when {
            exception.message?.contains("no user record corresponding to this identifier") == true ->
                "No existe una cuenta con este email"
            exception.message?.contains("password is invalid") == true ->
                "Contraseña incorrecta"
            exception.message?.contains("user account has been disabled") == true ->
                "Esta cuenta ha sido deshabilitada"
            exception.message?.contains("network error") == true ->
                "Error de conexión. Verifica tu internet"
            exception.message?.contains("email address is badly formatted") == true ->
                "El formato del email no es válido"
            exception.message?.contains("too many requests") == true ->
                "Cuenta temporalmente bloqueada por muchos intentos fallidos"
            else -> "Error de inicio de sesión: ${exception.message}"
        }
    }

    private fun isValidEmail(email: String): Boolean =
        email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()

    private fun isValidPassword(password: String): Boolean = password.length >= 6

    sealed class LoginResult {
        object Success : LoginResult()
        object EmailNotVerified : LoginResult()
        object VerificationEmailSent : LoginResult()
        data class Error(val message: String) : LoginResult()
    }
}