// CREAR NUEVO ARCHIVO: com/example/recibo/achievement/ui/AchievementViewModel.kt
package com.example.recibo.achievement.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recibo.achievement.data.Achievement
import com.example.recibo.achievement.data.AchievementRepository
import com.example.recibo.achievement.data.UserAchievement
import com.example.recibo.user.data.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class AchievementViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val achievementRepository = AchievementRepository()
    private val userRepository = UserRepository()

    private val _achievements = MutableLiveData<List<Achievement>>()
    val achievements: LiveData<List<Achievement>> = _achievements

    private val _userAchievements = MutableLiveData<List<UserAchievement>>()
    val userAchievements: LiveData<List<UserAchievement>> = _userAchievements

    private val _userPoints = MutableLiveData<Int>()
    val userPoints: LiveData<Int> = _userPoints

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _claimResult = MutableLiveData<ClaimResult?>()
    val claimResult: LiveData<ClaimResult?> = _claimResult

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true

            val currentUser = auth.currentUser
            if (currentUser != null) {
                try {
                    // Cargar logros disponibles
                    val achievementsResult = achievementRepository.getAllAchievements()
                    if (achievementsResult.isSuccess) {
                        _achievements.value = achievementsResult.getOrNull() ?: emptyList()
                    }

                    // Cargar progreso del usuario
                    val userAchievementsResult = achievementRepository.getUserAchievements(currentUser.uid)
                    if (userAchievementsResult.isSuccess) {
                        _userAchievements.value = userAchievementsResult.getOrNull() ?: emptyList()
                    }

                    // Cargar puntos del usuario
                    val userResult = userRepository.getUser(currentUser.uid)
                    if (userResult.isSuccess) {
                        val user = userResult.getOrNull()
                        _userPoints.value = user?.points ?: 0
                    }
                } catch (e: Exception) {
                    // Manejar error silenciosamente o mostrar mensaje
                }
            }

            _isLoading.value = false
        }
    }

    fun claimAchievement(achievement: Achievement) {
        val currentUser = auth.currentUser ?: return

        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Verificar que el logro puede ser reclamado
                val canClaimResult = achievementRepository.canClaimAchievement(currentUser.uid, achievement.id)
                if (!canClaimResult.isSuccess || canClaimResult.getOrNull() != true) {
                    _claimResult.value = ClaimResult.Error("No se puede reclamar este logro")
                    return@launch
                }

                // Reclamar el logro
                val claimResult = achievementRepository.claimAchievement(currentUser.uid, achievement.id)
                if (!claimResult.isSuccess) {
                    _claimResult.value = ClaimResult.Error("Error al reclamar logro")
                    return@launch
                }

                // Dar puntos de recompensa al usuario
                val userResult = userRepository.getUser(currentUser.uid)
                val user = userResult.getOrNull()
                if (user != null) {
                    val newPoints = user.points + achievement.reward
                    userRepository.updateUserPoints(currentUser.uid, newPoints)
                }

                _claimResult.value = ClaimResult.Success(achievement.reward)
                loadData() // Recargar datos

            } catch (e: Exception) {
                _claimResult.value = ClaimResult.Error("Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearClaimResult() {
        _claimResult.value = null
    }

    // Función para obtener el progreso de un logro específico
    fun getAchievementProgress(achievementId: String): UserAchievement? {
        return _userAchievements.value?.find { it.achievementId == achievementId }
    }

    sealed class ClaimResult {
        data class Success(val rewardPoints: Int) : ClaimResult()
        data class Error(val message: String) : ClaimResult()
    }
}