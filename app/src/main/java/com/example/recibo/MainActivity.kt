package com.example.recibo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.example.recibo.mainmenu.ui.MainMenuScreen
import com.example.recibo.login.ui.LoginScreen
import com.example.recibo.register.ui.RegisterScreen
import com.example.recibo.ui.theme.ReciBoTheme
import com.example.recibo.qr.ui.ScannerScreen
import com.example.recibo.qr.ui.CreatorScreen
import com.example.recibo.store.ui.StoreScreen
import com.example.recibo.challenge.ui.ChallengeScreen
import com.example.recibo.profile.ui.ProfileScreen

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReciBoTheme {
                val navController = rememberNavController()
                var showBars by remember { mutableStateOf(true) }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                LaunchedEffect(currentRoute) {
                    showBars = currentRoute !in listOf("login", "register", "scanner", "creator")
                }

                Scaffold(
                    topBar = {
                        if (showBars && currentRoute in listOf("mainmenu", "store", "challenge", "profile")) {
                            CenterAlignedTopAppBar(
                                title = { Text("ReciBo") },
                                actions = {
                                    IconButton(onClick = { navController.navigate("store") }) {
                                        Icon(Icons.Default.Store, contentDescription = "Tienda")
                                    }
                                    IconButton(onClick = { navController.navigate("challenge") }) {
                                        Icon(Icons.Default.EmojiEvents, contentDescription = "Desafíos")
                                    }
                                    IconButton(onClick = { navController.navigate("profile") }) {
                                        Icon(Icons.Default.Person, contentDescription = "Perfil")
                                    }
                                }
                            )
                        }
                    },
                    bottomBar = {
                        if (showBars && currentRoute in listOf("mainmenu", "store", "challenge", "profile")) {
                            NavigationBar {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                                    label = { Text("Escanear QR") },
                                    selected = false,
                                    onClick = {
                                        navController.navigate("scanner")
                                    }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                                    label = { Text("Crear QR") },
                                    selected = false,
                                    onClick = {
                                        navController.navigate("creator")
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "login",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("login") {
                            LoginScreen(
                                onNavigateToMainMenu = {
                                    navController.navigate("mainmenu") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onNavigateToRegister = { navController.navigate("register") }
                            )
                        }
                        composable("register") {
                            RegisterScreen { navController.navigate("login") }
                        }
                        composable("mainmenu") { MainMenuScreen() }
                        composable("store") { StoreScreen() }
                        composable("challenge") { ChallengeScreen() }
                        composable("profile") { ProfileScreen() }
                        composable("scanner") {
                            ScannerScreen { navController.popBackStack() }
                        }
                        composable("creator") {
                            CreatorScreen { navController.popBackStack() }
                        }
                    }
                }
            }
        }
    }
}