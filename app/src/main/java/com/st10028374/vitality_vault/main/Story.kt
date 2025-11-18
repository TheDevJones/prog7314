package com.st10028374.vitality_vault.main

data class Story(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "",
    val imageUrl: String = "",
    val caption: String = "",
    val timestamp: Long = 0L,
    val expiresAt: Long = 0L,
    val viewers: MutableList<String> = mutableListOf(),
    val workoutData: WorkoutStoryData? = null
)

data class WorkoutStoryData(
    val workoutType: String = "",
    val duration: String = "",
    val distance: String = "",
    val calories: String = "",
    val intensity: String = ""
)

data class UserStories(
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "",
    val stories: List<Story> = emptyList(),
    val hasUnviewed: Boolean = false
)