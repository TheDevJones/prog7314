package com.st10028374.vitality_vault.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.st10028374.vitality_vault.R
import java.text.SimpleDateFormat
import java.util.*

class CommentsAdapter(
    private val comments: List<Comment>
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    inner class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userAvatar: TextView = view.findViewById(R.id.tvCommentAvatar)
        val userName: TextView = view.findViewById(R.id.tvCommentUserName)
        val commentText: TextView = view.findViewById(R.id.tvCommentText)
        val timestamp: TextView = view.findViewById(R.id.tvCommentTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]

        holder.userName.text = comment.userName
        holder.userAvatar.text = comment.userName.firstOrNull()?.uppercase() ?: "U"
        holder.commentText.text = comment.comment
        holder.timestamp.text = getTimeAgo(comment.timestamp)
    }

    override fun getItemCount() = comments.size

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