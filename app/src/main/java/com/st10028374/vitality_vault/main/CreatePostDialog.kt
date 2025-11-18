package com.st10028374.vitality_vault.main

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.st10028374.vitality_vault.R

data class WorkoutItem(
    val id: String,
    val activityType: String,
    val duration: String,
    val date: String,
    val intensity: String,
    val completionPercentage: Int
)

class CreatePostDialog : DialogFragment() {

    private lateinit var spinnerWorkouts: Spinner
    private lateinit var etCaption: EditText
    private lateinit var ivSelectedImage: ImageView
    private lateinit var btnSelectImage: MaterialButton
    private lateinit var btnRemoveImage: ImageButton
    private lateinit var btnCreatePost: MaterialButton
    private lateinit var btnCancel: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var imageContainer: MaterialCardView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val workouts = mutableListOf<WorkoutItem>()
    private var selectedImageUri: Uri? = null
    private var onPostCreatedListener: (() -> Unit)? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                ivSelectedImage.setImageURI(uri)
                imageContainer.visibility = View.VISIBLE
                btnRemoveImage.visibility = View.VISIBLE
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(context, "Permission denied. Cannot select images.", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun newInstance(): CreatePostDialog {
            return CreatePostDialog()
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
        return inflater.inflate(R.layout.dialog_create_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        loadWorkouts()
        setupListeners()
    }

    private fun initializeViews(view: View) {
        spinnerWorkouts = view.findViewById(R.id.spinnerWorkouts)
        etCaption = view.findViewById(R.id.etCaption)
        ivSelectedImage = view.findViewById(R.id.ivSelectedImage)
        btnSelectImage = view.findViewById(R.id.btnSelectImage)
        btnRemoveImage = view.findViewById(R.id.btnRemoveImage)
        btnCreatePost = view.findViewById(R.id.btnCreatePost)
        btnCancel = view.findViewById(R.id.btnCancel)
        progressBar = view.findViewById(R.id.progressBar)
        imageContainer = view.findViewById(R.id.imageContainer)
    }

    private fun setupListeners() {
        btnSelectImage.setOnClickListener {
            checkPermissionAndOpenPicker()
        }

        btnRemoveImage.setOnClickListener {
            selectedImageUri = null
            imageContainer.visibility = View.GONE
            btnRemoveImage.visibility = View.GONE
        }

        btnCreatePost.setOnClickListener {
            createPost()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        // Setup quick caption chips
        view?.findViewById<com.google.android.material.chip.Chip>(R.id.chipCaption1)?.setOnClickListener {
            etCaption.setText("💪 Feeling strong!")
        }
        view?.findViewById<com.google.android.material.chip.Chip>(R.id.chipCaption2)?.setOnClickListener {
            etCaption.setText("🔥 New PR today!")
        }
        view?.findViewById<com.google.android.material.chip.Chip>(R.id.chipCaption3)?.setOnClickListener {
            etCaption.setText("✨ Crushed it!")
        }
        view?.findViewById<com.google.android.material.chip.Chip>(R.id.chipCaption4)?.setOnClickListener {
            etCaption.setText("🎯 Goals achieved!")
        }
    }

    private fun checkPermissionAndOpenPicker() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                openImagePicker()
            }
            else -> {
                permissionLauncher.launch(permission)
            }
        }
    }

    private fun loadWorkouts() {
        val userId = auth.currentUser?.uid ?: return

        progressBar.visibility = View.VISIBLE

        db.collection("workouts")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .addOnSuccessListener { documents ->
                workouts.clear()

                if (documents.isEmpty) {
                    Toast.makeText(context, "No workouts found. Complete a workout first!", Toast.LENGTH_LONG).show()
                    dismiss()
                    return@addOnSuccessListener
                }

                documents.forEach { doc ->
                    val workout = WorkoutItem(
                        id = doc.id,
                        activityType = doc.getString("activityType") ?: "Workout",
                        duration = doc.getString("duration") ?: "00:00:00",
                        date = doc.getString("date") ?: "",
                        intensity = doc.getString("intensity") ?: "Moderate",
                        completionPercentage = doc.getLong("completionPercentage")?.toInt() ?: 100
                    )
                    workouts.add(workout)
                }

                setupWorkoutSpinner()
                progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(context, "Failed to load workouts: ${e.message}", Toast.LENGTH_SHORT).show()
                dismiss()
            }
    }

