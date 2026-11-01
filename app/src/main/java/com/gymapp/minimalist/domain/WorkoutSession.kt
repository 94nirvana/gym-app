package com.gymapp.minimalist.domain

data class WorkoutSession(
    val id: Long = 0L,
    val workoutLetter: String,
    val workoutName: String,
    val completedAt: Long = System.currentTimeMillis(),
    val totalExercises: Int,
    val totalCompletedSets: Int,
    val totalVolumeKg: Double,
    val estimatedOneRepMaxKg: Double = 0.0
) {
    init {
        require(workoutLetter in listOf("A", "B", "C", "D", "E")) {
            "Workout letter must be one of A, B, C, D, E"
        }
        require(totalExercises >= 0) { "Total exercises cannot be negative" }
        require(totalCompletedSets >= 0) { "Total completed sets cannot be negative" }
        require(totalVolumeKg >= 0.0) { "Total volume cannot be negative" }
        require(estimatedOneRepMaxKg >= 0.0) { "1RM cannot be negative" }
    }

    companion object {
        /**
         * Calcula o 1RM estimado usando a fórmula consolidada de Epley:
         * 1RM = Weight * (1 + (Reps / 30.0))
         */
        fun calculateEpleyOneRepMax(weightKg: Double, reps: Int): Double {
            if (weightKg <= 0.0 || reps <= 0) return 0.0
            if (reps == 1) return weightKg
            return weightKg * (1.0 + (reps / 30.0))
        }
    }
}
