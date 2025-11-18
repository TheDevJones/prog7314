package com.st10028374.vitality_vault.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.st10028374.vitality_vault.utils.NotificationHelper

/**
 * Broadcast receiver for scheduled notifications (e.g., daily reminders)
 */
class NotificationBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationReceiver"
        const val ACTION_DAILY_REMINDER = "com.st10028374.vitality_vault.DAILY_REMINDER"
        const val ACTION_WORKOUT_REMINDER = "com.st10028374.vitality_vault.WORKOUT_REMINDER"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        Log.d(TAG, "Received broadcast: ${intent.action}")

        when (intent.action) {
            ACTION_DAILY_REMINDER -> {
                val message = intent.getStringExtra("message")
                    ?: "Don't forget your daily workout! 💪"

                NotificationHelper.showDailyReminderNotification(context, message)
            }

            ACTION_WORKOUT_REMINDER -> {
                val workoutType = intent.getStringExtra("workoutType") ?: "workout"
                val message = "Time for your $workoutType! Let's get moving 🏃"

                NotificationHelper.showDailyReminderNotification(context, message)
            }
        }
    }
}