package com.example.recibo.achievement.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recibo.achievement.data.Achievement
import com.example.recibo.achievement.data.UserAchievement

@Composable
fun AchievementScreen() {
    val viewModel: AchievementViewModel = viewModel()

    val achievements by viewModel.achievements.observeAsState(emptyList())
    val userAchievements by viewModel.userAchievements.observeAsState(emptyList())
    val userPoints by viewModel.userPoints.observeAsState(0)
    val isLoading by viewModel.isLoading.observeAsState(false)
    val claimResult by viewModel.claimResult.observeAsState()

    // Manejar resultados de reclamación
    LaunchedEffect(claimResult) {
        claimResult?.let { result ->
            when (result) {
                is AchievementViewModel.ClaimResult.Success -> {
                    // Logro reclamado exitosamente
                }
                is AchievementViewModel.ClaimResult.Error -> {
                    // Error al reclamar logro
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Logros y Desafíos",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading && achievements.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (achievements.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay logros disponibles",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(achievements) { achievement ->
                    val userAchievement = userAchievements.find { it.achievementId == achievement.id }
                    AchievementCard(
                        achievement = achievement,
                        userAchievement = userAchievement,
                        userPoints = userPoints,
                        onClaimClick = { viewModel.claimAchievement(achievement) }
                    )
                }
            }
        }
    }

    // Mostrar resultado de reclamación
    claimResult?.let { result ->
        LaunchedEffect(result) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearClaimResult()
        }

        when (result) {
            is AchievementViewModel.ClaimResult.Success -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Green.copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = "🎉 ¡Logro reclamado! +${result.rewardPoints} puntos",
                        modifier = Modifier.padding(16.dp),
                        color = Color.Green,
                        textAlign = TextAlign.Center
                    )
                }
            }
            is AchievementViewModel.ClaimResult.Error -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Red.copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = "❌ ${result.message}",
                        modifier = Modifier.padding(16.dp),
                        color = Color.Red,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun AchievementCard(
    achievement: Achievement,
    userAchievement: UserAchievement?,
    userPoints: Int,
    onClaimClick: () -> Unit
) {
    val progress = userAchievement?.currentProgress ?: 0
    val isCompleted = userAchievement?.isCompleted ?: false
    val isClaimed = userAchievement?.claimed ?: false
    val progressPercentage = if (achievement.requiredPoints > 0) {
        (progress.toFloat() / achievement.requiredPoints.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isClaimed -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                isCompleted -> Color(0xFFFFC107).copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = when {
                            isClaimed -> Color(0xFF4CAF50)
                            isCompleted -> Color(0xFFFFC107)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = achievement.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (achievement.reward > 0) {
                    Text(
                        text = "+${achievement.reward} pts",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFFC107)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = achievement.description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Barra de progreso
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Progreso: $progress/${achievement.requiredPoints}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(progressPercentage * 100).toInt()}%",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = progressPercentage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = when {
                        isClaimed -> Color(0xFF4CAF50)
                        isCompleted -> Color(0xFFFFC107)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botón de reclamación
            if (isCompleted && !isClaimed) {
                Button(
                    onClick = onClaimClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFC107),
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = "¡Reclamar Logro!",
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (isClaimed) {
                Text(
                    text = "✅ Logro Completado",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}