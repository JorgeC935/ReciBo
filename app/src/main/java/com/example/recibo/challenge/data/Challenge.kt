package com.example.recibo.challenge.data

data class Challenge(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val requiredPoints: Int = 10,
    val imageUrl: String = "",
    val category: String = "",
    val isAvailable: Boolean = true
) {
    // Constructor vacío requerido por Firestore
    constructor() : this("", "", "", 0, "", "", true)
}