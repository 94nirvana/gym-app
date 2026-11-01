package com.gymapp.minimalist.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gymapp.minimalist.R
import com.gymapp.minimalist.databinding.ItemExerciseBinding
import com.gymapp.minimalist.domain.Exercise
import java.util.Collections
import java.util.Locale

class ExerciseAdapter(
    private val onToggleCompleted: (Exercise, Boolean) -> Unit,
    private val onIncrementSet: (Exercise) -> Unit,
    private val onDecrementSet: (Exercise) -> Unit,
    private val onIncrementWeight: (Exercise) -> Unit,
    private val onDecrementWeight: (Exercise) -> Unit,
    private val onEditExercise: (Exercise) -> Unit,
    private val onDeleteExercise: (Exercise) -> Unit,
    private val onReordered: (List<Exercise>) -> Unit = {}
) : ListAdapter<Exercise, ExerciseAdapter.ExerciseViewHolder>(ExerciseDiffCallback()) {

    // Lista mutável interna para permitir animação contínua durante Drag & Drop
    private val currentItems = mutableListOf<Exercise>()

    override fun submitList(list: List<Exercise>?) {
        currentItems.clear()
        if (list != null) {
            currentItems.addAll(list)
        }
        super.submitList(list)
    }

    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(currentItems, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(currentItems, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    fun onDragComplete() {
        onReordered(currentItems.toList())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExerciseViewHolder {
        val binding = ItemExerciseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExerciseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExerciseViewHolder, position: Int) {
        val item = if (position in currentItems.indices) currentItems[position] else getItem(position)
        holder.bind(item)
    }

    inner class ExerciseViewHolder(
        private val binding: ItemExerciseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(exercise: Exercise) {
            binding.tvExerciseName.text = exercise.name

            // Efeito visual de tachado quando completado
            if (exercise.isCompleted) {
                binding.tvExerciseName.paintFlags =
                    binding.tvExerciseName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvExerciseName.alpha = 0.5f
            } else {
                binding.tvExerciseName.paintFlags =
                    binding.tvExerciseName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvExerciseName.alpha = 1.0f
            }

            // Desvincula listener temporariamente para evitar disparo cíclico
            binding.cbCompleted.setOnCheckedChangeListener(null)
            binding.cbCompleted.isChecked = exercise.isCompleted
            binding.cbCompleted.setOnCheckedChangeListener { _, isChecked ->
                onToggleCompleted(exercise, isChecked)
            }

            // Formatação de Séries
            binding.tvSets.text = String.format(
                Locale.getDefault(),
                "%d / %d Séries",
                exercise.completedSets,
                exercise.targetSets
            )

            // Destaque quando atinge a meta de séries
            if (exercise.completedSets >= exercise.targetSets) {
                binding.tvSets.setTextColor(
                    binding.root.context.getColor(R.color.accent)
                )
            } else {
                binding.tvSets.setTextColor(
                    binding.root.context.getColor(R.color.text_primary)
                )
            }

            // Formatação de Carga
            val weightFormatted = if (exercise.weightKg % 1.0 == 0.0) {
                String.format(Locale.getDefault(), "%.0f kg", exercise.weightKg)
            } else {
                String.format(Locale.getDefault(), "%.1f kg", exercise.weightKg)
            }
            binding.tvWeight.text = weightFormatted

            // Ações de Botões Rápidos
            binding.btnIncrementSet.setOnClickListener { onIncrementSet(exercise) }
            binding.btnDecrementSet.setOnClickListener { onDecrementSet(exercise) }
            binding.btnIncrementWeight.setOnClickListener { onIncrementWeight(exercise) }
            binding.btnDecrementWeight.setOnClickListener { onDecrementWeight(exercise) }

            binding.btnEditExercise.setOnClickListener { onEditExercise(exercise) }
            binding.btnDeleteExercise.setOnClickListener { onDeleteExercise(exercise) }
        }
    }

    class ExerciseDiffCallback : DiffUtil.ItemCallback<Exercise>() {
        override fun areItemsTheSame(oldItem: Exercise, newItem: Exercise): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Exercise, newItem: Exercise): Boolean =
            oldItem == newItem
    }
}
