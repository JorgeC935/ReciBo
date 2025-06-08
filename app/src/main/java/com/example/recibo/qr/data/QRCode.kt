package com.example.recibo.qr.data

import com.google.firebase.Timestamp

data class QRCode(
    val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val points: Int = 0,
    val isActive: Boolean = true,
    val isSingleUse: Boolean = true,
    val createdAt: Timestamp = Timestamp.now(),
    val scannedBy: String? = null,
    val scannedByName: String? = null,
    val scannedAt: Timestamp? = null,
    val qrContent: String = "" // El contenido del QR generado
) {
    constructor() : this("", "", "", 0, true, true, Timestamp.now(), null, null, null, "")
}