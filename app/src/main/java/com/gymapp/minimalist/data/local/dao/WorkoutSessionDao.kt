package com.gymapp.minimalist.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gymapp.minimalist.data.local.entity.WorkoutSessionEntity

@Dao
interface WorkoutSessionDao {

    @Query("SELECT * FROM workout_sessions ORDER BY completed_at DESC")
    suspend fun getAllSessions(): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_sessions WHERE workout_letter = :letter ORDER BY completed_at DESC")
    suspend fun getSessionsByWorkout(letter: String): List<WorkoutSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<WorkoutSessionEntity>): List<Long>

    @Query("DELETE FROM workout_sessions")
    suspend fun clearAll(): Int
}
