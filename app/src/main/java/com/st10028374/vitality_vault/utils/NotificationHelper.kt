package com.st10028374.vitality_vault.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.st10028374.vitality_vault.R
import com.st10028374.vitality_vault.main.MainActivity

/**
 * Helper class for managing app notifications
 */
object NotificationHelper {

    // Notification Channels
    private const val CHANNEL_WORKOUT = "workout_channel"
    private const val CHANNEL_SOCIAL = "social_channel"
    private const val CHANNEL_ACHIEVEMENTS = "achievements_channel"
    private const val CHANNEL_REMINDERS = "reminders_channel"

    // Notification IDs
    private const val NOTIFICATION_WORKOUT_COMPLETE = 1001
    private const val NOTIFICATION_WORKOUT_MILESTONE = 1002
    private const val NOTIFICATION_NEW_LIKE = 2001
    private const val NOTIFICATION_NEW_COMMENT = 2002
    private const val NOTIFICATION_NEW_FOLLOWER = 2003
    private const val NOTIFICATION_ACHIEVEMENT = 3001
    private const val NOTIFICATION_DAILY_REMINDER = 4001

    /**
     * Create notification channels (required for Android O and above)
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Workout Channel
            val workoutChannel = NotificationChannel(
                CHANNEL_WORKOUT,
                "Workout Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for workout progress and completion"
                enableVibration(true)
                setShowBadge(true)
            }

            // Social Channel
            val socialChannel = NotificationChannel(
                CHANNEL_SOCIAL,
                "Social Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for likes, comments, and followers"
                enableVibration(true)
                setShowBadge(true)
            }

            // Achievements Channel
            val achievementsChannel = NotificationChannel(
                CHANNEL_ACHIEVEMENTS,
                "Achievement Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for unlocked achievements and milestones"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }

            // Reminders Channel
            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Reminder Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily workout reminders and motivational messages"
                enableVibration(true)
                setShowBadge(false)
            }

            // Register channels
            notificationManager.createNotificationChannel(workoutChannel)
            notificationManager.createNotificationChannel(socialChannel)
            notificationManager.createNotificationChannel(achievementsChannel)
            notificationManager.createNotificationChannel(remindersChannel)
        }
    }
    /**
     * Show new post notification from someone you follow
     */
    fun showNewPostNotification(
        context: Context,
        userName: String,
        postType: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("openFragment", "social")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SOCIAL)
            .setSmallIcon(R.drawable.ic_steps)
            .setContentTitle("$userName posted a new $postType")
            .setContentText("Check out their latest workout!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(
            System.currentTimeMillis().toInt(),
            notification
        )
    }
    /**
     * Show workout completion notification
     */
    fun showWorkoutCompleteNotification(
        context: Context,
        workoutType: String,
        duration: String,
        calories: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("openFragment", "dashboard")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_WORKOUT)
            .setSmallIcon(R.drawable.ic_steps)
            .setContentTitle("Workout Complete! 🎉")
            .setContentText("$workoutType completed in $duration • $calories burned")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Great job! You completed your $workoutType workout in $duration and burned $calories. Keep up the great work!"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_WORKOUT_COMPLETE, notification)
    }

    /**
     * Show workout milestone notification
     */
    fun showWorkoutMilestoneNotification(
        context: Context,
        milestone: String,
        message: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("openFragment", "dashboard")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_WORKOUT)
            .setSmallIcon(R.drawable.ic_steps)
            .setContentTitle("Milestone Reached! 🎯")
            .setContentText(milestone)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_WORKOUT_MILESTONE, notification)
    }

    /**
     * Show new like notification
     */
    fun showNewLikeNotification(
        context: Context,
        userName: String,
        postType: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("openFragment", "social")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SOCIAL)
            .setSmallIcon(R.drawable.ic_heart)
            .setContentTitle("$userName liked your $postType")
            .setContentText("Tap to view")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_NEW_LIKE, notification)
    }

    /**
     * Show new comment notification
     */
    fun showNewCommentNotification(
        context: Context,
        userName: String,
        comment: String,
        postType: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("openFragment", "social")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SOCIAL)
            .setSmallIcon(R.drawable.ic_comment)
            .setContentTitle("$userName commented on your $postType")
            .setContentText(comment)
            .setStyle(NotificationCompat.BigTextStyle().bigText(comment))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_NEW_COMMENT, notification)
    }

    /**
     * Show new follower notification
     */
    fun showNewFollowerNotification(
        context: Context,
        userName: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("openFragment", "social")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SOCIAL)
            .setSmallIcon(R.drawable.ic_people)
            .setContentTitle("New Follower 👥")
            .setContentText("$userName started following you")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_NEW_FOLLOWER, notification)
    }

    /**
     * Show achievement unlocked notification
     */
    fun showAchievementNotification(
        context: Context,
        achievementTitle: String,
        achievementDescription: String,
        xpEarned: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("openFragment", "dashboard")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ACHIEVEMENTS)
            .setSmallIcon(R.drawable.ic_progress)
            .setContentTitle("Achievement Unlocked! 🏆")
            .setContentText(achievementTitle)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$achievementDescription\n\n+$xpEarned XP earned!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ACHIEVEMENT, notification)
    }

    /**
     * Show daily workout reminder
     */
    fun showDailyReminderNotification(
        context: Context,
        message: String = "Time to move! Start your daily workout now."
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("openFragment", "dashboard")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle("Daily Workout Reminder 💪")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_DAILY_REMINDER, notification)
    }

    /**
     * Show custom notification
     */
    fun showCustomNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        message: String,
        icon: Int = R.drawable.ic_steps
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    /**
     * Cancel a specific notification
     */
    fun cancelNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }
}