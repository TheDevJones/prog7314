package com.st10028374.vitality_vault.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast

object SystemThemeHelper {

    /**
     * Open system display settings where user can change dark mode
     */
    fun openSystemDisplaySettings(context: Context) {
        try {
            // Try to open dark mode settings directly (Android 10+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                // For older versions, open general display settings
                val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Unable to open display settings",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Check if system is in dark mode
     */
    fun isSystemInDarkMode(context: Context): Boolean {
        val currentNightMode = context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * Get current system theme name
     */
    fun getCurrentSystemTheme(context: Context): String {
        return if (isSystemInDarkMode(context)) {
            "Dark"
        } else {
            "Light"
        }
    }

    /**
     * Get theme index for spinner (0=Dark, 1=Light)
     */
    fun getCurrentThemeIndex(context: Context): Int {
        return if (isSystemInDarkMode(context)) 0 else 1
    }
}