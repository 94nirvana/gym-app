package com.gymapp.minimalist.ui

import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gymapp.minimalist.domain.Exercise
import com.gymapp.minimalist.domain.WorkoutPlan
import com.gymapp.minimalist.domain.WorkoutRepository
import com.gymapp.minimalist.domain.WorkoutSession
import kotlinx.coroutines.launch

data class TimerUiState(
    val totalSeconds: Int = 60,
    val secondsRemaining: Int = 60,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false
)

class WorkoutViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _selectedWorkout = MutableLiveData("A")
    val selectedWorkout: LiveData<String> = _selectedWorkout

    private val _currentPlan = MutableLiveData<WorkoutPlan>()
    val currentPlan: LiveData<WorkoutPlan> = _currentPlan

    private val _allPlans = MutableLiveData<Map<String, WorkoutPlan>>()
    val allPlans: LiveData<Map<String, WorkoutPlan>> = _allPlans

    private val _exercises = MutableLiveData<List<Exercise>>(emptyList())
    val exercises: LiveData<List<Exercise>> = _exercises

    private val _sessions = MutableLiveData<List<WorkoutSession>>(emptyList())
    val sessions: LiveData<List<WorkoutSession>> = _sessions

    private val _completedSessionEvent = MutableLiveData<WorkoutSession?>()
    val completedSessionEvent: LiveData<WorkoutSession?> = _completedSessionEvent

    private val _timerState = MutableLiveData(TimerUiState())
    val timerState: LiveData<TimerUiState> = _timerState

    private val _operationMessage = MutableLiveData<String?>()
    val operationMessage: LiveData<String?> = _operationMessage

    private var countDownTimer: CountDownTimer? = null

    init {
        loadPlans()
        loadExercises("A")
    }

    fun selectWorkout(letter: String) {
        if (letter in listOf("A", "B", "C", "D", "E")) {
            _selectedWorkout.value = letter
            loadPlanForLetter(letter)
            loadExercises(letter)
        }
    }

    fun loadPlans() {
        viewModelScope.launch {
            val plansList = repository.getAllWorkoutPlans()
            val map = plansList.associateBy { it.letter }
            _allPlans.postValue(map)
            val currentLetter = _selectedWorkout.value ?: "A"
            _currentPlan.postValue(map[currentLetter] ?: WorkoutPlan(currentLetter))
        }
    }

    private fun loadPlanForLetter(letter: String) {
        viewModelScope.launch {
            val plan = repository.getWorkoutPlan(letter)
            _currentPlan.postValue(plan)
        }
    }

    fun updateCurrentPlanName(customName: String) {
        val letter = _selectedWorkout.value ?: "A"
        viewModelScope.launch {
            val newPlan = WorkoutPlan(letter = letter, customName = customName.trim())
            repository.updateWorkoutPlan(newPlan)
            loadPlans()
        }
    }

    fun loadExercises(letter: String = _selectedWorkout.value ?: "A") {
        viewModelScope.launch {
            val list = repository.getExercisesForWorkout(letter)
            _exercises.postValue(list)
        }
    }

    fun addExercise(name: String, targetSets: Int, weightKg: Double) {
        val currentLetter = _selectedWorkout.value ?: "A"
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            val exercise = Exercise(
                workoutLetter = currentLetter,
                name = trimmed,
                targetSets = targetSets.coerceAtLeast(1),
                completedSets = 0,
                weightKg = weightKg.coerceAtLeast(0.0),
                isCompleted = false
            )
            repository.insertExercise(exercise)
            loadExercises(currentLetter)
        }
    }

    fun updateExercise(id: Long, name: String, targetSets: Int, weightKg: Double) {
        val current = _exercises.value?.find { it.id == id } ?: return
        val updated = current.copy(
            name = name.trim().ifEmpty { current.name },
            targetSets = targetSets.coerceAtLeast(1),
            weightKg = weightKg.coerceAtLeast(0.0)
        )
        viewModelScope.launch {
            repository.updateExercise(updated)
            loadExercises()
        }
    }

    fun deleteExercise(id: Long) {
        viewModelScope.launch {
            repository.deleteExercise(id)
            loadExercises()
        }
    }

    fun toggleCompleted(id: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleCompleted(id, isCompleted)
            loadExercises()
        }
    }

    fun incrementSets(id: Long) {
        viewModelScope.launch {
            repository.incrementCompletedSets(id)
            loadExercises()
        }
    }

    fun decrementSets(id: Long) {
        viewModelScope.launch {
            repository.decrementCompletedSets(id)
            loadExercises()
        }
    }

    fun updateWeight(id: Long, newWeight: Double) {
        viewModelScope.launch {
            repository.updateWeight(id, newWeight)
            loadExercises()
        }
    }

    fun reorderExercises(reorderedList: List<Exercise>) {
        val letter = _selectedWorkout.value ?: "A"
        _exercises.value = reorderedList
        viewModelScope.launch {
            val idsInOrder = reorderedList.map { it.id }
            repository.reorderExercises(letter, idsInOrder)
            loadExercises(letter)
        }
    }

    // --- Ciclo de Treino: Concluir e Resetar ---

    fun completeWorkout() {
        val letter = _selectedWorkout.value ?: "A"
        viewModelScope.launch {
            val session = repository.completeAndResetWorkout(letter)
            if (session != null) {
                _completedSessionEvent.postValue(session)
                loadExercises(letter)
            } else {
                _operationMessage.postValue("Nenhum exercício encontrado para concluir este treino.")
            }
        }
    }

    fun clearCompletedSessionEvent() {
        _completedSessionEvent.value = null
    }

    fun resetCurrentWorkoutCounters() {
        val letter = _selectedWorkout.value ?: "A"
        viewModelScope.launch {
            repository.resetWorkoutCounters(letter)
            loadExercises(letter)
            _operationMessage.postValue("Contadores do Treino $letter resetados!")
        }
    }

    // --- Histórico & Gráfico de Evolução ---

    fun loadSessions() {
        viewModelScope.launch {
            val list = repository.getAllWorkoutSessions()
            _sessions.postValue(list)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearWorkoutSessions()
            loadSessions()
            _operationMessage.postValue("Histórico de treinos limpo com sucesso.")
        }
    }

    // --- Backup & Restauração ---

    fun exportBackup(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportBackupJson()
            onResult(json)
        }
    }

    fun importBackup(jsonString: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.importBackupJson(jsonString)
            result.fold(
                onSuccess = { count ->
                    loadPlans()
                    loadExercises()
                    loadSessions()
                    onResult(true, "Backup restaurado com sucesso! ($count itens processados)")
                },
                onFailure = { error ->
                    onResult(false, error.message ?: "Erro desconhecido ao restaurar backup")
                }
            )
        }
    }

    fun clearOperationMessage() {
        _operationMessage.value = null
    }

    // --- Cronômetro de Descanso ---

    fun setTimerDuration(seconds: Int) {
        countDownTimer?.cancel()
        val validSecs = seconds.coerceAtLeast(5)
        _timerState.value = TimerUiState(
            totalSeconds = validSecs,
            secondsRemaining = validSecs,
            isRunning = false,
            isFinished = false
        )
    }

    fun startOrResumeTimer() {
        val current = _timerState.value ?: TimerUiState()
        if (current.isRunning) return

        val remainingMillis = if (current.secondsRemaining <= 0) {
            current.totalSeconds * 1000L
        } else {
            current.secondsRemaining * 1000L
        }

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(remainingMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secs = (millisUntilFinished / 1000).toInt()
                _timerState.postValue(
                    current.copy(
                        secondsRemaining = secs,
                        isRunning = true,
                        isFinished = false
                    )
                )
            }

            override fun onFinish() {
                _timerState.postValue(
                    current.copy(
                        secondsRemaining = 0,
                        isRunning = false,
                        isFinished = true
                    )
                )
            }
        }.start()

        _timerState.value = current.copy(isRunning = true, isFinished = false)
    }

    fun pauseTimer() {
        countDownTimer?.cancel()
        val current = _timerState.value ?: TimerUiState()
        _timerState.value = current.copy(isRunning = false)
    }

    fun resetTimer() {
        countDownTimer?.cancel()
        val current = _timerState.value ?: TimerUiState()
        _timerState.value = current.copy(
            secondsRemaining = current.totalSeconds,
            isRunning = false,
            isFinished = false
        )
    }

    override fun onCleared() {
        super.onCleared()
        countDownTimer?.cancel()
    }

    class Factory(private val repository: WorkoutRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WorkoutViewModel::class.java)) {
                return WorkoutViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
