package com.example.recibo.user.data

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val points: Int = 0,
    val level: Int = 1,
    val totalReceipts: Int = 0,
    val savedItems: List<String> = emptyList(), // IDs de items guardados
    val achievements: List<String> = emptyList(), // IDs de logros obtenidos
    val createdAt: Timestamp = Timestamp.now(),
    val lastLogin: Timestamp = Timestamp.now(),
    val profileImageUrl: String = "",
    val preferences: UserPreferences = UserPreferences()
)

data class UserPreferences(
    val notificationsEnabled: Boolean = true,
    val darkMode: Boolean = false,
    val language: String = "es"
)