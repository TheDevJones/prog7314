package com.st10028374.vitality_vault.routes.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.st10028374.vitality_vault.R
import com.st10028374.vitality_vault.main.NotificationTriggers
import com.st10028374.vitality_vault.main.Post
import java.text.SimpleDateFormat
import java.util.*

class PostsAdapter(
    private val posts: List<Post>,
    private val currentUserId: String,
    private val onLikeClick: (Post) -> Unit,
    private val onCommentClick: (Post) -> Unit,
    private val onShareClick: (Post) -> Unit,
    private val onProfileClick: (String) -> Unit
) : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    private val db = FirebaseFirestore.getInstance()

    inner class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userAvatar: TextView = view.findViewById(R.id.tvUserAvatar)
        val userName: TextView = view.findViewById(R.id.tvUserName)
        val postTime: TextView = view.findViewById(R.id.tvPostTime)
        val workoutImage: ImageView = view.findViewById(R.id.ivWorkoutImage)
        val workoutType: TextView = view.findViewById(R.id.tvWorkoutType)
        val workoutStats: TextView = view.findViewById(R.id.tvWorkoutStats)
        val caption: TextView = view.findViewById(R.id.tvCaption)
        val btnLike: ImageButton = view.findViewById(R.id.btnLike)
        val btnComment: ImageButton = view.findViewById(R.id.btnComment)
        val btnShare: ImageButton = view.findViewById(R.id.btnShare)
        val tvLikeCount: TextView = view.findViewById(R.id.tvLikeCount)
        val tvCommentCount: TextView = view.findViewById(R.id.tvCommentCount)
        val xpBadge: TextView = view.findViewById(R.id.tvXpBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        // Set user info
        holder.userName.text = post.userName
        holder.userAvatar.text = post.userName.firstOrNull()?.uppercase() ?: "U"
        holder.postTime.text = getTimeAgo(post.timestamp)

        // Set workout info
        holder.workoutType.text = post.workoutType
        holder.workoutStats.text = buildString {
            if (post.duration.isNotEmpty()) append(post.duration)
            if (post.distance.isNotEmpty()) {
                if (isNotEmpty()) append(" • ")
                append(post.distance)
            }
            if (post.calories.isNotEmpty()) {
                if (isNotEmpty()) append(" • ")
                append(post.calories)
            }
        }

        holder.caption.text = post.caption

        // Set XP badge
        holder.xpBadge.text = "+25 XP"

        // Set like button state
        val isLiked = post.likes.contains(currentUserId)
        holder.btnLike.setImageResource(
            if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart
        )

        // Set counts
        holder.tvLikeCount.text = "${post.likes.size}"
        holder.tvCommentCount.text = "${post.commentCount}"

        // Click listeners
        holder.userAvatar.setOnClickListener { onProfileClick(post.userId) }
        holder.userName.setOnClickListener { onProfileClick(post.userId) }

        holder.btnLike.setOnClickListener {
            onLikeClick(post)

            // Trigger notification if not self-like
            if (currentUserId != post.userId) {
                // Get current user's name
                db.collection("users").document(currentUserId)
                    .get()
                    .addOnSuccessListener { doc ->
                        val currentUserName = doc.getString("displayName") ?: "Someone"

                        NotificationTriggers.sendLikeNotification(
                            context = holder.itemView.context,
                            postOwnerId = post.userId,
                            likerUserId = currentUserId,
                            likerUserName = currentUserName,
                            postId = post.id
                        )
                    }
            }
        }

        holder.btnComment.setOnClickListener { onCommentClick(post) }
        holder.btnShare.setOnClickListener { onShareClick(post) }
    }

    override fun getItemCount() = posts.size

    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            diff < 604800000 -> "${diff / 86400000}d ago"
            else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
        }
    }
}