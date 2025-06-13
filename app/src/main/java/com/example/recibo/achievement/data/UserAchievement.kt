package com.example.recibo.achievement.data

import com.google.firebase.Timestamp

data class UserAchievement(
    val userId: String = "",
    val achievementId: String = "",
    val currentProgress: Int = 0,
    val isCompleted: Boolean = false,
    val completedAt: Timestamp? = null,
    val claimed: Boolean = false,
    val claimedAt: Timestamp? = null
) {
    constructor() : this("", "", 0, false, null, false, null)
}
