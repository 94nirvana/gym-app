package com.gymapp.minimalist.domain

data class WorkoutPlan(
    val letter: String, // "A", "B", "C", "D", "E"
    val customName: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(letter in listOf("A", "B", "C", "D", "E")) {
            "Workout plan letter must be one of A, B, C, D, E"
        }
        require(customName.length <= 50) {
            "Custom name cannot exceed 50 characters"
        }
    }

    val displayName: String
        get() = if (customName.isNotBlank()) "Treino $letter • $customName" else "Treino $letter"
}
