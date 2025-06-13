package com.example.recibo.store.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.recibo.R
import com.example.recibo.store.data.StoreItem

@Composable
fun StoreScreen() {

    val compositionResult by rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(R.raw.tienda)
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
        Spacer(modifier = Modifier.height(270.dp))

        // Animación Lottie
        LottieAnimation(
            composition = compositionResult,
            progress = { progress },
            modifier = Modifier
                .size(200.dp)
                .padding(bottom = 24.dp)
        )


    }

    val viewModel: StoreViewModel = viewModel()

    val storeItems by viewModel.storeItems.observeAsState(emptyList())
    val userPoints by viewModel.userPoints.observeAsState(0)
    val isLoading by viewModel.isLoading.observeAsState(false)
    val purchaseResult by viewModel.purchaseResult.observeAsState()
    val purchaseConfirmation by viewModel.purchaseConfirmation.observeAsState()
    val currentUser by viewModel.currentUser.observeAsState()

    // Manejar resultados de compra
    LaunchedEffect(purchaseResult) {
        purchaseResult?.let { result ->
            // Aquí podrías mostrar un Snackbar o Toast
            when (result) {
                is StoreViewModel.PurchaseResult.Success -> {
                    // Compra exitosa
                }
                is StoreViewModel.PurchaseResult.InsufficientPoints -> {
                    // Puntos insuficientes
                }
                is StoreViewModel.PurchaseResult.Error -> {
                    // Error en la compra
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header con puntos del usuario
        UserPointsHeader(userPoints = userPoints)

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading && storeItems.isEmpty()) {
            // Loading inicial
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (storeItems.isEmpty()) {
            // No hay items
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay artículos disponibles en la tienda",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Lista de items
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(storeItems) { item ->
                    StoreItemCard(
                        item = item,
                        userPoints = userPoints,
                        itemsPurchased = currentUser?.itemsPurchased ?: emptyList(), // AGREGAR esta línea
                        onPurchaseClick = { viewModel.showPurchaseConfirmation(item) }
                    )
                }
            }
        }
    }

    // Diálogo de confirmación de compra
    purchaseConfirmation?.let { item ->
        PurchaseConfirmationDialog(
            item = item,
            userPoints = userPoints,
            onConfirm = {
                viewModel.purchaseItem(item)
            },
            onDismiss = { viewModel.hidePurchaseConfirmation() }
        )
    }

    // Mostrar resultado de compra
    purchaseResult?.let { result ->
        LaunchedEffect(result) {
            // Limpiar el resultado después de un tiempo
            kotlinx.coroutines.delay(3000)
            viewModel.clearPurchaseResult()
        }

        when (result) {
            is StoreViewModel.PurchaseResult.Success -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Green.copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = "✅ ¡${result.itemName} comprado exitosamente!",
                        modifier = Modifier.padding(16.dp),
                        color = Color.Green,
                        textAlign = TextAlign.Center
                    )
                }
            }
            is StoreViewModel.PurchaseResult.InsufficientPoints -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Red.copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = "❌ No tienes suficientes puntos",
                        modifier = Modifier.padding(16.dp),
                        color = Color.Red,
                        textAlign = TextAlign.Center
                    )
                }
            }
            is StoreViewModel.PurchaseResult.Error -> {
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
fun UserPointsHeader(userPoints: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tus Puntos:",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Stars,
                    contentDescription = "Puntos",
                    tint = Color(0xFFFFD700), // Dorado
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = userPoints.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun StoreItemCard(
    item: StoreItem,
    userPoints: Int,
    itemsPurchased: List<String> = emptyList(),
    onPurchaseClick: () -> Unit
) {
    val canAfford = userPoints >= item.price
    val alreadyPurchased = itemsPurchased.contains(item.id)
    val missingPoints = if (!canAfford) item.price - userPoints else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Nombre del item
            Text(
                text = item.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Descripción
            Text(
                text = item.description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Precio y botón
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Stars,
                        contentDescription = "Precio",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${item.price} puntos",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Botón de compra
                Button(
                    onClick = onPurchaseClick,
                    enabled = canAfford && !alreadyPurchased && item.stock != 0, // MODIFICAR esta línea
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            alreadyPurchased -> Color.Gray
                            canAfford -> Color(0xFFFFC107)
                            else -> Color.Gray
                        },
                        contentColor = if (canAfford && !alreadyPurchased) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when {
                            alreadyPurchased -> "Comprado"
                            item.stock == 0 -> "Sin Stock"
                            canAfford -> "Comprar"
                            else -> "Faltan $missingPoints pts"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PurchaseConfirmationDialog(
    item: StoreItem,
    userPoints: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Confirmar Compra",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text("¿Estás seguro de que quieres comprar:")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Por ${item.price} puntos?",
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Puntos restantes: ${userPoints - item.price}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFC107),
                    contentColor = Color.Black
                )
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}