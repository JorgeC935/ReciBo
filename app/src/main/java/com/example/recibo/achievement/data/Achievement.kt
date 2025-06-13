package com.example.recibo.achievement.data

import com.google.firebase.Timestamp

data class Achievement(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val iconUrl: String = "",
    val requiredPoints: Int = 0,
    val category: String = "", // "scanner", "creator", "points", "social"
    val isActive: Boolean = true,
    val reward: Int = 0, // Puntos que se obtienen al completar el logro
    val createdAt: Timestamp = Timestamp.now()
) {
    constructor() : this("", "", "", "", 0, "", true, 0, Timestamp.now())
}