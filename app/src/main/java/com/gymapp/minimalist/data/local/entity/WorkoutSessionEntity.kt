package com.gymapp.minimalist.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gymapp.minimalist.domain.WorkoutSession

@Entity(
    tableName = "workout_sessions",
    indices = [
        Index(value = ["workout_letter"]),
        Index(value = ["completed_at"])
    ]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "workout_letter")
    val workoutLetter: String,
    @ColumnInfo(name = "workout_name")
    val workoutName: String,
    @ColumnInfo(name = "completed_at")
    val completedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "total_exercises")
    val totalExercises: Int,
    @ColumnInfo(name = "total_completed_sets")
    val totalCompletedSets: Int,
    @ColumnInfo(name = "total_volume_kg")
    val totalVolumeKg: Double,
    @ColumnInfo(name = "estimated_one_rep_max_kg")
    val estimatedOneRepMaxKg: Double
) {
    fun toDomain(): WorkoutSession = WorkoutSession(
        id = id,
        workoutLetter = workoutLetter,
        workoutName = workoutName,
        completedAt = completedAt,
        totalExercises = totalExercises,
        totalCompletedSets = totalCompletedSets,
        totalVolumeKg = totalVolumeKg,
        estimatedOneRepMaxKg = estimatedOneRepMaxKg
    )

    companion object {
        fun fromDomain(session: WorkoutSession): WorkoutSessionEntity = WorkoutSessionEntity(
            id = session.id,
            workoutLetter = session.workoutLetter,
            workoutName = session.workoutName,
            completedAt = session.completedAt,
            totalExercises = session.totalExercises,
            totalCompletedSets = session.totalCompletedSets,
            totalVolumeKg = session.totalVolumeKg,
            estimatedOneRepMaxKg = session.estimatedOneRepMaxKg
        )
    }
}
