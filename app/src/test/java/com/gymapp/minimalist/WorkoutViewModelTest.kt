package com.gymapp.minimalist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.gymapp.minimalist.domain.Exercise
import com.gymapp.minimalist.domain.WorkoutPlan
import com.gymapp.minimalist.domain.WorkoutRepository
import com.gymapp.minimalist.domain.WorkoutSession
import com.gymapp.minimalist.ui.WorkoutViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FakeWorkoutRepository : WorkoutRepository {
    private val storage = mutableListOf<Exercise>()
    private val plans = mutableMapOf<String, WorkoutPlan>(
        "A" to WorkoutPlan("A"),
        "B" to WorkoutPlan("B"),
        "C" to WorkoutPlan("C"),
        "D" to WorkoutPlan("D"),
        "E" to WorkoutPlan("E")
    )
    private val sessions = mutableListOf<WorkoutSession>()
    private var idCounter = 1L

    override suspend fun getExercisesForWorkout(workoutLetter: String): List<Exercise> {
        return storage.filter { it.workoutLetter == workoutLetter }
            .sortedWith(compareBy({ it.orderIndex }, { it.id }))
    }

    override suspend fun insertExercise(exercise: Exercise): Long {
        val newId = idCounter++
        val order = storage.filter { it.workoutLetter == exercise.workoutLetter }.size
        storage.add(exercise.copy(id = newId, orderIndex = order))
        return newId
    }

    override suspend fun updateExercise(exercise: Exercise): Boolean {
        val index = storage.indexOfFirst { it.id == exercise.id }
        if (index != -1) {
            storage[index] = exercise
            return true
        }
        return false
    }

    override suspend fun deleteExercise(exerciseId: Long): Boolean {
        return storage.removeAll { it.id == exerciseId }
    }

    override suspend fun toggleCompleted(exerciseId: Long, isCompleted: Boolean): Boolean {
        val index = storage.indexOfFirst { it.id == exerciseId }
        if (index != -1) {
            storage[index] = storage[index].copy(isCompleted = isCompleted)
            return true
        }
        return false
    }

    override suspend fun incrementCompletedSets(exerciseId: Long): Boolean {
        val index = storage.indexOfFirst { it.id == exerciseId }
        if (index != -1) {
            val curr = storage[index]
            val newCompleted = curr.completedSets + 1
            storage[index] = curr.copy(
                completedSets = newCompleted,
                isCompleted = newCompleted >= curr.targetSets
            )
            return true
        }
        return false
    }

    override suspend fun decrementCompletedSets(exerciseId: Long): Boolean {
        val index = storage.indexOfFirst { it.id == exerciseId }
        if (index != -1) {
            val curr = storage[index]
            val newCompleted = (curr.completedSets - 1).coerceAtLeast(0)
            storage[index] = curr.copy(
                completedSets = newCompleted,
                isCompleted = newCompleted >= curr.targetSets
            )
            return true
        }
        return false
    }

    override suspend fun updateWeight(exerciseId: Long, newWeight: Double): Boolean {
        val index = storage.indexOfFirst { it.id == exerciseId }
        if (index != -1) {
            storage[index] = storage[index].copy(weightKg = newWeight.coerceAtLeast(0.0))
            return true
        }
        return false
    }

    override suspend fun reorderExercises(
        workoutLetter: String,
        exerciseIdsInOrder: List<Long>
    ): Boolean {
        exerciseIdsInOrder.forEachIndexed { newIndex, id ->
            val idx = storage.indexOfFirst { it.id == id }
            if (idx != -1) {
                storage[idx] = storage[idx].copy(orderIndex = newIndex)
            }
        }
        return true
    }

    override suspend fun getWorkoutPlan(letter: String): WorkoutPlan {
        return plans[letter] ?: WorkoutPlan(letter)
    }

    override suspend fun getAllWorkoutPlans(): List<WorkoutPlan> {
        return plans.values.toList()
    }

    override suspend fun updateWorkoutPlan(plan: WorkoutPlan): Boolean {
        plans[plan.letter] = plan
        return true
    }

    override suspend fun completeAndResetWorkout(workoutLetter: String): WorkoutSession? {
        val exercises = storage.filter { it.workoutLetter == workoutLetter }
        if (exercises.isEmpty()) return null

        val planName = plans[workoutLetter]?.customName?.ifBlank { null } ?: "Treino $workoutLetter"
        val totalVolume = exercises.sumOf { (if (it.completedSets > 0) it.completedSets else it.targetSets) * it.weightKg }
        val totalSets = exercises.sumOf { if (it.completedSets > 0) it.completedSets else it.targetSets }

        val session = WorkoutSession(
            id = idCounter++,
            workoutLetter = workoutLetter,
            workoutName = planName,
            totalExercises = exercises.size,
            totalCompletedSets = totalSets,
            totalVolumeKg = totalVolume,
            estimatedOneRepMaxKg = 80.0
        )
        sessions.add(session)

        // Reseta os exercícios da ficha
        exercises.forEach { e ->
            val idx = storage.indexOfFirst { it.id == e.id }
            if (idx != -1) {
                storage[idx] = storage[idx].copy(completedSets = 0, isCompleted = false)
            }
        }
        return session
    }

    override suspend fun resetWorkoutCounters(workoutLetter: String): Boolean {
        storage.filter { it.workoutLetter == workoutLetter }.forEach { e ->
            val idx = storage.indexOfFirst { it.id == e.id }
            if (idx != -1) {
                storage[idx] = storage[idx].copy(completedSets = 0, isCompleted = false)
            }
        }
        return true
    }

    override suspend fun getAllWorkoutSessions(): List<WorkoutSession> {
        return sessions.toList()
    }

    override suspend fun getSessionsForWorkout(workoutLetter: String): List<WorkoutSession> {
        return sessions.filter { it.workoutLetter == workoutLetter }
    }

    override suspend fun clearWorkoutSessions(): Boolean {
        sessions.clear()
        return true
    }

    override suspend fun exportBackupJson(): String {
        return "{\"schemaVersion\":1}"
    }

    override suspend fun importBackupJson(jsonString: String): Result<Int> {
        return Result.success(1)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeWorkoutRepository
    private lateinit var viewModel: WorkoutViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeWorkoutRepository()
        viewModel = WorkoutViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial workout selected is A and list is empty`() {
        assertEquals("A", viewModel.selectedWorkout.value)
        assertTrue(viewModel.exercises.value?.isEmpty() == true)
    }

    @Test
    fun `adding exercise updates exercise list`() = runTest {
        viewModel.addExercise("Supino Reto", 4, 70.0)
        testDispatcher.scheduler.advanceUntilIdle()

        val list = viewModel.exercises.value
        assertNotNull(list)
        assertEquals(1, list?.size)
        assertEquals("Supino Reto", list?.first()?.name)
        assertEquals(4, list?.first()?.targetSets)
        assertEquals(70.0, list?.first()?.weightKg ?: 0.0, 0.001)
    }

    @Test
    fun `customizing plan name updates currentPlan LiveData`() = runTest {
        viewModel.updateCurrentPlanName("Peito & Tríceps")
        testDispatcher.scheduler.advanceUntilIdle()

        val plan = viewModel.currentPlan.value
        assertNotNull(plan)
        assertEquals("Peito & Tríceps", plan?.customName)
        assertEquals("Treino A • Peito & Tríceps", plan?.displayName)
    }

    @Test
    fun `reordering exercises changes order in list`() = runTest {
        viewModel.addExercise("Exercício 1", 3, 20.0)
        viewModel.addExercise("Exercício 2", 3, 30.0)
        testDispatcher.scheduler.advanceUntilIdle()

        val initialList = viewModel.exercises.value.orEmpty()
        assertEquals("Exercício 1", initialList[0].name)
        assertEquals("Exercício 2", initialList[1].name)

        val reversed = initialList.reversed()
        viewModel.reorderExercises(reversed)
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedList = viewModel.exercises.value.orEmpty()
        assertEquals("Exercício 2", updatedList[0].name)
        assertEquals("Exercício 1", updatedList[1].name)
    }

    @Test
    fun `completing workout creates session in history and resets completed sets`() = runTest {
        viewModel.addExercise("Agachamento", 4, 100.0)
        testDispatcher.scheduler.advanceUntilIdle()

        val exId = viewModel.exercises.value?.first()?.id ?: 1L
        viewModel.incrementSets(exId)
        viewModel.incrementSets(exId)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.completeWorkout()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verifica que o evento de sessão concluída foi disparado
        val event = viewModel.completedSessionEvent.value
        assertNotNull(event)
        assertEquals("A", event?.workoutLetter)

        // Verifica que os contadores foram resetados para a próxima semana
        val updatedExercises = viewModel.exercises.value.orEmpty()
        assertEquals(0, updatedExercises.first().completedSets)
        assertFalse(updatedExercises.first().isCompleted)
    }

    @Test
    fun `resetting current workout counters resets sets and checkboxes without saving session`() = runTest {
        viewModel.addExercise("Barra Fixa", 3, 0.0)
        testDispatcher.scheduler.advanceUntilIdle()

        val exId = viewModel.exercises.value?.first()?.id ?: 1L
        viewModel.toggleCompleted(exId, true)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.exercises.value?.first()?.isCompleted == true)

        viewModel.resetCurrentWorkoutCounters()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.exercises.value?.first()?.isCompleted == true)
        assertEquals(0, viewModel.exercises.value?.first()?.completedSets)
    }

    @Test
    fun `timer starts, pauses and resets correctly`() {
        viewModel.setTimerDuration(45)
        assertEquals(45, viewModel.timerState.value?.secondsRemaining)
        assertFalse(viewModel.timerState.value?.isRunning == true)

        viewModel.startOrResumeTimer()
        assertTrue(viewModel.timerState.value?.isRunning == true)

        viewModel.pauseTimer()
        assertFalse(viewModel.timerState.value?.isRunning == true)

        viewModel.resetTimer()
        assertEquals(45, viewModel.timerState.value?.secondsRemaining)
    }
}
