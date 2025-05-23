package com.example.recibo.login.ui

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recibo.R
import kotlinx.coroutines.launch

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

    val email :String by viewModel.email.observeAsState(initial = "")
    val password :String by viewModel.password.observeAsState(initial = "")
    val loginEnable :Boolean by viewModel.loginEnable.observeAsState(initial = false)
    val isLoading :Boolean by viewModel.isLoading.observeAsState(initial = false)
    val coroutineScope = rememberCoroutineScope()

    if(isLoading){
        Box(Modifier.fillMaxSize()){
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }else{
        Column(modifier = Modifier) {
            Spacer(modifier = Modifier.padding(8.dp))
            HeaderImage(Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.padding(16.dp))
            EmailField(email, {viewModel.onLoginChanged(it, password)})
            Spacer(modifier = Modifier.padding(4.dp))
            PasswordField(password, {viewModel.onLoginChanged(email, it)})
            Spacer(modifier = Modifier.padding(8.dp))
            DontHaveAccount(
                Modifier.align(Alignment.End),
                onNavigateToRegister
            )
            Spacer(modifier = Modifier.padding(16.dp))
            LoginButton(loginEnable) {
                coroutineScope.launch {
                    viewModel.onLoginSelected()
                    onNavigateToMainMenu()
                }
            }
        }
    }
}

@Composable
fun HeaderImage(modifier: Modifier) {
    Image(
        painter = painterResource(id = R.drawable.logo_principal),
        contentDescription = "Header",
        modifier = modifier
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
fun DontHaveAccount(modifier: Modifier, onNavigateToRegister: () -> Unit) {
    Text(
        text = "¿No tienes cuenta?",
        modifier = modifier.clickable { onNavigateToRegister() },
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color.DarkGray
    )
}

@Composable
fun LoginButton(loginEnable: Boolean, onLoginSelected: () -> Unit) {
    Button(
        onClick = { onLoginSelected() },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonColors(
            containerColor = Color.Green,
            disabledContainerColor = Color.LightGray,
            contentColor = Color.Black,
            disabledContentColor = Color.Black
        ),
        enabled = loginEnable
    ) {
        Text(text = "Iniciar Sesión")
    }
}