package com.example.recibo.register.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

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

    val name :String by viewModel.name.observeAsState(initial = "")
    val email :String by viewModel.email.observeAsState(initial = "")
    val password :String by viewModel.password.observeAsState(initial = "")
    val confirmPassword :String by viewModel.confirmPassword.observeAsState(initial = "")
    val registerEnable :Boolean by viewModel.registerEnable.observeAsState(initial = false)
    val isLoading :Boolean by viewModel.isLoading.observeAsState(initial = false)
    val coroutineScope = rememberCoroutineScope()

    if(isLoading){
        Box(Modifier.fillMaxSize()){
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }else{
        Column(modifier = Modifier) {

            Spacer(modifier = Modifier.padding(16.dp))
            NameField(name, {viewModel.onRegisterChanged(it, email, password, confirmPassword)})
            Spacer(modifier = Modifier.padding(4.dp))
            EmailField(email, {viewModel.onRegisterChanged(name, it, password, confirmPassword)})
            Spacer(modifier = Modifier.padding(4.dp))
            PasswordField(password, {viewModel.onRegisterChanged(name, email, it, confirmPassword)})
            Spacer(modifier = Modifier.padding(4.dp))
            ConfirmPasswordField(confirmPassword, {viewModel.onRegisterChanged(name, email, password, it)})
            Spacer(modifier = Modifier.padding(8.dp))
            AlreadyHaveAccount(
                Modifier.align(Alignment.End),
                onNavigateToLogin
            )
            Spacer(modifier = Modifier.padding(16.dp))
            RegisterButton(registerEnable) {
                coroutineScope.launch {
                    viewModel.onLoginSelected()
                    onNavigateToLogin()
                }
            }
        }
    }
}

@Composable
fun NameField(name: String, onTextFieldChanged: (String) -> Unit) {
    TextField(
        value = name,
        onValueChange = { onTextFieldChanged(it) } ,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(text = "Name") },
        singleLine = true,
        maxLines = 1,
        colors = TextFieldDefaults.colors(
            focusedTextColor = Color.DarkGray,
            focusedContainerColor = Color.LightGray,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
fun EmailField(email: String, onTextFieldChanged:(String) -> Unit) {
    TextField(
        value = email,
        onValueChange = { onTextFieldChanged(it) } ,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(text = "Email") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        singleLine = true,
        maxLines = 1,
        colors = TextFieldDefaults.colors(
            focusedTextColor = Color.DarkGray,
            focusedContainerColor = Color.LightGray,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
fun PasswordField(password: String, onTextFieldChanged:(String) -> Unit) {
    TextField(
        value = password,
        onValueChange = { onTextFieldChanged(it) },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(text = "Contraseña") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        maxLines = 1,
        colors = TextFieldDefaults.colors(
            focusedTextColor = Color.DarkGray,
            focusedContainerColor = Color.LightGray,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
fun ConfirmPasswordField(confirmPassword: String, onTextFieldChanged: (String) -> Unit) {
    TextField(
        value = confirmPassword,
        onValueChange = { onTextFieldChanged(it) },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(text = "Confirmar Contraseña") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        maxLines = 1,
        colors = TextFieldDefaults.colors(
            focusedTextColor = Color.DarkGray,
            focusedContainerColor = Color.LightGray,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
fun AlreadyHaveAccount(modifier: Modifier, onNavigateToLogin: () -> Unit) {
    Text(
        text = "Olvidaste la contraseña?",
        modifier = modifier.clickable { onNavigateToLogin() },
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color.DarkGray
    )
}

@Composable
fun RegisterButton(registerEnable: Boolean, onRegisterSelected: () -> Unit) {
    Button(
        onClick = { onRegisterSelected() },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonColors(
            containerColor = Color.Green,
            disabledContainerColor = Color.LightGray,
            contentColor = Color.Black,
            disabledContentColor = Color.Black
        ),
        enabled = registerEnable
    ) {
        Text(text = "Iniciar Sesión")
    }
}