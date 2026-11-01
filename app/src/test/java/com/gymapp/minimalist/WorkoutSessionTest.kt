package com.gymapp.minimalist

import com.gymapp.minimalist.domain.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WorkoutSessionTest {

    @Test
    fun `creating valid workout session`() {
        val session = WorkoutSession(
            id = 1L,
            workoutLetter = "A",
            workoutName = "Treino A • Peito",
            completedAt = 1000L,
            totalExercises = 5,
            totalCompletedSets = 18,
            totalVolumeKg = 1450.0,
            estimatedOneRepMaxKg = 90.0
        )

        assertEquals("A", session.workoutLetter)
        assertEquals(5, session.totalExercises)
        assertEquals(18, session.totalCompletedSets)
        assertEquals(1450.0, session.totalVolumeKg, 0.001)
        assertEquals(90.0, session.estimatedOneRepMaxKg, 0.001)
    }

    @Test
    fun `epley formula calculates 1RM accurately`() {
        // 100kg para 1 repetição = 100kg
        val oneRep = WorkoutSession.calculateEpleyOneRepMax(100.0, 1)
        assertEquals(100.0, oneRep, 0.001)

        // 100kg para 10 repetições: 100 * (1 + 10/30) = 133.333kg
        val tenReps = WorkoutSession.calculateEpleyOneRepMax(100.0, 10)
        assertEquals(133.333, tenReps, 0.01)

        // Carga zero ou reps zero retorna 0
        assertEquals(0.0, WorkoutSession.calculateEpleyOneRepMax(0.0, 10), 0.001)
        assertEquals(0.0, WorkoutSession.calculateEpleyOneRepMax(100.0, 0), 0.001)
    }

    @Test
    fun `invalid negative values in session throw exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            WorkoutSession(
                workoutLetter = "A",
                workoutName = "Treino A",
                totalExercises = -1,
                totalCompletedSets = 5,
                totalVolumeKg = 100.0
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            WorkoutSession(
                workoutLetter = "A",
                workoutName = "Treino A",
                totalExercises = 2,
                totalCompletedSets = -1,
                totalVolumeKg = 100.0
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            WorkoutSession(
                workoutLetter = "A",
                workoutName = "Treino A",
                totalExercises = 2,
                totalCompletedSets = 5,
                totalVolumeKg = -10.0
            )
        }
    }
}
