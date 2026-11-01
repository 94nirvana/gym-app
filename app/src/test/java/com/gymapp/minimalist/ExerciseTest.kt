package com.gymapp.minimalist

import com.gymapp.minimalist.domain.Exercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseTest {

    @Test
    fun `creating valid exercise succeeds`() {
        val exercise = Exercise(
            id = 1L,
            workoutLetter = "A",
            name = "Supino Reto",
            targetSets = 4,
            completedSets = 2,
            weightKg = 70.0,
            isCompleted = false,
            orderIndex = 0
        )

        assertEquals(1L, exercise.id)
        assertEquals("A", exercise.workoutLetter)
        assertEquals("Supino Reto", exercise.name)
        assertEquals(4, exercise.targetSets)
        assertEquals(2, exercise.completedSets)
        assertEquals(70.0, exercise.weightKg, 0.001)
        assertFalse(exercise.isCompleted)
        assertEquals(0, exercise.orderIndex)
    }

    @Test
    fun `all valid workout letters A through E are accepted`() {
        listOf("A", "B", "C", "D", "E").forEach { letter ->
            val exercise = Exercise(workoutLetter = letter, name = "Teste")
            assertEquals(letter, exercise.workoutLetter)
        }
    }

    @Test
    fun `invalid workout letter throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            Exercise(workoutLetter = "F", name = "Invalido")
        }
        assertThrows(IllegalArgumentException::class.java) {
            Exercise(workoutLetter = "Z", name = "Invalido")
        }
    }

    @Test
    fun `blank exercise name throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            Exercise(workoutLetter = "A", name = "   ")
        }
    }

    @Test
    fun `zero or negative target sets throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            Exercise(workoutLetter = "A", name = "Supino", targetSets = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Exercise(workoutLetter = "A", name = "Supino", targetSets = -1)
        }
    }

    @Test
    fun `negative completed sets throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            Exercise(workoutLetter = "A", name = "Supino", completedSets = -1)
        }
    }

    @Test
    fun `negative weight throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            Exercise(workoutLetter = "A", name = "Supino", weightKg = -5.0)
        }
    }

    @Test
    fun `negative order index throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            Exercise(workoutLetter = "A", name = "Supino", orderIndex = -1)
        }
    }
}
