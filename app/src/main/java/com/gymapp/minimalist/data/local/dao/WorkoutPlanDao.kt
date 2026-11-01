package com.gymapp.minimalist.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gymapp.minimalist.data.local.entity.WorkoutPlanEntity

@Dao
interface WorkoutPlanDao {

    @Query("SELECT * FROM workout_plans WHERE letter = :letter LIMIT 1")
    suspend fun getPlanByLetter(letter: String): WorkoutPlanEntity?

    @Query("SELECT * FROM workout_plans ORDER BY letter ASC")
    suspend fun getAllPlans(): List<WorkoutPlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(plan: WorkoutPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plans: List<WorkoutPlanEntity>)

    @Query("DELETE FROM workout_plans")
    suspend fun clearAll(): Int
}
