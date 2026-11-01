package com.gymapp.minimalist.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gymapp.minimalist.domain.WorkoutPlan

@Entity(tableName = "workout_plans")
data class WorkoutPlanEntity(
    @PrimaryKey
    val letter: String,
    @ColumnInfo(name = "custom_name")
    val customName: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): WorkoutPlan = WorkoutPlan(
        letter = letter,
        customName = customName,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(plan: WorkoutPlan): WorkoutPlanEntity = WorkoutPlanEntity(
            letter = plan.letter,
            customName = plan.customName,
            updatedAt = plan.updatedAt
        )
    }
}
