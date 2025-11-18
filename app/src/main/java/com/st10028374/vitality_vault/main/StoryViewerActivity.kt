package com.st10028374.vitality_vault.main

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.st10028374.vitality_vault.R
import java.text.SimpleDateFormat
import java.util.*

class StoryViewerActivity : AppCompatActivity() {

    private lateinit var ivStoryImage: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserAvatar: TextView
    private lateinit var tvTimestamp: TextView
    private lateinit var tvCaption: TextView
    private lateinit var btnClose: ImageButton
    private lateinit var progressContainer: LinearLayout
    private lateinit var workoutStatsCard: MaterialCardView
    private lateinit var tvWorkoutType: TextView
    private lateinit var tvWorkoutStats: TextView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var userStoriesList = mutableListOf<UserStories>()
    private var currentUserIndex = 0
    private var currentStoryIndex = 0

    private val storyDuration = 5000L // 5 seconds per story
    private val handler = Handler(Looper.getMainLooper())
    private var storyRunnable: Runnable? = null
    private var isPaused = false
    private var pressStartTime = 0L

    private val progressBars = mutableListOf<ProgressBar>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_story_viewer)

        // Make fullscreen
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        initializeViews()
        loadStoriesData()
        setupTouchListeners()
    }

    private fun initializeViews() {
        ivStoryImage = findViewById(R.id.ivStoryImage)
        tvUserName = findViewById(R.id.tvStoryUserName)
        tvUserAvatar = findViewById(R.id.tvStoryUserAvatar)
        tvTimestamp = findViewById(R.id.tvStoryTimestamp)
        tvCaption = findViewById(R.id.tvStoryCaption)
        btnClose = findViewById(R.id.btnCloseStory)
        progressContainer = findViewById(R.id.progressContainer)
        workoutStatsCard = findViewById(R.id.workoutStatsCard)
        tvWorkoutType = findViewById(R.id.tvStoryWorkoutType)
        tvWorkoutStats = findViewById(R.id.tvStoryWorkoutStats)

        btnClose.setOnClickListener { finish() }
    }

    private fun loadStoriesData() {
        val userId = intent.getStringExtra("userId") ?: ""
        val position = intent.getIntExtra("position", 0)

        // Load all user stories
        db.collection("stories")
            .whereGreaterThan("expiresAt", System.currentTimeMillis())
            .get()
            .addOnSuccessListener { documents ->
                val storiesMap = mutableMapOf<String, MutableList<Story>>()

                documents.forEach { doc ->
                    val story = Story(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        userName = doc.getString("userName") ?: "",
                        userAvatar = doc.getString("userAvatar") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        caption = doc.getString("caption") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        expiresAt = doc.getLong("expiresAt") ?: 0L,
                        viewers = (doc.get("viewers") as? List<String>)?.toMutableList() ?: mutableListOf()
                    )

                    if (!storiesMap.containsKey(story.userId)) {
                        storiesMap[story.userId] = mutableListOf()
                    }
                    storiesMap[story.userId]?.add(story)
                }

                // Convert to UserStories list
                storiesMap.forEach { (uid, stories) ->
                    val userStories = UserStories(
                        userId = uid,
                        userName = stories.firstOrNull()?.userName ?: "",
                        userAvatar = stories.firstOrNull()?.userAvatar ?: "",
                        stories = stories.sortedBy { it.timestamp },
                        hasUnviewed = stories.any { !it.viewers.contains(auth.currentUser?.uid) }
                    )
                    userStoriesList.add(userStories)
                }

                // Sort to match the order they were clicked
                userStoriesList.sortBy { it.userId != userId }
                currentUserIndex = position

                if (userStoriesList.isNotEmpty()) {
                    displayStory()
                } else {
                    finish()
                }
            }
    }

    private fun displayStory() {
        if (currentUserIndex >= userStoriesList.size) {
            finish()
            return
        }

        val userStories = userStoriesList[currentUserIndex]
        if (currentStoryIndex >= userStories.stories.size) {
            moveToNextUser()
            return
        }

        val story = userStories.stories[currentStoryIndex]

        // Update UI
        tvUserName.text = story.userName
        tvUserAvatar.text = story.userName.firstOrNull()?.uppercase() ?: "U"
        tvTimestamp.text = getTimeAgo(story.timestamp)

        if (story.caption.isNotEmpty()) {
            tvCaption.visibility = View.VISIBLE
            tvCaption.text = story.caption
        } else {
            tvCaption.visibility = View.GONE
        }

        // Show workout stats if available
        story.workoutData?.let { workout ->
            workoutStatsCard.visibility = View.VISIBLE
            tvWorkoutType.text = workout.workoutType
            tvWorkoutStats.text = buildString {
                if (workout.duration.isNotEmpty()) append(workout.duration)
                if (workout.distance.isNotEmpty()) {
                    if (isNotEmpty()) append(" • ")
                    append(workout.distance)
                }
                if (workout.calories.isNotEmpty()) {
                    if (isNotEmpty()) append(" • ")
                    append(workout.calories)
                }
            }
        } ?: run {
            workoutStatsCard.visibility = View.GONE
        }

        // Load image
        ivStoryImage.setBackgroundResource(R.drawable.workout_image)

        // Setup progress bars
        setupProgressBars(userStories.stories.size)

        // Mark as viewed
        markStoryAsViewed(story.id)

        // Start auto-progress
        startStoryTimer()
    }

    private fun setupProgressBars(count: Int) {
        progressContainer.removeAllViews()
        progressBars.clear()

        for (i in 0 until count) {
            val progressBar = ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
            ).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    8,
                    1f
                ).apply {
                    marginStart = if (i == 0) 0 else 4
                    marginEnd = if (i == count - 1) 0 else 4
                }
                max = 100
                progress = if (i < currentStoryIndex) 100 else 0
                progressDrawable = resources.getDrawable(R.drawable.story_progress, null)
            }

            progressContainer.addView(progressBar)
            progressBars.add(progressBar)
        }
    }

    private fun startStoryTimer() {
        storyRunnable?.let { handler.removeCallbacks(it) }

        val progressBar = progressBars.getOrNull(currentStoryIndex) ?: return
        progressBar.progress = 0

        val startTime = System.currentTimeMillis()
        storyRunnable = object : Runnable {
            override fun run() {
                if (isPaused) {
                    handler.postDelayed(this, 50)
                    return
                }

                val elapsed = System.currentTimeMillis() - startTime
                val progress = ((elapsed.toFloat() / storyDuration) * 100).toInt()

                if (progress >= 100) {
                    progressBar.progress = 100
                    moveToNextStory()
                } else {
                    progressBar.progress = progress
                    handler.postDelayed(this, 50)
                }
            }
        }
        handler.post(storyRunnable!!)
    }

    private fun setupTouchListeners() {
        val rootView = findViewById<View>(R.id.storyRootLayout)

        rootView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    pressStartTime = System.currentTimeMillis()

                    // Determine if left or right side
                    val x = event.x
                    val screenWidth = rootView.width

                    if (x < screenWidth / 3) {
                        // Left third - previous story
                        moveToPreviousStory()
                    } else if (x > screenWidth * 2 / 3) {
                        // Right third - next story
                        moveToNextStory()
                    } else {
                        // Middle - pause
                        isPaused = true
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val pressDuration = System.currentTimeMillis() - pressStartTime

                    // Only unpause if it was a long press (hold to pause)
                    if (pressDuration > 200) {
                        isPaused = false
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun moveToNextStory() {
        currentStoryIndex++
        val userStories = userStoriesList[currentUserIndex]

        if (currentStoryIndex >= userStories.stories.size) {
            moveToNextUser()
        } else {
            displayStory()
        }
    }

    private fun moveToPreviousStory() {
        if (currentStoryIndex > 0) {
            currentStoryIndex--
            displayStory()
        } else if (currentUserIndex > 0) {
            currentUserIndex--
            currentStoryIndex = userStoriesList[currentUserIndex].stories.size - 1
            displayStory()
        }
    }

    private fun moveToNextUser() {
        currentUserIndex++
        currentStoryIndex = 0

        if (currentUserIndex >= userStoriesList.size) {
            finish()
        } else {
            displayStory()
        }
    }

    private fun markStoryAsViewed(storyId: String) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("stories").document(storyId)
            .get()
            .addOnSuccessListener { document ->
                val viewers = (document.get("viewers") as? List<String>)?.toMutableList() ?: mutableListOf()
                if (!viewers.contains(userId)) {
                    viewers.add(userId)
                    db.collection("stories").document(storyId)
                        .update("viewers", viewers)
                }
            }
    }

    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
        }
    }

    override fun onPause() {
        super.onPause()
        isPaused = true
    }

    override fun onResume() {
        super.onResume()
        isPaused = false
    }

    override fun onDestroy() {
        super.onDestroy()
        storyRunnable?.let { handler.removeCallbacks(it) }
    }
}