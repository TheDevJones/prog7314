package com.st10028374.vitality_vault.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.st10028374.vitality_vault.database.repository.OfflineRepository
import com.st10028374.vitality_vault.databinding.FragmentDashboardBinding
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var offlineRepository: OfflineRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)

        // Initialize offline repository
        offlineRepository = OfflineRepository(requireContext())

        setupWorkoutButton()
        setupRecyclerView()
        loadUserGreeting()
        loadRecentWorkouts()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // Refresh workouts whenever user navigates back
        loadRecentWorkouts()
        loadUserGreeting()
    }

    /** Setup Start Workout button */
    private fun setupWorkoutButton() {
        binding.startWorkout.setOnClickListener {
            val intent = Intent(requireContext(), WorkoutActivity::class.java)
            startActivity(intent)
        }
    }

    /** Setup RecyclerView */
    private fun setupRecyclerView() {
        binding.recentWorkoutsRecycler.layoutManager = LinearLayoutManager(requireContext())
    }

    /** Load the user's display name for greeting */
    private fun loadUserGreeting() {
        val user = auth.currentUser
        val displayName = user?.displayName ?: "User"
        val hourOfDay = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val greetingText = when (hourOfDay) {
            in 5..11 -> "Good Morning, $displayName!"
            in 12..17 -> "Good Afternoon, $displayName!"
            else -> "Good Evening, $displayName!"
        }
        binding.greeting.text = greetingText
    }

    /** Load the 5 most recent workouts from local database (works offline) */
    private fun loadRecentWorkouts() {
        val userId = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                val offlineRepository = OfflineRepository(requireContext())

                // Try to sync immediately if online
                if (offlineRepository.isOnline()) {
                    offlineRepository.syncAllData()
                }

                // Load from Room DB (works offline)
                val result = offlineRepository.getRecentWorkouts(userId)

                if (result.isSuccess) {
                    val workoutEntities = result.getOrNull() ?: emptyList()

                    // Convert to Workout objects
                    val workouts = workoutEntities.map { entity ->
                        Workout(
                            activityType = entity.activityType,
                            duration = entity.duration,
                            intensity = entity.intensity,
                            date = entity.date,
                            completionPercentage = entity.completionPercentage
                        )
                    }

                    val adapter = WorkoutAdapter(workouts)
                    binding.recentWorkoutsRecycler.adapter = adapter

                    // Show sync status if there are unsynced items
                    showSyncStatus()
                } else {
                    Log.e("DashboardFragment", "Failed to load workouts", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e("DashboardFragment", "Error loading workouts", e)
            }
        }
    }

    /** Show sync status if there are unsynced items */
    private fun showSyncStatus() {
        lifecycleScope.launch {
            try {
                val offlineRepository = OfflineRepository(requireContext())

                // Small delay to let sync complete
                kotlinx.coroutines.delay(1000)

                val (unsyncedWorkouts, unsyncedRoutes) = offlineRepository.getUnsyncedCount()

                if (unsyncedWorkouts > 0 || unsyncedRoutes > 0) {
                    val total = unsyncedWorkouts + unsyncedRoutes
                    val message = "You have $total items pending sync ($unsyncedWorkouts workouts, $unsyncedRoutes routes)"
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                } else if (offlineRepository.isOnline()) {
                    // All synced successfully
                    Toast.makeText(requireContext(), "✓ All data synced", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("DashboardFragment", "Error checking sync status", e)
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}