package com.example.recibo.mainmenu.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.example.recibo.R

@Composable
fun MainMenuScreen() {
    val compositionResult by rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(R.raw.welcome_animation)
    )

    val progress by animateLottieCompositionAsState(
        composition = compositionResult,
        iterations = LottieConstants.IterateForever,
        isPlaying = true,
        speed = 1f,
        restartOnPlay = false
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top // Cambiado de Center a Top
    ) {
        // Espaciado desde arriba
        Spacer(modifier = Modifier.height(80.dp))

        // Animación Lottie
        LottieAnimation(
            composition = compositionResult,
            progress = { progress },
            modifier = Modifier
                .size(200.dp)
                .padding(bottom = 24.dp)
        )

        // Texto de bienvenida
        Text(
            text = "¡Bienvenido a ReciBo!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Green
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tu app para ganar puntos reciclando",
            fontSize = 16.sp,
            color = Color.Green
        )
    }
}