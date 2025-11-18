package com.st10028374.vitality_vault.main

data class Workout(
    val activityType: String = "",
    val duration: String = "",
    val intensity: String = "",
    val date: String = "",
    val completionPercentage: Int = 100
)
