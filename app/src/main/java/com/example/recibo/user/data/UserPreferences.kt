package com.example.recibo.user.data

data class UserPreferences(
    val notificationsEnabled: Boolean = true,
    val darkMode: Boolean = false,
    val language: String = "es",
    val biometricEnabled: Boolean = false
)