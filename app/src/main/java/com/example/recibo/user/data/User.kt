package com.example.recibo.user.data

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val points: Int = 0,
    val totalPointsEarned: Int = 0,
    val itemsPurchased: List<String> = emptyList(),
    val challengesCompleted: List<String> = emptyList(),
    val profileImageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis()
)