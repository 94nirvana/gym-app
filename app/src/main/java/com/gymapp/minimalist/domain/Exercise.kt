package com.gymapp.minimalist.domain

data class Exercise(
    val id: Long = 0L,
    val workoutLetter: String, // "A", "B", "C", "D", "E"
    val name: String,
    val targetSets: Int = 3,
    val completedSets: Int = 0,
    val weightKg: Double = 0.0,
    val isCompleted: Boolean = false,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(workoutLetter in listOf("A", "B", "C", "D", "E")) {
            "Workout letter must be one of A, B, C, D, E"
        }
        require(name.isNotBlank()) {
            "Exercise name cannot be blank"
        }
        require(targetSets >= 1) {
            "Target sets must be at least 1"
        }
        require(completedSets >= 0) {
            "Completed sets cannot be negative"
        }
        require(weightKg >= 0.0) {
            "Weight cannot be negative"
        }
        require(orderIndex >= 0) {
            "Order index cannot be negative"
        }
    }
}
