package com.example.recibo.login.ui

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

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
            _loginResult.value = LoginResult.Success
        }
    }

    private fun isEmailVerificationRequired(): Boolean {
        // Puedes cambiar esto a true si quieres requerir verificación de email
        return false
    }

    private fun getErrorMessage(exception: Exception): String {
        return when (exception.message) {
            "There is no user record corresponding to this identifier. The user may have been deleted." ->
                "No existe una cuenta con este email"
            "The password is invalid or the user does not have a password." ->
                "Contraseña incorrecta"
            "The user account has been disabled by an administrator." ->
                "Esta cuenta ha sido deshabilitada"
            "A network error (such as timeout, interrupted connection or unreachable host) has occurred." ->
                "Error de conexión. Verifica tu internet"
            "The email address is badly formatted." ->
                "El formato del email no es válido"
            "Access to this account has been temporarily disabled due to many failed login attempts." ->
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