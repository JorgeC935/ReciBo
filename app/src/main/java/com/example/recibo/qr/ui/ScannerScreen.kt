package com.example.recibo.qr.ui

import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.CompoundBarcodeView
import androidx.compose.foundation.text.selection.SelectionContainer

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(onBack: () -> Unit) {
    val viewModel: ScannerViewModel = viewModel()
    val context = LocalContext.current
    val scanResult by viewModel.scanResult.observeAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Estados del ViewModel
    val scannedResult by viewModel.scannedResult.observeAsState("")
    val isScanning by viewModel.isScanning.observeAsState(true)
    val hasResult by viewModel.hasResult.observeAsState(false)

    // Permisos de cámara
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(scanResult) {
        scanResult?.let { result ->
            when (result) {
                is ScannerViewModel.ScanResult.Success -> {
                    // Mostrar mensaje de éxito
                    // Aquí puedes agregar un snackbar o diálogo
                }
                is ScannerViewModel.ScanResult.Error -> {
                    // Mostrar mensaje de error
                    // Aquí puedes agregar un snackbar o diálogo
                }
            }
        }
    }

    LaunchedEffect(scanResult) {
        scanResult?.let { result ->
            when (result) {
                is ScannerViewModel.ScanResult.Success -> {
                    snackbarHostState.showSnackbar(
                        "¡Éxito! Ganaste ${result.pointsEarned} puntos de ${result.creatorName}"
                    )
                }
                is ScannerViewModel.ScanResult.Error -> {
                    snackbarHostState.showSnackbar(result.message)
                }
            }
            viewModel.clearScanResult()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Bar personalizada
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver"
                )
            }
            Text(
                text = "Escáner QR",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            if (hasResult) {
                IconButton(onClick = { viewModel.restartScanning() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Escanear de nuevo"
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            !cameraPermissionState.status.isGranted -> {
                // Sin permisos de cámara
                PermissionDeniedContent(
                    onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                    shouldShowRationale = cameraPermissionState.status.shouldShowRationale
                )
            }

            hasResult -> {
                // Mostrar resultado
                ResultContent(
                    scannedResult = scannedResult,
                    onScanAgain = { viewModel.restartScanning() }
                )
            }

            isScanning -> {
                // Cámara activa para escanear
                CameraPreview(
                    onQrScanned = { result ->
                        viewModel.onQrScanned(result)
                    }
                )
            }
        }
    }
}

@Composable
fun PermissionDeniedContent(
    onRequestPermission: () -> Unit,
    shouldShowRationale: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (shouldShowRationale) {
                "La cámara es necesaria para escanear códigos QR"
            } else {
                "Se requieren permisos de cámara"
            },
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            modifier = Modifier.padding(16.dp)
        )

        Button(
            onClick = onRequestPermission,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Conceder Permisos")
        }
    }
}

@Composable
fun ResultContent(
    scannedResult: String,
    onScanAgain: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Indicador de éxito
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "✅ QR Escaneado Exitosamente",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Resultado
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Contenido del QR:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                SelectionContainer {
                    Text(text = scannedResult, fontSize = 14.sp, modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp), textAlign = TextAlign.Start)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botón para escanear de nuevo
        Button(
            onClick = onScanAgain,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = "Escanear Otro QR",
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Información adicional
        Text(
            text = "💡 El contenido ha sido guardado y está disponible para su uso",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun CameraPreview(onQrScanned: (String) -> Unit) {
    var scanFlag by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scanFlag = false
    }

    Column {
        // Instrucciones
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "📱 Apunta la cámara hacia el código QR",
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
        }

        // Vista de la cámara
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            AndroidView(
                factory = { context ->
                    CompoundBarcodeView(context).apply {
                        val capture = CaptureManager(context as androidx.activity.ComponentActivity, this)
                        capture.initializeFromIntent(context.intent, null)
                        capture.decode()
                        this.setStatusText("")
                        this.decodeContinuous { result ->
                            if (!scanFlag && result.text != null) {
                                scanFlag = true
                                onQrScanned(result.text)
                            }
                        }
                        this.resume()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tips
        Text(
            text = "💡 Mantén el código QR dentro del marco y espera a que se detecte automáticamente",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}