package com.st10028374.vitality_vault.main

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.st10028374.vitality_vault.utils.NotificationHelper

object NotificationTriggers {

    private const val TAG = "NotificationTriggers"
    private val db = FirebaseFirestore.getInstance()

    /**
     * Send notification when someone likes a post
     */
    fun sendLikeNotification(
        context: Context,
        postOwnerId: String,
        likerUserId: String,
        likerUserName: String,
        postId: String
    ) {
        // Don't notify if user liked their own post
        if (postOwnerId == likerUserId) return

        // Get post owner's FCM token
        db.collection("users").document(postOwnerId)
            .get()
            .addOnSuccessListener { document ->
                val fcmToken = document.getString("fcmToken")
                if (fcmToken != null) {
                    // Send FCM notification via your backend
                    // For now, show local notification if this is the current device
                    NotificationHelper.showNewLikeNotification(
                        context = context,
                        userName = likerUserName,
                        postType = "post"
                    )
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get FCM token", e)
            }
    }

    /**
     * Send notification when someone comments on a post
     */
    fun sendCommentNotification(
        context: Context,
        postOwnerId: String,
        commenterUserId: String,
        commenterUserName: String,
        comment: String,
        postId: String
    ) {
        // Don't notify if user commented on their own post
        if (postOwnerId == commenterUserId) return

        // Get post owner's FCM token
        db.collection("users").document(postOwnerId)
            .get()
            .addOnSuccessListener { document ->
                val fcmToken = document.getString("fcmToken")
                if (fcmToken != null) {
                    // Show local notification
                    NotificationHelper.showNewCommentNotification(
                        context = context,
                        userName = commenterUserName,
                        comment = comment,
                        postType = "post"
                    )
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get FCM token", e)
            }
    }

    /**
     * Send notification when someone follows you
     */
    fun sendFollowerNotification(
        context: Context,
        followedUserId: String,
        followerUserId: String,
        followerUserName: String
    ) {
        // Don't notify if user followed themselves
        if (followedUserId == followerUserId) return

        // Get followed user's FCM token
        db.collection("users").document(followedUserId)
            .get()
            .addOnSuccessListener { document ->
                val fcmToken = document.getString("fcmToken")
                if (fcmToken != null) {
                    // Show local notification
                    NotificationHelper.showNewFollowerNotification(
                        context = context,
                        userName = followerUserName
                    )
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get FCM token", e)
            }
    }

    /**
     * Send notification when someone you follow posts
     */
    fun sendNewPostNotification(
        context: Context,
        posterId: String,
        posterUserName: String,
        postType: String
    ) {
        // Get all followers of this user
        db.collection("followers")
            .whereEqualTo("followingId", posterId)
            .get()
            .addOnSuccessListener { documents ->
                documents.forEach { doc ->
                    val followerId = doc.getString("followerId")
                    if (followerId != null) {
                        // Get follower's FCM token
                        db.collection("users").document(followerId)
                            .get()
                            .addOnSuccessListener { userDoc ->
                                val fcmToken = userDoc.getString("fcmToken")
                                if (fcmToken != null) {
                                    // Show notification
                                    NotificationHelper.showNewPostNotification(
                                        context = context,
                                        userName = posterUserName,
                                        postType = postType
                                    )
                                }
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get followers", e)
            }
    }
}