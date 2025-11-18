package com.st10028374.vitality_vault.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.st10028374.vitality_vault.R
import com.st10028374.vitality_vault.databinding.FragmentSocialBinding
import com.st10028374.vitality_vault.routes.adapter.PostsAdapter


data class Post(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "",
    val workoutType: String = "",
    val duration: String = "",
    val distance: String = "",
    val calories: String = "",
    val caption: String = "",
    val imageUrl: String = "",
    val timestamp: Long = 0L,
    val likes: MutableList<String> = mutableListOf(),
    val commentCount: Int = 0
)

data class Comment(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "",
    val comment: String = "",
    val timestamp: Long = 0L
)

class SocialFragment : Fragment() {

    private var _binding: FragmentSocialBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var postsAdapter: PostsAdapter
    private val posts = mutableListOf<Post>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSocialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupUserProfile()
        setupButtons()
        loadPosts()
        loadStories()
    }

    private fun setupRecyclerView() {
        postsAdapter = PostsAdapter(
            posts = posts,
            currentUserId = auth.currentUser?.uid ?: "",
            onLikeClick = { post -> handleLike(post) },
            onCommentClick = { post -> showComments(post) },
            onShareClick = { post -> sharePost(post) },
            onProfileClick = { userId -> showUserProfile(userId) }
        )

        binding.rvPosts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postsAdapter
        }
    }

    private fun setupUserProfile() {
        val user = auth.currentUser
        val userId = user?.uid ?: return

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                // Check if fragment is still attached
                if (_binding == null) return@addOnSuccessListener

                if (document.exists()) {
                    val displayName = document.getString("displayName") ?: "User"
                    binding.tvWelcomeUser.text = "Welcome back, $displayName"
                }
            }
    }

    private fun setupButtons() {
        binding.btnSettings.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, SettingsFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnCreatePost.setOnClickListener {
            createNewPost()
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadPosts()
            loadStories()
        }

        setupStoriesClickListeners()
    }

    private fun setupStoriesClickListeners() {
        // Find story containers by ID in the view
        view?.findViewById<View>(R.id.yourStoryContainer)?.setOnClickListener {
            showCreateStoryDialog()
        }

        view?.findViewById<View>(R.id.story1Container)?.setOnClickListener {
            openStoryViewer("alex_user_id", 0)
        }

        view?.findViewById<View>(R.id.story2Container)?.setOnClickListener {
            openStoryViewer("john_user_id", 1)
        }

        view?.findViewById<View>(R.id.story3Container)?.setOnClickListener {
            openStoryViewer("lisa_user_id", 2)
        }

        view?.findViewById<View>(R.id.story4Container)?.setOnClickListener {
            openStoryViewer("tom_user_id", 3)
        }
    }

    private fun showCreateStoryDialog() {
        val dialog = CreateStoryDialog.newInstance()
        dialog.setOnStoryCreatedListener {
            loadStories()
            Toast.makeText(context, "Story created successfully!", Toast.LENGTH_SHORT).show()
        }
        dialog.show(parentFragmentManager, "create_story")
    }

    private fun openStoryViewer(userId: String, position: Int) {
        val intent = android.content.Intent(requireContext(), StoryViewerActivity::class.java)
        intent.putExtra("userId", userId)
        intent.putExtra("position", position)
        startActivity(intent)
    }

    private fun loadStories() {
        val currentUserId = auth.currentUser?.uid ?: return

        // Check if binding is null
        if (_binding == null) return

        // Load stories from Firestore
        db.collection("stories")
            .whereGreaterThan("expiresAt", System.currentTimeMillis())
            .get()
            .addOnSuccessListener { documents ->
                // Check if fragment is still attached
                if (_binding == null) return@addOnSuccessListener

                val userStoriesMap = mutableMapOf<String, MutableList<Story>>()

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

                    if (!userStoriesMap.containsKey(story.userId)) {
                        userStoriesMap[story.userId] = mutableListOf()
                    }
                    userStoriesMap[story.userId]?.add(story)
                }

                // Update story ring colors based on viewed status
                updateStoryRings(userStoriesMap, currentUserId)
            }
    }

    private fun updateStoryRings(storiesMap: Map<String, List<Story>>, currentUserId: String) {
        // Check if user has unviewed stories and update ring colors
        storiesMap.forEach { (userId, stories) ->
            val hasUnviewed = stories.any { !it.viewers.contains(currentUserId) }
            // Update UI to show purple ring for unviewed, gray for viewed
            // This can be done by changing strokeColor in the story avatars
        }
    }

    private fun loadPosts() {
        // Check if binding is null before starting
        if (_binding == null) return

        binding.progressBar.visibility = View.VISIBLE

        db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener { documents ->
                // Check if fragment is still attached before updating UI
                if (_binding == null) return@addOnSuccessListener

                posts.clear()
                documents.forEach { doc ->
                    val post = Post(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        userName = doc.getString("userName") ?: "User",
                        userAvatar = doc.getString("userAvatar") ?: "",
                        workoutType = doc.getString("workoutType") ?: "",
                        duration = doc.getString("duration") ?: "",
                        distance = doc.getString("distance") ?: "",
                        calories = doc.getString("calories") ?: "",
                        caption = doc.getString("caption") ?: "",
                        imageUrl = doc.getString("imageUrl") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        likes = (doc.get("likes") as? List<String>)?.toMutableList() ?: mutableListOf(),
                        commentCount = doc.getLong("commentCount")?.toInt() ?: 0
                    )
                    posts.add(post)
                }
                postsAdapter.notifyDataSetChanged()
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
            .addOnFailureListener {
                // Check if fragment is still attached before updating UI
                if (_binding == null) return@addOnFailureListener

                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(context, "Failed to load posts", Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleLike(post: Post) {
        val userId = auth.currentUser?.uid ?: return
        val postRef = db.collection("posts").document(post.id)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val likes = (snapshot.get("likes") as? List<String>)?.toMutableList() ?: mutableListOf()

            if (likes.contains(userId)) {
                likes.remove(userId)
            } else {
                likes.add(userId)
            }

            transaction.update(postRef, "likes", likes)
        }.addOnSuccessListener {
            loadPosts()
        }
    }

    private fun showComments(post: Post) {
        // Navigate to comments fragment
        val commentsFragment = CommentsBottomSheet.newInstance(post.id)
        commentsFragment.show(parentFragmentManager, "comments")
    }

    private fun sharePost(post: Post) {
        Toast.makeText(context, "Share functionality coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun showUserProfile(userId: String) {
        Toast.makeText(context, "Profile view coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun createNewPost() {
        val user = auth.currentUser ?: return

        // Show create post dialog
        val dialog = CreatePostDialog.newInstance()
        dialog.setOnPostCreatedListener {
            loadPosts()
        }
        dialog.show(parentFragmentManager, "create_post")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class CommentsBottomSheet : com.google.android.material.bottomsheet.BottomSheetDialogFragment() {

    private lateinit var rvComments: androidx.recyclerview.widget.RecyclerView
    private lateinit var etComment: android.widget.EditText
    private lateinit var btnSendComment: android.widget.ImageButton
    private lateinit var btnClose: android.widget.ImageButton
    private lateinit var tvCommentUserAvatar: android.widget.TextView
    private lateinit var progressBar: android.widget.ProgressBar

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val comments = mutableListOf<Comment>()
    private lateinit var commentsAdapter: CommentsAdapter
    private var postId: String = ""

    companion object {
        fun newInstance(postId: String): CommentsBottomSheet {
            val fragment = CommentsBottomSheet()
            val args = Bundle()
            args.putString("postId", postId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_comments, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postId = arguments?.getString("postId") ?: ""

        initializeViews(view)
        setupRecyclerView()
        loadUserAvatar()
        loadComments()
        setupListeners()
    }

    private fun initializeViews(view: View) {
        rvComments = view.findViewById(R.id.rvComments)
        etComment = view.findViewById(R.id.etComment)
        btnSendComment = view.findViewById(R.id.btnSendComment)
        btnClose = view.findViewById(R.id.btnClose)
        tvCommentUserAvatar = view.findViewById(R.id.tvCommentUserAvatar)
        progressBar = view.findViewById(R.id.progressBarComments)
    }

    private fun setupRecyclerView() {
        commentsAdapter = CommentsAdapter(comments)
        rvComments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = commentsAdapter
        }
    }

    private fun loadUserAvatar() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                // Check if view is still available
                if (!isAdded) return@addOnSuccessListener

                val displayName = document.getString("displayName") ?: "User"
                tvCommentUserAvatar.text = displayName.firstOrNull()?.uppercase() ?: "U"
            }
    }

    private fun loadComments() {
        progressBar.visibility = View.VISIBLE

        db.collection("posts").document(postId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                // Check if fragment is still added
                if (!isAdded) return@addSnapshotListener

                if (error != null) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(context, "Failed to load comments", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                comments.clear()
                snapshots?.forEach { doc ->
                    val comment = Comment(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        userName = doc.getString("userName") ?: "User",
                        userAvatar = doc.getString("userAvatar") ?: "",
                        comment = doc.getString("comment") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                    comments.add(comment)
                }

                commentsAdapter.notifyDataSetChanged()
                progressBar.visibility = View.GONE

                // Scroll to bottom to show newest comments
                if (comments.isNotEmpty()) {
                    rvComments.scrollToPosition(comments.size - 1)
                }
            }
    }

    private fun setupListeners() {
        btnClose.setOnClickListener {
            dismiss()
        }

        btnSendComment.setOnClickListener {
            sendComment()
        }

        // Send on enter key
        etComment.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendComment()
                true
            } else {
                false
            }
        }
    }

    private fun sendComment() {
        val commentText = etComment.text.toString().trim()
        if (commentText.isEmpty()) {
            Toast.makeText(context, "Please write a comment", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser ?: return

        btnSendComment.isEnabled = false

        // Get user's display name
        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                if (!isAdded) {
                    btnSendComment.isEnabled = true
                    return@addOnSuccessListener
                }

                val displayName = document.getString("displayName") ?: "User"

                val newComment = hashMapOf(
                    "userId" to user.uid,
                    "userName" to displayName,
                    "userAvatar" to "",
                    "comment" to commentText,
                    "timestamp" to System.currentTimeMillis()
                )

                // Add comment to subcollection
                db.collection("posts").document(postId)
                    .collection("comments")
                    .add(newComment)
                    .addOnSuccessListener {
                        if (!isAdded) {
                            btnSendComment.isEnabled = true
                            return@addOnSuccessListener
                        }

                        // Update comment count on post
                        updateCommentCount()

                        // Get post owner ID and send notification
                        db.collection("posts").document(postId)
                            .get()
                            .addOnSuccessListener { postDoc ->
                                val postOwnerId = postDoc.getString("userId")
                                if (postOwnerId != null && postOwnerId != user.uid) {
                                    NotificationTriggers.sendCommentNotification(
                                        context = requireContext(),
                                        postOwnerId = postOwnerId,
                                        commenterUserId = user.uid,
                                        commenterUserName = displayName,
                                        comment = commentText,
                                        postId = postId
                                    )
                                }
                            }

                        // Clear input
                        etComment.setText("")
                        btnSendComment.isEnabled = true

                        // Hide keyboard
                        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                        imm.hideSoftInputFromWindow(etComment.windowToken, 0)
                    }
                    .addOnFailureListener { e ->
                        if (!isAdded) return@addOnFailureListener

                        btnSendComment.isEnabled = true
                        Toast.makeText(context, "Failed to post comment: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                btnSendComment.isEnabled = true
            }
    }
    private fun updateCommentCount() {
        db.collection("posts").document(postId)
            .collection("comments")
            .get()
            .addOnSuccessListener { snapshot ->
                val count = snapshot.size()
                db.collection("posts").document(postId)
                    .update("commentCount", count)
            }
    }
}