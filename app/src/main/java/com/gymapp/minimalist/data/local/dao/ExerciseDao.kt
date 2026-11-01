package com.gymapp.minimalist.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gymapp.minimalist.data.local.entity.ExerciseEntity

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises WHERE workout_letter = :letter ORDER BY order_index ASC, id ASC")
    suspend fun getExercisesForWorkout(letter: String): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    suspend fun getExerciseById(id: Long): ExerciseEntity?

    @Query("SELECT * FROM exercises ORDER BY workout_letter ASC, order_index ASC, id ASC")
    suspend fun getAllExercises(): List<ExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntity>): List<Long>

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity): Int

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteExerciseById(id: Long): Int

    @Query("UPDATE exercises SET is_completed = :isCompleted WHERE id = :id")
    suspend fun updateCompletedStatus(id: Long, isCompleted: Boolean): Int

    @Query("UPDATE exercises SET completed_sets = :sets, is_completed = :isCompleted WHERE id = :id")
    suspend fun updateSetsAndCompletion(id: Long, sets: Int, isCompleted: Boolean): Int

    @Query("UPDATE exercises SET weight_kg = :weight WHERE id = :id")
    suspend fun updateWeight(id: Long, weight: Double): Int

    @Query("UPDATE exercises SET order_index = :newOrder WHERE id = :id")
    suspend fun updateOrderIndex(id: Long, newOrder: Int): Int

    @Query("UPDATE exercises SET completed_sets = 0, is_completed = 0 WHERE workout_letter = :letter")
    suspend fun resetWorkoutCounters(letter: String): Int

    @Query("SELECT MAX(order_index) FROM exercises WHERE workout_letter = :letter")
    suspend fun getMaxOrderIndex(letter: String): Int?

    @Query("DELETE FROM exercises")
    suspend fun clearAll(): Int
}
