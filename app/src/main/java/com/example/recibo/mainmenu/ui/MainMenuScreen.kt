package com.example.recibo.mainmenu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.example.recibo.R

data class AnimatedButton(
    val title: String,
    val subtitle: String,
    val animationRes: Int, // R.raw.your_animation
    val gradientColors: List<Color>,
    val action: () -> Unit
)

@Composable
fun MainMenuScreen(
    onNavigateToScanner: () -> Unit = {},
    onNavigateToCreator: () -> Unit = {},
    onNavigateToStore: () -> Unit = {},
    onNavigateToAchievements: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val animatedButtons = listOf(
        AnimatedButton(
            title = "Escanear QR",
            subtitle = "Escanea códigos QR para ganar puntos",
            animationRes = R.raw.scanner_button, // Cambia por tu animación
            gradientColors = listOf(Color(0xFF6C63FF), Color(0xFF4FACFE)),
            action = onNavigateToScanner
        ),
        AnimatedButton(
            title = "Crear QR",
            subtitle = "Genera códigos QR personalizados",
            animationRes = R.raw.qrbutton, // Cambia por tu animación
            gradientColors = listOf(Color(0xFF667eea), Color(0xFF764ba2)),
            action = onNavigateToCreator
        ),
        AnimatedButton(
            title = "Tienda",
            subtitle = "Intercambia puntos por recompensas",
            animationRes = R.raw.tienda, // Cambia por tu animación
            gradientColors = listOf(Color(0xFFf093fb), Color(0xFFf5576c)),
            action = onNavigateToStore
        ),
        AnimatedButton(
            title = "Desafíos",
            subtitle = "Completa misiones y logros",
            animationRes = R.raw.welcome_animation, // Cambia por tu animación
            gradientColors = listOf(Color(0xFF4facfe), Color(0xFF00f2fe)),
            action = onNavigateToAchievements
        ),
        AnimatedButton(
            title = "Mi Perfil",
            subtitle = "Gestiona tu cuenta y estadísticas",
            animationRes = R.raw.profile_button, // Cambia por tu animación
            gradientColors = listOf(Color(0xFFa8edea), Color(0xFFfed6e3)),
            action = onNavigateToProfile
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Encabezado con animación de bienvenida
        WelcomeHeader()

        Spacer(modifier = Modifier.height(24.dp))

        // Lista de botones animados
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(animatedButtons) { button ->
                AnimatedButtonCard(button = button)
            }
        }
    }
}

@Composable
fun WelcomeHeader() {
    val compositionResult by rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(R.raw.environment)
    )

    val progress by animateLottieCompositionAsState(
        composition = compositionResult,
        iterations = LottieConstants.IterateForever,
        isPlaying = true,
        speed = 0.8f
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LottieAnimation(
                    composition = compositionResult,
                    progress = { progress },
                    modifier = Modifier.size(120.dp)
                )

                Column {
                    Text(
                        text = "¡Bienvenido a ReciBo!",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Tu App para reciclar y ganar",
                        fontSize = 16.sp,
                        color = Color.Green
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedButtonCard(button: AnimatedButton) {
    var isPressed by remember { mutableStateOf(false) }

    val compositionResult by rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(button.animationRes)
    )

    val progress by animateLottieCompositionAsState(
        composition = compositionResult,
        iterations = if (isPressed) 1 else LottieConstants.IterateForever,
        isPlaying = true,
        speed = if (isPressed) 1.5f else 0.7f,
        restartOnPlay = isPressed
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable {
                isPressed = true
                button.action()
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed) 12.dp else 6.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(button.gradientColors)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = button.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = button.subtitle,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                LottieAnimation(
                    composition = compositionResult,
                    progress = { progress },
                    modifier = Modifier.size(60.dp)
                )
            }
        }
    }

    // Reset el estado después de la animación
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(500)
            isPressed = false
        }
    }
}