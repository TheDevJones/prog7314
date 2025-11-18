package com.st10028374.vitality_vault.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.st10028374.vitality_vault.databinding.ActivityBiometricAuthBinding
import com.st10028374.vitality_vault.main.MainActivity
import com.st10028374.vitality_vault.utils.BiometricHelper

class BiometricAuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBiometricAuthBinding
    private lateinit var biometricHelper: BiometricHelper
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBiometricAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        biometricHelper = BiometricHelper(this)

        // Check if user is logged in
        if (auth.currentUser == null) {
            navigateToLogin()
            return
        }

        setupUI()
        // Automatically trigger biometric prompt
        showBiometricPrompt()
    }

    private fun setupUI() {
        val user = auth.currentUser
        binding.tvUserName.text = user?.displayName ?: "User"
        binding.tvUserEmail.text = user?.email ?: ""

        // Set first letter of name as avatar
        val firstLetter = (user?.displayName?.firstOrNull() ?: user?.email?.firstOrNull() ?: 'U')
        binding.tvAvatar.text = firstLetter.toString().uppercase()

        binding.btnUseBiometric.setOnClickListener {
            showBiometricPrompt()
        }

        binding.btnUsePassword.setOnClickListener {
            // Navigate to login for password authentication
            navigateToLogin()
        }

        binding.btnCancel.setOnClickListener {
            // Sign out and go to login
            auth.signOut()
            biometricHelper.setBiometricEnabled(false)
            navigateToLogin()
        }
    }

    private fun showBiometricPrompt() {
        if (!biometricHelper.isBiometricAvailable()) {
            Toast.makeText(this, biometricHelper.getBiometricStatus(), Toast.LENGTH_LONG).show()
            // If biometric not available, disable it and go to main
            biometricHelper.setBiometricEnabled(false)
            navigateToMain()
            return
        }

        biometricHelper.showBiometricPrompt(
            activity = this,
            title = "Verify it's you",
            subtitle = "Use your biometric credential to access Vitality Vault",
            negativeButtonText = "Use Password",
            onSuccess = {
                Toast.makeText(this, "Authentication successful!", Toast.LENGTH_SHORT).show()
                navigateToMain()
            },
            onError = { error ->
                // Check if user clicked "Use Password" (negative button)
                if (error.contains("Cancel", ignoreCase = true) ||
                    error.contains("negative button", ignoreCase = true) ||
                    error.contains("Authentication canceled", ignoreCase = true)) {
                    // User wants to use password instead
                    navigateToLogin()
                } else {
                    Toast.makeText(this, "Authentication error: $error", Toast.LENGTH_SHORT).show()
                }
            },
            onFailed = {
                Toast.makeText(this, "Authentication failed. Try again.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        // Sign out the user so they have to re-authenticate with password
        auth.signOut()
        biometricHelper.setBiometricEnabled(false)

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        // Prevent back navigation - user must authenticate
        Toast.makeText(this, "Please authenticate to continue", Toast.LENGTH_SHORT).show()
    }
}