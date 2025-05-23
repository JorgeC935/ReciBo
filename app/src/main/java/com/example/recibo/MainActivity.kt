package com.example.recibo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.recibo.mainmenu.ui.MainMenuScreen
import com.example.recibo.login.ui.LoginScreen
import com.example.recibo.register.ui.RegisterScreen
import com.example.recibo.ui.theme.ReciBoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReciBoTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = "login"
                ){
                    composable("login"){
                        LoginScreen(
                            onNavigateToMainMenu = { navController.navigate("mainmenu") },
                            onNavigateToRegister = { navController.navigate("register") }
                        )
                    }
                    composable("mainmenu") {
                        MainMenuScreen()
                    }
                    composable("register") {
                        RegisterScreen(
                            onNavigateToLogin = { navController.navigate("login") }
                        )

                    }
                }
            }
        }
    }
}
