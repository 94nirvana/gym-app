package com.gymapp.minimalist.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.gymapp.minimalist.R
import com.gymapp.minimalist.data.RoomWorkoutRepository
import com.gymapp.minimalist.data.local.GymRoomDatabase
import com.gymapp.minimalist.databinding.ActivityMainBinding
import com.gymapp.minimalist.databinding.DialogBackupBinding
import com.gymapp.minimalist.databinding.DialogEditWorkoutTitleBinding
import com.gymapp.minimalist.databinding.DialogExerciseFormBinding
import com.gymapp.minimalist.databinding.DialogHistoryBinding
import com.gymapp.minimalist.domain.Exercise
import com.gymapp.minimalist.domain.WorkoutPlan
import com.gymapp.minimalist.domain.WorkoutSession
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ExerciseAdapter

    private val viewModel: WorkoutViewModel by viewModels {
        val database = GymRoomDatabase.getInstance(applicationContext)
        val repository = RoomWorkoutRepository(database)
        WorkoutViewModel.Factory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupWorkoutChips()
        setupTimerControls()
        setupAddButton()
        setupWorkoutCycleActions()
        setupHeaderActions()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = ExerciseAdapter(
            onToggleCompleted = { exercise, isCompleted ->
                viewModel.toggleCompleted(exercise.id, isCompleted)
            },
            onIncrementSet = { exercise ->
                viewModel.incrementSets(exercise.id)
            },
            onDecrementSet = { exercise ->
                viewModel.decrementSets(exercise.id)
            },
            onIncrementWeight = { exercise ->
                viewModel.updateWeight(exercise.id, exercise.weightKg + 2.5)
            },
            onDecrementWeight = { exercise ->
                val newWeight = (exercise.weightKg - 2.5).coerceAtLeast(0.0)
                viewModel.updateWeight(exercise.id, newWeight)
            },
            onEditExercise = { exercise ->
                showExerciseFormDialog(exercise)
            },
            onDeleteExercise = { exercise ->
                showDeleteConfirmationDialog(exercise)
            },
            onReordered = { reorderedList ->
                viewModel.reorderExercises(reorderedList)
            }
        )

        binding.rvExercises.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        // Reordenação de exercícios via Drag & Drop (ItemTouchHelper)
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                @Suppress("DEPRECATION")
                val fromPos = viewHolder.adapterPosition
                @Suppress("DEPRECATION")
                val toPos = target.adapterPosition
                return adapter.onItemMove(fromPos, toPos)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                adapter.onDragComplete()
            }
        })
        touchHelper.attachToRecyclerView(binding.rvExercises)
    }

    private fun setupWorkoutChips() {
        binding.chipGroupWorkouts.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val selectedLetter = when (checkedIds.first()) {
                R.id.chipWorkoutA -> "A"
                R.id.chipWorkoutB -> "B"
                R.id.chipWorkoutC -> "C"
                R.id.chipWorkoutD -> "D"
                R.id.chipWorkoutE -> "E"
                else -> "A"
            }
            viewModel.selectWorkout(selectedLetter)
        }

        // Suporte a toque longo no chip para renomear
        val chipMap = mapOf(
            binding.chipWorkoutA to "A",
            binding.chipWorkoutB to "B",
            binding.chipWorkoutC to "C",
            binding.chipWorkoutD to "D",
            binding.chipWorkoutE to "E"
        )
        chipMap.forEach { (chip, letter) ->
            chip.setOnLongClickListener {
                showEditWorkoutPlanDialog(letter)
                true
            }
        }
    }

    private fun setupHeaderActions() {
        // Clicar no título/subtítulo para renomear a ficha atual
        binding.layoutWorkoutTitleHeader.setOnClickListener {
            val currentLetter = viewModel.selectedWorkout.value ?: "A"
            showEditWorkoutPlanDialog(currentLetter)
        }

        // Abrir Histórico e Gráficos de Evolução
        binding.btnOpenHistory.setOnClickListener {
            viewModel.loadSessions()
            showHistoryDialog()
        }

        // Abrir Backup e Restauração Local
        binding.btnOpenBackup.setOnClickListener {
            showBackupDialog()
        }
    }

    private fun setupWorkoutCycleActions() {
        // Botão Concluir Treino
        binding.btnCompleteWorkout.setOnClickListener {
            val exercises = viewModel.exercises.value.orEmpty()
            if (exercises.isEmpty()) {
                Toast.makeText(this, "Nenhum exercício cadastrado nesta ficha.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentLetter = viewModel.selectedWorkout.value ?: "A"
            val plan = viewModel.currentPlan.value
            val title = plan?.displayName ?: "Treino $currentLetter"

            MaterialAlertDialogBuilder(this)
                .setTitle("Concluir Treino")
                .setMessage("Deseja salvar a sessão de $title no histórico e resetar os contadores de séries para a próxima semana?")
                .setPositiveButton("Sim, Concluir") { _, _ ->
                    viewModel.completeWorkout()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        // Botão Resetar Treino
        binding.btnResetWorkout.setOnClickListener {
            val currentLetter = viewModel.selectedWorkout.value ?: "A"
            MaterialAlertDialogBuilder(this)
                .setTitle("Resetar Treino")
                .setMessage("Deseja zerar as séries e marcações do Treino $currentLetter sem registrar no histórico?")
                .setPositiveButton("Resetar") { _, _ ->
                    viewModel.resetCurrentWorkoutCounters()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun setupTimerControls() {
        binding.btnPreset30.setOnClickListener { viewModel.setTimerDuration(30) }
        binding.btnPreset45.setOnClickListener { viewModel.setTimerDuration(45) }
        binding.btnPreset60.setOnClickListener { viewModel.setTimerDuration(60) }
        binding.btnPreset90.setOnClickListener { viewModel.setTimerDuration(90) }
        binding.btnPreset120.setOnClickListener { viewModel.setTimerDuration(120) }

        binding.btnStartTimer.setOnClickListener {
            val state = viewModel.timerState.value ?: TimerUiState()
            if (state.isRunning) {
                viewModel.pauseTimer()
            } else {
                viewModel.startOrResumeTimer()
            }
        }

        binding.btnResetTimer.setOnClickListener {
            viewModel.resetTimer()
        }
    }

    private fun setupAddButton() {
        binding.btnAddExercise.setOnClickListener {
            showExerciseFormDialog(null)
        }
    }

    private fun observeViewModel() {
        // Atualiza título da ficha e subtítulo dinâmico
        viewModel.currentPlan.observe(this) { plan ->
            val letter = viewModel.selectedWorkout.value ?: "A"
            if (plan != null) {
                binding.tvCurrentWorkoutSubtitle.text = "${plan.displayName} ✎"
            } else {
                binding.tvCurrentWorkoutSubtitle.text = "Treino $letter ✎"
            }
        }

        // Atualiza textos dos chips com os nomes personalizados
        viewModel.allPlans.observe(this) { plansMap ->
            binding.chipWorkoutA.text = getChipLabel("A", plansMap["A"])
            binding.chipWorkoutB.text = getChipLabel("B", plansMap["B"])
            binding.chipWorkoutC.text = getChipLabel("C", plansMap["C"])
            binding.chipWorkoutD.text = getChipLabel("D", plansMap["D"])
            binding.chipWorkoutE.text = getChipLabel("E", plansMap["E"])
        }

        viewModel.exercises.observe(this) { exercises ->
            adapter.submitList(exercises)
            if (exercises.isNullOrEmpty()) {
                binding.layoutEmptyState.visibility = View.VISIBLE
                binding.rvExercises.visibility = View.GONE
            } else {
                binding.layoutEmptyState.visibility = View.GONE
                binding.rvExercises.visibility = View.VISIBLE
            }
        }

        viewModel.timerState.observe(this) { state ->
            val minutes = state.secondsRemaining / 60
            val seconds = state.secondsRemaining % 60
            binding.tvTimerDisplay.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

            if (state.isRunning) {
                binding.btnStartTimer.text = "Pausar"
                binding.tvTimerDisplay.setTextColor(getColor(R.color.accent))
            } else {
                binding.btnStartTimer.text = if (state.secondsRemaining < state.totalSeconds) "Continuar" else "Iniciar"
                binding.tvTimerDisplay.setTextColor(getColor(R.color.text_primary))
            }

            if (state.isFinished) {
                triggerHapticFeedback()
                Toast.makeText(this, "Tempo de descanso finalizado!", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.completedSessionEvent.observe(this) { session ->
            if (session != null) {
                val volumeStr = if (session.totalVolumeKg % 1.0 == 0.0) {
                    "%.0f kg".format(session.totalVolumeKg)
                } else {
                    "%.1f kg".format(session.totalVolumeKg)
                }
                MaterialAlertDialogBuilder(this)
                    .setTitle("Treino Concluído!")
                    .setMessage("${session.workoutName} finalizado com sucesso!\n\n• Volume Total: $volumeStr\n• Séries Concluídas: ${session.totalCompletedSets}\n• Exercícios: ${session.totalExercises}\n\nFicha preparada para a próxima semana.")
                    .setPositiveButton("Excelente", null)
                    .show()
                viewModel.clearCompletedSessionEvent()
            }
        }

        viewModel.operationMessage.observe(this) { msg ->
            if (!msg.isNullOrBlank()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                viewModel.clearOperationMessage()
            }
        }
    }

    private fun getChipLabel(letter: String, plan: WorkoutPlan?): String {
        val custom = plan?.customName?.trim()
        return if (!custom.isNullOrEmpty()) {
            "$letter • $custom"
        } else {
            "Treino $letter"
        }
    }

    // --- Diálogos Minimalistas ---

    private fun showEditWorkoutPlanDialog(letter: String) {
        val dialogBinding = DialogEditWorkoutTitleBinding.inflate(LayoutInflater.from(this))
        val currentPlan = viewModel.allPlans.value?.get(letter)
        dialogBinding.tvEditTitleHeader.text = "Personalizar Treino $letter"
        dialogBinding.etWorkoutCustomName.setText(currentPlan?.customName ?: "")

        MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Salvar") { _, _ ->
                val newName = dialogBinding.etWorkoutCustomName.text?.toString().orEmpty()
                viewModel.updateCurrentPlanName(newName)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showHistoryDialog() {
        val dialogBinding = DialogHistoryBinding.inflate(LayoutInflater.from(this))
        val historyAdapter = HistorySessionAdapter()

        dialogBinding.rvHistorySessions.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = historyAdapter
        }

        var isVolumeMetric = true

        fun updateChartAndList(sessionsList: List<WorkoutSession>) {
            historyAdapter.submitList(sessionsList)

            val points = sessionsList.reversed().map { session ->
                val value = if (isVolumeMetric) {
                    session.totalVolumeKg.toFloat()
                } else {
                    session.estimatedOneRepMaxKg.toFloat()
                }
                ChartPoint(
                    timestamp = session.completedAt,
                    value = value,
                    label = session.workoutLetter
                )
            }
            dialogBinding.chartProgress.setData(points)
        }

        val alertDialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        viewModel.sessions.observe(this) { list ->
            updateChartAndList(list.orEmpty())
        }

        dialogBinding.btnToggleMetric.setOnClickListener {
            isVolumeMetric = !isVolumeMetric
            dialogBinding.btnToggleMetric.text = if (isVolumeMetric) {
                "Métrica: Volume (kg)"
            } else {
                "Métrica: 1RM Est. (kg)"
            }
            updateChartAndList(viewModel.sessions.value.orEmpty())
        }

        dialogBinding.btnClearHistory.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Limpar Histórico")
                .setMessage("Deseja apagar todo o histórico de treinos registrados?")
                .setPositiveButton("Limpar") { _, _ ->
                    viewModel.clearHistory()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        dialogBinding.btnCloseHistory.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun showBackupDialog() {
        val dialogBinding = DialogBackupBinding.inflate(LayoutInflater.from(this))

        val alertDialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Fechar", null)
            .create()

        dialogBinding.btnExportJson.setOnClickListener {
            viewModel.exportBackup { json ->
                dialogBinding.etBackupPayload.setText(json)
                dialogBinding.tvBackupStatus.text = "✓ Backup gerado com checksum SHA-256 e copiado para a área de transferência."
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("GymTrack Backup", json)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "JSON copiado para a área de transferência!", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBinding.btnImportJson.setOnClickListener {
            val inputJson = dialogBinding.etBackupPayload.text?.toString().orEmpty().trim()
            if (inputJson.isEmpty()) {
                dialogBinding.tvBackupStatus.text = "Erro: Cole o JSON de backup no campo acima para restaurar."
                return@setOnClickListener
            }

            MaterialAlertDialogBuilder(this)
                .setTitle("Confirmar Restauração")
                .setMessage("A restauração substituirá as fichas e histórico atuais pelos dados do backup. Deseja continuar?")
                .setPositiveButton("Restaurar") { _, _ ->
                    viewModel.importBackup(inputJson) { success, message ->
                        if (success) {
                            dialogBinding.tvBackupStatus.text = "✓ $message"
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        } else {
                            dialogBinding.tvBackupStatus.text = "✗ $message"
                            Toast.makeText(this, "Falha: $message", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        alertDialog.show()
    }

    private fun showExerciseFormDialog(exerciseToEdit: Exercise?) {
        val dialogBinding = DialogExerciseFormBinding.inflate(LayoutInflater.from(this))

        if (exerciseToEdit != null) {
            dialogBinding.tvDialogTitle.text = getString(R.string.edit_exercise)
            dialogBinding.etExerciseName.setText(exerciseToEdit.name)
            dialogBinding.etTargetSets.setText(exerciseToEdit.targetSets.toString())
            val weightStr = if (exerciseToEdit.weightKg % 1.0 == 0.0) {
                exerciseToEdit.weightKg.toInt().toString()
            } else {
                exerciseToEdit.weightKg.toString()
            }
            dialogBinding.etWeight.setText(weightStr)
        } else {
            dialogBinding.tvDialogTitle.text = getString(R.string.add_exercise)
            dialogBinding.etTargetSets.setText("3")
            dialogBinding.etWeight.setText("20.0")
        }

        MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = dialogBinding.etExerciseName.text?.toString().orEmpty().trim()
                val targetSets = dialogBinding.etTargetSets.text?.toString()?.toIntOrNull() ?: 3
                val weight = dialogBinding.etWeight.text?.toString()?.toDoubleOrNull() ?: 0.0

                if (name.isEmpty()) {
                    Toast.makeText(this, "Nome do exercício não pode ser vazio", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (exerciseToEdit != null) {
                    viewModel.updateExercise(exerciseToEdit.id, name, targetSets, weight)
                } else {
                    viewModel.addExercise(name, targetSets, weight)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteConfirmationDialog(exercise: Exercise) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete)
            .setMessage("Deseja realmente remover \"${exercise.name}\"?")
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteExercise(exercise.id)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(300)
            }
        } catch (_: Exception) {}
    }
}
