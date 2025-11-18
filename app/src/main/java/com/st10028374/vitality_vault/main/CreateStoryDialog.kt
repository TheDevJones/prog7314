package com.st10028374.vitality_vault.main

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.st10028374.vitality_vault.R

class CreateStoryDialog : DialogFragment() {

    private lateinit var ivStoryPreview: ImageView
    private lateinit var btnSelectImage: MaterialButton
    private lateinit var btnFromWorkout: MaterialButton
    private lateinit var etStoryCaption: EditText
    private lateinit var btnCreateStory: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var imageContainer: MaterialCardView
    private lateinit var btnRemoveImage: ImageButton

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var selectedImageUri: Uri? = null
    private var selectedWorkout: WorkoutItem? = null
    private var onStoryCreatedListener: (() -> Unit)? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                ivStoryPreview.setImageURI(uri)
                imageContainer.visibility = View.VISIBLE
                btnRemoveImage.visibility = View.VISIBLE
            }
        }
    }

    companion object {
        fun newInstance(): CreateStoryDialog {
            return CreateStoryDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_create_story, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupListeners()
    }

    private fun initializeViews(view: View) {
        ivStoryPreview = view.findViewById(R.id.ivStoryPreview)
        btnSelectImage = view.findViewById(R.id.btnSelectStoryImage)
        btnFromWorkout = view.findViewById(R.id.btnStoryFromWorkout)
        etStoryCaption = view.findViewById(R.id.etStoryCaption)
        btnCreateStory = view.findViewById(R.id.btnCreateStory)
        btnCancel = view.findViewById(R.id.btnCancelStory)
        progressBar = view.findViewById(R.id.progressBarStory)
        imageContainer = view.findViewById(R.id.storyImageContainer)
        btnRemoveImage = view.findViewById(R.id.btnRemoveStoryImage)
    }

    private fun setupListeners() {
        btnSelectImage.setOnClickListener {
            openImagePicker()
        }

        btnFromWorkout.setOnClickListener {
            selectFromWorkout()
        }

        btnRemoveImage.setOnClickListener {
            selectedImageUri = null
            imageContainer.visibility = View.GONE
            btnRemoveImage.visibility = View.GONE
        }

        btnCreateStory.setOnClickListener {
            createStory()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private fun selectFromWorkout() {
        // Load recent workout and auto-create story
        val userId = auth.currentUser?.uid ?: return

        progressBar.visibility = View.VISIBLE

        db.collection("workouts")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val workout = documents.documents[0]
                    selectedWorkout = WorkoutItem(
                        id = workout.id,
                        activityType = workout.getString("activityType") ?: "Workout",
                        duration = workout.getString("duration") ?: "00:00:00",
                        date = workout.getString("date") ?: "",
                        intensity = workout.getString("intensity") ?: "Moderate",
                        completionPercentage = workout.getLong("completionPercentage")?.toInt() ?: 100
                    )

                    // Show workout info
                    etStoryCaption.setText("Just completed a ${selectedWorkout?.activityType}! 💪")
                    Toast.makeText(context, "Workout loaded! Add a caption and post.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No recent workouts found", Toast.LENGTH_SHORT).show()
                }
                progressBar.visibility = View.GONE
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(context, "Failed to load workout", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createStory() {
        val caption = etStoryCaption.text.toString().trim()

        if (selectedImageUri == null && selectedWorkout == null) {
            Toast.makeText(context, "Please select an image or workout", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser ?: return
        progressBar.visibility = View.VISIBLE
        btnCreateStory.isEnabled = false

        // Get user's display name
        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val displayName = document.getString("displayName") ?: "User"

                val now = System.currentTimeMillis()
                val expiresAt = now + (24 * 60 * 60 * 1000) // 24 hours from now

                // Create story data
                val newStory = hashMapOf(
                    "userId" to user.uid,
                    "userName" to displayName,
                    "userAvatar" to "",
                    "imageUrl" to (selectedImageUri?.toString() ?: ""),
                    "caption" to caption,
                    "timestamp" to now,
                    "expiresAt" to expiresAt,
                    "viewers" to listOf<String>()
                )

                // Add workout data if selected
                selectedWorkout?.let { workout ->
                    val distance = calculateDistance(workout)
                    val calories = calculateCalories(workout)

                    newStory["workoutData"] = hashMapOf(
                        "workoutType" to workout.activityType,
                        "duration" to workout.duration,
                        "distance" to distance,
                        "calories" to calories,
                        "intensity" to workout.intensity
                    )
                }

                // Save story to Firestore
                db.collection("stories")
                    .add(newStory)
                    .addOnSuccessListener {
                        progressBar.visibility = View.GONE
                        Toast.makeText(context, "Story posted! 🎉", Toast.LENGTH_SHORT).show()
                        onStoryCreatedListener?.invoke()
                        dismiss()
                    }
                    .addOnFailureListener { e ->
                        progressBar.visibility = View.GONE
                        btnCreateStory.isEnabled = true
                        Toast.makeText(context, "Failed to post story: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun calculateDistance(workout: WorkoutItem): String {
        val activityType = workout.activityType.lowercase()
        if (activityType.contains("yoga") ||
            activityType.contains("hiit") ||
            activityType.contains("strength")) {
            return ""
        }

        val durationParts = workout.duration.split(":")
        if (durationParts.size != 3) return "0.0 km"

        val hours = durationParts[0].toIntOrNull() ?: 0
        val minutes = durationParts[1].toIntOrNull() ?: 0
        val seconds = durationParts[2].toIntOrNull() ?: 0

        val totalMinutes = hours * 60 + minutes + seconds / 60.0

        val speed = when (activityType) {
            "running" -> 10.0
            "cycling" -> 20.0
            "walking" -> 5.0
            "swimming" -> 3.0
            else -> 8.0
        }

        val distance = (totalMinutes / 60.0) * speed
        return String.format("%.1f km", distance)
    }

    private fun calculateCalories(workout: WorkoutItem): String {
        val durationParts = workout.duration.split(":")
        if (durationParts.size != 3) return "0 kcal"

        val hours = durationParts[0].toIntOrNull() ?: 0
        val minutes = durationParts[1].toIntOrNull() ?: 0
        val seconds = durationParts[2].toIntOrNull() ?: 0

        val totalMinutes = hours * 60 + minutes + seconds / 60.0

        val caloriesPerMinute = when (workout.intensity.lowercase()) {
            "low" -> 5.0
            "moderate" -> 8.0
            "high" -> 12.0
            else -> 8.0
        }

        val calories = totalMinutes * caloriesPerMinute
        return String.format("%.0f kcal", calories)
    }

    fun setOnStoryCreatedListener(listener: () -> Unit) {
        onStoryCreatedListener = listener
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}