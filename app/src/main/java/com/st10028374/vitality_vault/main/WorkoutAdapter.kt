package com.st10028374.vitality_vault.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.st10028374.vitality_vault.databinding.ItemWorkoutBinding

class WorkoutAdapter(
    private val workouts: List<Workout>
) : RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder>() {

    inner class WorkoutViewHolder(private val binding: ItemWorkoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(workout: Workout) {
            binding.workoutType.text = workout.activityType

            val details = buildString {
                append(workout.date)
                append(" • ")
                append(workout.duration)
                append(" • ")
                append(workout.intensity) // Added intensity here
                if (workout.completionPercentage < 100) {
                    append(" • ${workout.completionPercentage}% completed")
                } else {
                    append(" • Completed")
                }
            }

            binding.workoutDetails.text = details
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val binding = ItemWorkoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WorkoutViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        holder.bind(workouts[position])
    }

    override fun getItemCount(): Int = workouts.size
}
