package com.example.recibo.register.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RegisterScreen(onNavigateToLogin: () -> Unit) {
    val viewModel: RegisterViewModel = viewModel()

    Box(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Register(Modifier.align(Alignment.Center), viewModel, onNavigateToLogin)
    }
}

@Composable
fun Register(
    modifier: Modifier,
    viewModel: RegisterViewModel,
    onNavigateToLogin: () -> Unit
) {
    val name by viewModel.name.observeAsState("")
    val email by viewModel.email.observeAsState("")
    val password by viewModel.password.observeAsState("")
    val confirmPassword by viewModel.confirmPassword.observeAsState("")
    val registerEnable by viewModel.registerEnable.observeAsState(false)
    val isLoading by viewModel.isLoading.observeAsState(false)
    val registerResult by viewModel.registerResult.observeAsState()

    // Manejo de resultados del registro
    LaunchedEffect(registerResult) {
        when (registerResult) {
            is RegisterViewModel.RegisterResult.Success -> {
                onNavigateToLogin()
            }
            is RegisterViewModel.RegisterResult.Error -> {
                // El error se muestra en el snackbar más abajo
            }
            null -> { /* No hacer nada */ }
        }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize()) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    } else {
        Column(modifier = modifier) {
            Text(
                text = "Crear Cuenta",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            NameField(name) { newName ->
                viewModel.onRegisterChanged(newName, email, password, confirmPassword)
            }

            Spacer(modifier = Modifier.height(8.dp))

            EmailField(email) { newEmail ->
                viewModel.onRegisterChanged(name, newEmail, password, confirmPassword)
            }

            Spacer(modifier = Modifier.height(8.dp))

            PasswordField(password) { newPassword ->
                viewModel.onRegisterChanged(name, email, newPassword, confirmPassword)
            }

            Spacer(modifier = Modifier.height(8.dp))

            ConfirmPasswordField(confirmPassword) { newConfirmPassword ->
                viewModel.onRegisterChanged(name, email, password, newConfirmPassword)
            }

            Spacer(modifier = Modifier.height(16.dp))

            AlreadyHaveAccount(
                Modifier.align(Alignment.End),
                onNavigateToLogin
            )

            Spacer(modifier = Modifier.height(24.dp))

            RegisterButton(registerEnable) {
                viewModel.onRegisterSelected()
            }

            // Mostrar mensaje de error si existe
            registerResult?.let { result ->
                if (result is RegisterViewModel.RegisterResult.Error) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Red.copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            text = result.message,
                            color = Color.Red,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NameField(name: String, onTextFieldChanged: (String) -> Unit) {
    OutlinedTextField(
        value = name,
        onValueChange = { onTextFieldChanged(it) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Nombre completo") },
        placeholder = { Text("Ingresa tu nombre") },
        singleLine = true,
        maxLines = 1
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
        placeholder = { Text("Mínimo 6 caracteres") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        maxLines = 1
    )
}

@Composable
fun ConfirmPasswordField(confirmPassword: String, onTextFieldChanged: (String) -> Unit) {
    OutlinedTextField(
        value = confirmPassword,
        onValueChange = { onTextFieldChanged(it) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Confirmar Contraseña") },
        placeholder = { Text("Repite tu contraseña") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        maxLines = 1
    )
}

@Composable
fun AlreadyHaveAccount(modifier: Modifier, onNavigateToLogin: () -> Unit) {
    Text(
        text = "¿Ya tienes cuenta? Inicia sesión",
        modifier = modifier.clickable { onNavigateToLogin() },
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun RegisterButton(registerEnable: Boolean, onRegisterSelected: () -> Unit) {
    Button(
        onClick = { onRegisterSelected() },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        enabled = registerEnable
    ) {
        Text(
            text = "Crear Cuenta",
            fontSize = 16.sp
        )
    }
}