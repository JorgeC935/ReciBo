package com.example.recibo.login.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recibo.R

@Composable
fun LoginScreen(onNavigateToMainMenu: () -> Unit, onNavigateToRegister: () -> Unit) {
    val viewModel: LoginViewModel = viewModel()

    Box(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Login(Modifier.align(Alignment.Center), viewModel, onNavigateToMainMenu, onNavigateToRegister)
    }
}

@Composable
fun Login(
    modifier: Modifier,
    viewModel: LoginViewModel,
    onNavigateToMainMenu: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val email by viewModel.email.observeAsState("")
    val password by viewModel.password.observeAsState("")
    val loginEnable by viewModel.loginEnable.observeAsState(false)
    val isLoading by viewModel.isLoading.observeAsState(false)
    val loginResult by viewModel.loginResult.observeAsState()

    LaunchedEffect(loginResult) {
        when (loginResult) {
            is LoginViewModel.LoginResult.Success -> {
                onNavigateToMainMenu()
            }
            is LoginViewModel.LoginResult.EmailNotVerified -> {
            }
            is LoginViewModel.LoginResult.VerificationEmailSent -> {
            }
            is LoginViewModel.LoginResult.Error -> {
            }
            null -> {}
        }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize()) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    } else {
        Column(modifier = modifier) {
            HeaderImage(Modifier.align(Alignment.CenterHorizontally))

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Bienvenido",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Inicia sesión en tu cuenta",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            EmailField(email) { newEmail ->
                viewModel.onLoginChanged(newEmail, password)
            }

            Spacer(modifier = Modifier.height(12.dp))

            PasswordField(password) { newPassword ->
                viewModel.onLoginChanged(email, newPassword)
            }

            Spacer(modifier = Modifier.height(16.dp))

            DontHaveAccount(
                Modifier.align(Alignment.End),
                onNavigateToRegister
            )

            Spacer(modifier = Modifier.height(24.dp))

            LoginButton(loginEnable) {
                viewModel.onLoginSelected()
            }

            loginResult?.let { result ->
                Spacer(modifier = Modifier.height(16.dp))

                when (result) {
                    is LoginViewModel.LoginResult.Error -> {
                        ErrorCard(result.message)
                    }
                    is LoginViewModel.LoginResult.EmailNotVerified -> {
                        EmailNotVerifiedCard {
                            viewModel.resendVerificationEmail()
                        }
                    }
                    is LoginViewModel.LoginResult.VerificationEmailSent -> {
                        SuccessCard("Email de verificación enviado. Revisa tu bandeja de entrada.")
                    }
                    else -> { /* No mostrar nada para Success */ }
                }
            }
        }
    }
}

@Composable
fun HeaderImage(modifier: Modifier) {
    Image(
        painter = painterResource(id = R.drawable.logo_principal),
        contentDescription = "Logo",
        modifier = modifier.size(120.dp)
    )
}

@Composable
fun EmailField(email: String, onTextFieldChanged: (String) -> Unit) {
    OutlinedTextField(
        value = email,
        onValueChange = { onTextFieldChanged(it) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Email") },
        placeholder = { Text("ejemplo@correo.com") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        singleLine = true,
        maxLines = 1
    )
}

@Composable
fun PasswordField(password: String, onTextFieldChanged: (String) -> Unit) {
    OutlinedTextField(
        value = password,
        onValueChange = { onTextFieldChanged(it) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Contraseña") },
        placeholder = { Text("Tu contraseña") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        maxLines = 1
    )
}

@Composable
fun DontHaveAccount(modifier: Modifier, onNavigateToRegister: () -> Unit) {
    Text(
        text = "¿No tienes cuenta? Regístrate",
        modifier = modifier.clickable { onNavigateToRegister() },
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun LoginButton(loginEnable: Boolean, onLoginSelected: () -> Unit) {
    Button(
        onClick = { onLoginSelected() },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        enabled = loginEnable
    ) {
        Text(
            text = "Iniciar Sesión",
            fontSize = 16.sp
        )
    }
}

@Composable
fun ErrorCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Red.copy(alpha = 0.1f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "❌ $message",
            color = Color.Red,
            modifier = Modifier.padding(16.dp),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EmailNotVerifiedCard(onResendEmail: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFC107).copy(alpha = 0.1f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⚠️ Email no verificado",
                color = Color(0xFFF57C00),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Necesitas verificar tu email antes de continuar",
                color = Color(0xFFF57C00),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            TextButton(onClick = onResendEmail) {
                Text("Reenviar email de verificación")
            }
        }
    }
}

@Composable
fun SuccessCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Green.copy(alpha = 0.1f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "✅ $message",
            color = Color.Green,
            modifier = Modifier.padding(16.dp),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}