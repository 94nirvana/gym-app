package com.gymapp.minimalist

import com.gymapp.minimalist.domain.WorkoutPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WorkoutPlanTest {

    @Test
    fun `creating valid workout plan with custom name`() {
        val plan = WorkoutPlan(letter = "A", customName = "Peito e Tríceps")
        assertEquals("A", plan.letter)
        assertEquals("Peito e Tríceps", plan.customName)
        assertEquals("Treino A • Peito e Tríceps", plan.displayName)
    }

    @Test
    fun `default display name when custom name is empty`() {
        val plan = WorkoutPlan(letter = "B", customName = "")
        assertEquals("Treino B", plan.displayName)
    }

    @Test
    fun `invalid plan letter throws exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            WorkoutPlan(letter = "X", customName = "Inválido")
        }
    }

    @Test
    fun `custom name exceeding 50 characters throws exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            WorkoutPlan(letter = "A", customName = "A".repeat(51))
        }
    }
}
