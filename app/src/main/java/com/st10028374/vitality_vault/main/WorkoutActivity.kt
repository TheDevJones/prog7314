package com.st10028374.vitality_vault.main

import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.st10028374.vitality_vault.database.repository.OfflineRepository
import com.st10028374.vitality_vault.databinding.ActivityWorkoutBinding
import com.st10028374.vitality_vault.utils.NotificationHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WorkoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorkoutBinding
    private lateinit var offlineRepository: OfflineRepository
    private var timer: CountDownTimer? = null
    private var totalTimeInMillis: Long = 0
    private var timeLeftInMillis: Long = 0
    private var isRunning = false
    private var workoutSaved = false

    // TTS variables
    private lateinit var ttsHelper: TextToSpeechHelper
    private var lastAnnouncedMinute = 0
    private var ttsEnabled = true
    private var startCountdownTimer: CountDownTimer? = null

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize offline repository
        offlineRepository = OfflineRepository(this)

        // Initialize TTS
        ttsHelper = TextToSpeechHelper(this)
        ttsHelper.initialize { success ->
            if (!success) {
                Toast.makeText(this, "Text-to-Speech not available", Toast.LENGTH_SHORT).show()
            }
        }

        // Load TTS preference
        val prefs = getSharedPreferences("VitalityVaultPrefs", MODE_PRIVATE)
        ttsEnabled = prefs.getBoolean("tts_enabled", true)

        setupSpinners()
        setupButtons()
    }

    private fun setupSpinners() {
        val workouts = listOf("Running", "Cycling", "Yoga", "Strength Training", "HIIT")
        binding.activityType.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, workouts)

        val intensities = listOf("Low", "Moderate", "High")
        binding.intensity.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, intensities)
    }

    private fun setupButtons() {
        binding.startButton.setOnClickListener {
            val goalDuration = binding.goalDuration.text.toString()
            if (goalDuration.isEmpty()) {
                Toast.makeText(this, "Please enter goal duration", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            totalTimeInMillis = parseTime(goalDuration)
            if (totalTimeInMillis <= 0) {
                Toast.makeText(this, "Invalid duration format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            timeLeftInMillis = totalTimeInMillis

            // Start countdown before workout
            if (ttsEnabled) {
                startWorkoutWithCountdown()
            } else {
                startWorkoutImmediately()
            }
        }

        binding.pauseButton.setOnClickListener {
            pauseTimer()
            binding.pauseButton.visibility = View.GONE
            binding.resumeButton.visibility = View.VISIBLE

            if (ttsEnabled) {
                ttsHelper.speak("Workout paused")
            }
        }

        binding.resumeButton.setOnClickListener {
            if (!isRunning && timeLeftInMillis > 0) {
                startTimer()
                binding.resumeButton.visibility = View.GONE
                binding.pauseButton.visibility = View.VISIBLE

                if (ttsEnabled) {
                    ttsHelper.speak("Resuming workout")
                }
            }
        }

        binding.endButton.setOnClickListener {
            pauseTimer()

            if (ttsEnabled) {
                ttsHelper.speak("Ending workout")
            }

            saveWorkout()
            finish()
        }
    }

    /**
     * Start workout with countdown (3, 2, 1, Go!)
     */
    private fun startWorkoutWithCountdown() {
        val workoutType = binding.activityType.selectedItem.toString()
        val intensity = binding.intensity.selectedItem.toString()
        val duration = binding.goalDuration.text.toString()

        // Disable start button during countdown
        binding.startButton.isEnabled = false
        binding.startButton.text = "Starting..."

        // Announce workout details
        ttsHelper.announceWorkoutStart(workoutType, duration, intensity)

        // Wait for initial announcement, then do countdown
        Handler(Looper.getMainLooper()).postDelayed({
            startCountdown()
        }, 3000) // 3 second delay for initial announcement
    }

    /**
     * Countdown timer (3, 2, 1, Go!)
     */
    private fun startCountdown() {
        var countValue = 3

        startCountdownTimer = object : CountDownTimer(4000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000).toInt()

                if (secondsRemaining > 0) {
                    binding.startButton.text = secondsRemaining.toString()
                    ttsHelper.announceCountdown(secondsRemaining)
                }
            }

            override fun onFinish() {
                binding.startButton.text = "GO!"
                ttsHelper.announceCountdown(0) // Says "Go!"

                // Small delay then start workout
                Handler(Looper.getMainLooper()).postDelayed({
                    startWorkoutImmediately()
                }, 500)
            }
        }.start()
    }

    /**
     * Start workout immediately without countdown
     */
    private fun startWorkoutImmediately() {
        startTimer()

        binding.startButton.visibility = View.GONE
        binding.pauseButton.visibility = View.VISIBLE
        binding.startButton.isEnabled = true
        binding.startButton.text = "Start"
    }

    private fun startTimer() {
        if (timeLeftInMillis <= 0) return

        timer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerText()

                // Check for milestones
                checkMilestones(millisUntilFinished)
            }

            override fun onFinish() {
                timeLeftInMillis = 0
                updateTimerText()
                isRunning = false

                if (ttsEnabled) {
                    ttsHelper.speak("Workout complete! Excellent work!")
                }

                saveWorkout()
                Toast.makeText(this@WorkoutActivity, "Workout Completed!", Toast.LENGTH_SHORT).show()
            }
        }.start()

        isRunning = true
    }

    private fun pauseTimer() {
        timer?.cancel()
        isRunning = false
    }

    private fun updateTimerText() {
        val elapsedMillis = totalTimeInMillis - timeLeftInMillis
        val hours = (elapsedMillis / 1000) / 3600
        val minutes = ((elapsedMillis / 1000) % 3600) / 60
        val seconds = (elapsedMillis / 1000) % 60

        binding.textView.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    /**
     * Check for workout milestones and send notifications
     */
    private fun checkMilestones(remainingMillis: Long) {
        val elapsedMillis = totalTimeInMillis - remainingMillis
        val elapsedMinutes = (elapsedMillis / 1000) / 60

        // Milestone notifications at 5, 10, 15, 20, 25, 30 minutes
        val milestones = listOf(5, 10, 15, 20, 25, 30)

        if (milestones.contains(elapsedMinutes.toInt()) && lastAnnouncedMinute != elapsedMinutes.toInt()) {
            lastAnnouncedMinute = elapsedMinutes.toInt()
            val workoutType = binding.activityType.selectedItem.toString()

            NotificationHelper.showWorkoutMilestoneNotification(
                context = this,
                milestone = "$elapsedMinutes minutes completed!",
                message = "Great progress on your $workoutType workout! Keep it up! 💪"
            )

            // TTS Announcement
            if (ttsEnabled) {
                ttsHelper.announceWorkoutMilestone(elapsedMinutes.toInt(), workoutType)
            }
        }
    }

    private fun parseTime(time: String): Long {
        val parts = time.split(":")
        if (parts.size != 3) return 0
        val h = parts[0].toLongOrNull() ?: 0
        val m = parts[1].toLongOrNull() ?: 0
        val s = parts[2].toLongOrNull() ?: 0
        return (h * 3600 + m * 60 + s) * 1000
    }

    private fun saveWorkout() {
        if (userId == null || workoutSaved) return
        workoutSaved = true

        val elapsedMillis = totalTimeInMillis - timeLeftInMillis
        if (elapsedMillis <= 0) {
            Toast.makeText(this, "No time elapsed to save", Toast.LENGTH_SHORT).show()
            return
        }

        val hours = (elapsedMillis / 1000) / 3600
        val minutes = ((elapsedMillis / 1000) % 3600) / 60
        val seconds = (elapsedMillis / 1000) % 60
        val durationFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)

        val workoutType = binding.activityType.selectedItem.toString()
        val intensity = binding.intensity.selectedItem.toString()
        val completionPercentage = if (timeLeftInMillis == 0L) 100 else ((elapsedMillis * 100) / totalTimeInMillis).toInt()

        // Calculate estimated calories
        val caloriesPerMinute = when (intensity) {
            "Low" -> 5.0
            "Moderate" -> 8.0
            "High" -> 12.0
            else -> 8.0
        }
        val totalMinutes = (elapsedMillis / 1000) / 60.0
        val estimatedCalories = (totalMinutes * caloriesPerMinute).toInt()

        // Use OfflineRepository instead of direct Firestore
        lifecycleScope.launch {
            try {
                val result = offlineRepository.saveWorkout(
                    userId = userId,
                    activityType = workoutType,
                    duration = durationFormatted,
                    intensity = intensity,
                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    completionPercentage = completionPercentage,
                    timestamp = Date(),
                    //calories = estimatedCalories
                )

                if (result.isSuccess) {
                    val unsyncedCount = offlineRepository.getUnsyncedCount()
                    val message = if (unsyncedCount.first == 0) {
                        "Workout saved and synced!"
                    } else {
                        "Workout saved offline. Will sync when online."
                    }
                    Toast.makeText(this@WorkoutActivity, message, Toast.LENGTH_LONG).show()

                    // Send workout completion notification
                    NotificationHelper.showWorkoutCompleteNotification(
                        context = this@WorkoutActivity,
                        workoutType = workoutType,
                        duration = durationFormatted,
                        calories = "$estimatedCalories kcal"
                    )

                    // TTS Announcement
                    if (ttsEnabled) {
                        ttsHelper.announceWorkoutComplete(
                            workoutType,
                            durationFormatted,
                            "$estimatedCalories kcal"
                        )
                    }

                    checkAchievements(workoutType, totalMinutes.toInt(), completionPercentage)

                } else {
                    Toast.makeText(
                        this@WorkoutActivity,
                        "Failed to save workout: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@WorkoutActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Check if user has unlocked any achievements
     */
    private fun checkAchievements(workoutType: String, minutes: Int, completion: Int) {
        val prefs = getSharedPreferences("VitalityVaultPrefs", MODE_PRIVATE)
        val firstWorkout = prefs.getBoolean("achievement_first_workout", false)

        if (!firstWorkout) {
            prefs.edit().putBoolean("achievement_first_workout", true).apply()
            NotificationHelper.showAchievementNotification(
                context = this,
                achievementTitle = "First Steps",
                achievementDescription = "Completed your first workout!",
                xpEarned = 50
            )

            if (ttsEnabled) {
                ttsHelper.speak("Achievement unlocked! First Steps. You earned 50 experience points!")
            }
        }

        if (minutes >= 30 && completion == 100) {
            val marathonAchievement = prefs.getBoolean("achievement_marathon", false)
            if (!marathonAchievement) {
                prefs.edit().putBoolean("achievement_marathon", true).apply()
                NotificationHelper.showAchievementNotification(
                    context = this,
                    achievementTitle = "Marathon Runner",
                    achievementDescription = "Completed a 30+ minute workout!",
                    xpEarned = 100
                )

                if (ttsEnabled) {
                    ttsHelper.speak("Achievement unlocked! Marathon Runner. You earned 100 experience points!")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        startCountdownTimer?.cancel()
        ttsHelper.shutdown()
    }
}