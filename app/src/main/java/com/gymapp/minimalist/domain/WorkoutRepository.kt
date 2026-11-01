package com.gymapp.minimalist.domain

interface WorkoutRepository {
    suspend fun getExercisesForWorkout(workoutLetter: String): List<Exercise>
    suspend fun insertExercise(exercise: Exercise): Long
    suspend fun updateExercise(exercise: Exercise): Boolean
    suspend fun deleteExercise(exerciseId: Long): Boolean
    suspend fun toggleCompleted(exerciseId: Long, isCompleted: Boolean): Boolean
    suspend fun incrementCompletedSets(exerciseId: Long): Boolean
    suspend fun decrementCompletedSets(exerciseId: Long): Boolean
    suspend fun updateWeight(exerciseId: Long, newWeight: Double): Boolean
    suspend fun reorderExercises(workoutLetter: String, exerciseIdsInOrder: List<Long>): Boolean

    // Planos de Treino (Personalização de Fichas)
    suspend fun getWorkoutPlan(letter: String): WorkoutPlan
    suspend fun getAllWorkoutPlans(): List<WorkoutPlan>
    suspend fun updateWorkoutPlan(plan: WorkoutPlan): Boolean

    // Ciclo de Treino (Conclusão e Reset)
    suspend fun completeAndResetWorkout(workoutLetter: String): WorkoutSession?
    suspend fun resetWorkoutCounters(workoutLetter: String): Boolean

    // Histórico de Treinos e Evolução
    suspend fun getAllWorkoutSessions(): List<WorkoutSession>
    suspend fun getSessionsForWorkout(workoutLetter: String): List<WorkoutSession>
    suspend fun clearWorkoutSessions(): Boolean

    // Backup & Restauração (Segurança e Integridade)
    suspend fun exportBackupJson(): String
    suspend fun importBackupJson(jsonString: String): Result<Int>
}
