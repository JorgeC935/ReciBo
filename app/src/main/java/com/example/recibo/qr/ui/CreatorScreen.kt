package com.example.recibo.qr.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recibo.qr.data.QRCode
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorScreen(onBack: () -> Unit) {
    val viewModel: CreatorViewModel = viewModel()

    val userPoints by viewModel.userPoints.observeAsState(0)
    val isLoading by viewModel.isLoading.observeAsState(false)
    val error by viewModel.error.observeAsState()
    val success by viewModel.success.observeAsState()
    val generatedQR by viewModel.generatedQR.observeAsState()
    val userQRCodes by viewModel.userQRCodes.observeAsState(emptyList())

    val snackbarHostState = remember { SnackbarHostState() }

    // Manejar mensajes
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(success) {
        success?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                }
                Text(
                    text = "Crear QR",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (generatedQR != null) {
                // Mostrar QR generado
                GeneratedQRView(
                    generatedQR = generatedQR!!,
                    onBack = { viewModel.clearGeneratedQR() }
                )
            } else {
                // Mostrar formulario y lista de QRs
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        // Puntos disponibles
                        UserPointsCard(userPoints = userPoints)
                    }

                    item {
                        // Formulario de creación
                        CreateQRForm(
                            userPoints = userPoints,
                            isLoading = isLoading,
                            onCreateQR = { points, isSingleUse ->
                                viewModel.generateQR(points, isSingleUse)
                            }
                        )
                    }

                    item {
                        Text(
                            text = "Mis QRs Creados",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    if (userQRCodes.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No has creado ningún QR aún",
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(userQRCodes) { qrCode ->
                            QRCodeCard(
                                qrCode = qrCode,
                                onCancel = { viewModel.cancelQR(qrCode) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserPointsCard(userPoints: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Puntos Disponibles:",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Stars,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = userPoints.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun CreateQRForm(
    userPoints: Int,
    isLoading: Boolean,
    onCreateQR: (Int, Boolean) -> Unit
) {
    var points by remember { mutableStateOf("") }
    var isSingleUse by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Crear Nuevo QR",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = points,
                onValueChange = { points = it },
                label = { Text("Puntos a asignar") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Uso único:")
                Switch(
                    checked = isSingleUse,
                    onCheckedChange = { isSingleUse = it },
                    enabled = !isLoading
                )
            }

            if (!isSingleUse) {
                Text(
                    text = "QR de uso múltiple: Podrá ser escaneado varias veces hasta que lo desactives",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val pointsInt = points.toIntOrNull()
                    if (pointsInt != null && pointsInt > 0) {
                        onCreateQR(pointsInt, isSingleUse)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && points.toIntOrNull() != null &&
                        (points.toIntOrNull() ?: 0) > 0 &&
                        (points.toIntOrNull() ?: 0) <= userPoints
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Generar QR")
            }
        }
    }
}

@Composable
fun QRCodeCard(
    qrCode: QRCode,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Stars,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${qrCode.points} puntos",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (qrCode.isSingleUse) "Uso único" else "Uso múltiple",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reemplaza la lógica de estado del QR con:
                    val statusColor = when {
                        !qrCode.isActive -> Color.Red
                        qrCode.scannedBy != null && qrCode.isSingleUse -> Color.Gray
                        qrCode.scannedBy != null && !qrCode.isSingleUse -> Color.Green
                        else -> Color.Blue
                    }

                    val statusText = when {
                        !qrCode.isActive -> "Cancelado"
                        qrCode.scannedBy != null && qrCode.isSingleUse -> "Usado"
                        qrCode.scannedBy != null && !qrCode.isSingleUse -> "En uso múltiple"
                        else -> "Activo"
                    }

                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (qrCode.isActive && qrCode.scannedBy == null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Cancel,
                                contentDescription = "Cancelar QR",
                                tint = Color.Red,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (qrCode.scannedBy != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Escaneado por: ${qrCode.scannedByName}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                qrCode.scannedAt?.let { scannedAt ->
                    Text(
                        text = "Fecha: ${formatDate(scannedAt.toDate())}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Creado: ${formatDate(qrCode.createdAt.toDate())}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun GeneratedQRView(
    generatedQR: CreatorViewModel.GeneratedQRResult,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
            )
        ) {
            Text(
                text = "✅ QR Generado Exitosamente",
                modifier = Modifier.padding(16.dp),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Comparte este QR",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Image(
                    bitmap = generatedQR.bitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier.size(300.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "${generatedQR.qrCode.points} puntos",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = if (generatedQR.qrCode.isSingleUse) "Uso único" else "Uso múltiple",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Crear Otro QR")
        }

        Spacer(modifier = Modifier.height(16.dp))

// Información adicional
        Text(
            text = "💡 Comparte este QR para que otros usuarios puedan escanearlo y obtener puntos",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return formatter.format(date)
}
