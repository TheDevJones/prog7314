package com.st10028374.vitality_vault.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.st10028374.vitality_vault.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Firebase Cloud Messaging service for handling push notifications
 */
class VitalityVaultMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
    }

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Called when a new FCM token is generated
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")

        // Save token locally
        val prefs = getSharedPreferences("VitalityVaultPrefs", MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()

        // Send token to Firestore for this user
        sendTokenToFirestore(token)
    }

    /**
     * Called when a message is received
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "Message received from: ${message.from}")

        // Handle data payload
        if (message.data.isNotEmpty()) {
            Log.d(TAG, "Message data: ${message.data}")
            handleDataPayload(message.data)
        }

        // Handle notification payload
        message.notification?.let {
            Log.d(TAG, "Message notification: ${it.body}")
            handleNotificationPayload(it)
        }
    }

    /**
     * Handle notification data payload
     */
    private fun handleDataPayload(data: Map<String, String>) {
        val type = data["type"] ?: return

        when (type) {
            "workout_complete" -> {
                NotificationHelper.showWorkoutCompleteNotification(
                    context = this,
                    workoutType = data["workoutType"] ?: "Workout",
                    duration = data["duration"] ?: "0 min",
                    calories = data["calories"] ?: "0 kcal"
                )
            }

            "workout_milestone" -> {
                NotificationHelper.showWorkoutMilestoneNotification(
                    context = this,
                    milestone = data["milestone"] ?: "Milestone reached",
                    message = data["message"] ?: "Great job!"
                )
            }

            "new_like" -> {
                NotificationHelper.showNewLikeNotification(
                    context = this,
                    userName = data["userName"] ?: "Someone",
                    postType = data["postType"] ?: "post"
                )
            }

            "new_comment" -> {
                NotificationHelper.showNewCommentNotification(
                    context = this,
                    userName = data["userName"] ?: "Someone",
                    comment = data["comment"] ?: "Commented on your post",
                    postType = data["postType"] ?: "post"
                )
            }

            "new_follower" -> {
                NotificationHelper.showNewFollowerNotification(
                    context = this,
                    userName = data["userName"] ?: "Someone"
                )
            }

            "new_post" -> {
                NotificationHelper.showNewPostNotification(
                    context = this,
                    userName = data["userName"] ?: "Someone",
                    postType = data["postType"] ?: "workout"
                )
            }

            "achievement" -> {
                NotificationHelper.showAchievementNotification(
                    context = this,
                    achievementTitle = data["title"] ?: "Achievement Unlocked",
                    achievementDescription = data["description"] ?: "Congratulations!",
                    xpEarned = data["xp"]?.toIntOrNull() ?: 0
                )
            }

            "daily_reminder" -> {
                NotificationHelper.showDailyReminderNotification(
                    context = this,
                    message = data["message"] ?: "Time to workout!"
                )
            }
        }
    }

    /**
     * Handle notification payload (when app is in foreground)
     */
    private fun handleNotificationPayload(notification: RemoteMessage.Notification) {
        NotificationHelper.showCustomNotification(
            context = this,
            channelId = "social_channel",
            notificationId = System.currentTimeMillis().toInt(),
            title = notification.title ?: "Vitality Vault",
            message = notification.body ?: ""
        )
    }

    /**
     * Send FCM token to Firestore
     */
    private fun sendTokenToFirestore(token: String) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .update("fcmToken", token)
            .addOnSuccessListener {
                Log.d(TAG, "FCM token saved to Firestore")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save FCM token to Firestore", e)
            }
    }
}