    private fun setupWorkoutSpinner() {
        val workoutStrings = workouts.map { workout ->
            "${workout.activityType} - ${workout.duration} (${workout.date})"
        }

        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item_workout,
            workoutStrings
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerWorkouts.adapter = adapter
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private fun createPost() {
        val selectedPosition = spinnerWorkouts.selectedItemPosition
        if (selectedPosition < 0 || selectedPosition >= workouts.size) {
            Toast.makeText(context, "Please select a workout", Toast.LENGTH_SHORT).show()
            return
        }

        val caption = etCaption.text.toString().trim()
        if (caption.isEmpty()) {
            Toast.makeText(context, "Please add a caption", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedWorkout = workouts[selectedPosition]
        val user = auth.currentUser ?: return

        progressBar.visibility = View.VISIBLE
        btnCreatePost.isEnabled = false

        // Get user's display name
        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val displayName = document.getString("displayName") ?: "User"

                // Calculate distance and calories based on workout
                val distance = calculateDistance(selectedWorkout)
                val calories = calculateCalories(selectedWorkout)

                // Create post data
                val newPost = hashMapOf(
                    "userId" to user.uid,
                    "userName" to displayName,
                    "userAvatar" to "",
                    "workoutType" to selectedWorkout.activityType,
                    "duration" to selectedWorkout.duration,
                    "distance" to distance,
                    "calories" to calories,
                    "caption" to caption,
                    "imageUrl" to (selectedImageUri?.toString() ?: ""),
                    "timestamp" to System.currentTimeMillis(),
                    "likes" to listOf<String>(),
                    "commentCount" to 0
                )

                // Save post to Firestore
                db.collection("posts")
                    .add(newPost)
                    .addOnSuccessListener {
                        progressBar.visibility = View.GONE
                        Toast.makeText(context, "Post created successfully! 🎉", Toast.LENGTH_SHORT).show()
                        onPostCreatedListener?.invoke()
                        dismiss()
                    }
                    .addOnFailureListener { e ->
                        progressBar.visibility = View.GONE
                        btnCreatePost.isEnabled = true
                        Toast.makeText(context, "Failed to create post: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                btnCreatePost.isEnabled = true
                Toast.makeText(context, "Failed to get user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun calculateDistance(workout: WorkoutItem): String {
        // Don't show distance for static workouts
        val activityType = workout.activityType.lowercase()
        if (activityType.contains("yoga") ||
            activityType.contains("hiit") ||
            activityType.contains("strength")) {
            return "" // Return empty string for these activities
        }

        // Simple calculation based on duration and activity type
        val durationParts = workout.duration.split(":")
        if (durationParts.size != 3) return "0.0 km"

        val hours = durationParts[0].toIntOrNull() ?: 0
        val minutes = durationParts[1].toIntOrNull() ?: 0
        val seconds = durationParts[2].toIntOrNull() ?: 0

        val totalMinutes = hours * 60 + minutes + seconds / 60.0

        // Average speeds (km/h) for different activities
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
        // Simple calculation based on duration and intensity
        val durationParts = workout.duration.split(":")
        if (durationParts.size != 3) return "0 kcal"

        val hours = durationParts[0].toIntOrNull() ?: 0
        val minutes = durationParts[1].toIntOrNull() ?: 0
        val seconds = durationParts[2].toIntOrNull() ?: 0

        val totalMinutes = hours * 60 + minutes + seconds / 60.0

        // Calories per minute based on intensity
        val caloriesPerMinute = when (workout.intensity.lowercase()) {
            "low" -> 5.0
            "moderate" -> 8.0
            "high" -> 12.0
            else -> 8.0
        }

        val calories = totalMinutes * caloriesPerMinute
        return String.format("%.0f kcal", calories)
    }

    fun setOnPostCreatedListener(listener: () -> Unit) {
        onPostCreatedListener = listener
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}