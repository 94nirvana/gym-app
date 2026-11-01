package com.gymapp.minimalist.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gymapp.minimalist.databinding.ItemHistorySessionBinding
import com.gymapp.minimalist.domain.WorkoutSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistorySessionAdapter(
    private var sessions: List<WorkoutSession> = emptyList()
) : RecyclerView.Adapter<HistorySessionAdapter.HistoryViewHolder>() {

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault())

    fun submitList(newList: List<WorkoutSession>) {
        sessions = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistorySessionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(sessions[position])
    }

    override fun getItemCount(): Int = sessions.size

    inner class HistoryViewHolder(
        private val binding: ItemHistorySessionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(session: WorkoutSession) {
            binding.tvSessionLetterBadge.text = session.workoutLetter
            binding.tvSessionName.text = session.workoutName
            binding.tvSessionDate.text = dateFormatter.format(Date(session.completedAt))

            val volumeFormatted = if (session.totalVolumeKg % 1.0 == 0.0) {
                String.format(Locale.getDefault(), "%.0f kg", session.totalVolumeKg)
            } else {
                String.format(Locale.getDefault(), "%.1f kg", session.totalVolumeKg)
            }
            binding.tvSessionVolume.text = volumeFormatted

            binding.tvSessionSets.text = String.format(
                Locale.getDefault(),
                "%d séries • %d exerc.",
                session.totalCompletedSets,
                session.totalExercises
            )
        }
    }
}
