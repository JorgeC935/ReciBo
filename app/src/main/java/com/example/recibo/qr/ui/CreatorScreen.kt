package com.example.recibo.qr.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CreatorScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Button(onClick = onBack) {
            Text("Volver")
        }
        Text("Creador QR", modifier = Modifier.fillMaxSize())
    }
}