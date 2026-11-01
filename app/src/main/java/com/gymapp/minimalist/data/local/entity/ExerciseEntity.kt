package com.gymapp.minimalist.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gymapp.minimalist.domain.Exercise

@Entity(
    tableName = "exercises",
    indices = [
        Index(value = ["workout_letter", "order_index"]),
        Index(value = ["workout_letter"])
    ]
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "workout_letter")
    val workoutLetter: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "target_sets")
    val targetSets: Int,
    @ColumnInfo(name = "completed_sets")
    val completedSets: Int,
    @ColumnInfo(name = "weight_kg")
    val weightKg: Double,
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean,
    @ColumnInfo(name = "order_index")
    val orderIndex: Int = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Exercise = Exercise(
        id = id,
        workoutLetter = workoutLetter,
        name = name,
        targetSets = targetSets,
        completedSets = completedSets,
        weightKg = weightKg,
        isCompleted = isCompleted,
        orderIndex = orderIndex,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(exercise: Exercise): ExerciseEntity = ExerciseEntity(
            id = exercise.id,
            workoutLetter = exercise.workoutLetter,
            name = exercise.name,
            targetSets = exercise.targetSets,
            completedSets = exercise.completedSets,
            weightKg = exercise.weightKg,
            isCompleted = exercise.isCompleted,
            orderIndex = exercise.orderIndex,
            createdAt = exercise.createdAt
        )
    }
}
