package com.example.recibo

import android.os.Bundle
import android.view.WindowManager
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
import com.example.recibo.ui.theme.ReciBoTheme
import com.example.recibo.mainmenu.ui.MainMenuScreen
import com.example.recibo.login.ui.LoginScreen
import com.example.recibo.register.ui.RegisterScreen
import com.example.recibo.qr.ui.ScannerScreen
import com.example.recibo.qr.ui.CreatorScreen
import com.example.recibo.store.ui.StoreScreen
import com.example.recibo.achievement.ui.AchievementScreen
import com.example.recibo.profile.ui.ProfileScreen
import com.example.recibo.qr.ui.ScanResultScreen



class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            ReciBoTheme {
                val navController = rememberNavController()
                var showBars by remember { mutableStateOf(true) }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                LaunchedEffect(currentRoute) {
                    showBars = when(currentRoute) {
                        "login", "register" -> false
                        "scanner", "creator" -> false // Barras ocultas para pantallas de QR
                        else -> true
                    }
                }

                Scaffold(
                    topBar = {
                        if (showBars && currentRoute in listOf("mainmenu", "store", "achievement", "profile")) {
                            CenterAlignedTopAppBar(
                                title = { Text("ReciBo") },
                                actions = {
                                    IconButton(onClick = { navController.navigate("store") }) {
                                        Icon(Icons.Default.Store, contentDescription = "Tienda")
                                    }
                                    IconButton(onClick = { navController.navigate("achievement") }) {
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
                        if (showBars && currentRoute in listOf("mainmenu", "store", "achievement", "profile")) {
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
                    NavHost(navController = navController, startDestination = "login", modifier = Modifier.padding(innerPadding)) {
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
                        // En tu MainActivity.kt, actualiza esta parte del NavHost:

                        composable("mainmenu") {
                            MainMenuScreen(
                                onNavigateToScanner = { navController.navigate("scanner") },
                                onNavigateToCreator = { navController.navigate("creator") },
                                onNavigateToStore = { navController.navigate("store") },
                                onNavigateToAchievements = { navController.navigate("achievement") },
                                onNavigateToProfile = { navController.navigate("profile") }
                            )
                        }
                        composable("store") { StoreScreen() }
                        composable("achievement") { AchievementScreen() }
                        composable("profile") {
                            ProfileScreen(
                                onNavigateToLogin = {
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("scanner") {
                            ScannerScreen(
                                onNavigateToResult = { pointsEarned, creatorName ->
                                    navController.navigate("scanResult/$pointsEarned/$creatorName")
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("creator") {
                            CreatorScreen { navController.popBackStack() }
                        }
                        composable("scanResult/{pointsEarned}/{creatorName}") { backStackEntry ->
                            val pointsEarned = backStackEntry.arguments?.getString("pointsEarned")?.toIntOrNull() ?: 0
                            val creatorName = backStackEntry.arguments?.getString("creatorName") ?: ""

                            ScanResultScreen(
                                pointsEarned = pointsEarned,
                                creatorName = creatorName,
                                onNavigateToMainMenu = {
                                    navController.navigate("mainmenu") {
                                        popUpTo("mainmenu") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}