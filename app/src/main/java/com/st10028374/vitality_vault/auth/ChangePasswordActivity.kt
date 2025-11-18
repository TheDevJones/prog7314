package com.st10028374.vitality_vault.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.st10028374.vitality_vault.databinding.ActivitySignUpBinding

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupUI()
        loadUserData()
    }

    private fun setupUI() {
        // Hide social login buttons and tab container
        binding.llSocialButtons.visibility = View.GONE
        binding.llTabContainer.visibility = View.GONE
        binding.tvSignUpOrDivider.visibility = View.GONE

        // Change the subtitle text
        binding.tvSignUpSubtitle.text = "Update your account password"

        // Update button text
        binding.btnCreateAccount.text = "Update Password"

        // Update label for confirm password
        binding.tvSignUpConfirmPasswordLabel.text = "New Password"
        binding.etSignUpConfirmPassword.hint = "Enter new password"

        // Update password label to "Current Password"
        binding.tvSignUpPasswordLabel.text = "Current Password"
        binding.etSignUpPassword.hint = "Enter current password"

        // Disable editing for email, username, and display name
        binding.etSignUpEmail.isEnabled = false
        binding.etSignUpUsername.isEnabled = false
        binding.etSignUpDisplayName.isEnabled = false

        // Make disabled fields look different
        binding.etSignUpEmail.alpha = 0.6f
        binding.etSignUpUsername.alpha = 0.6f
        binding.etSignUpDisplayName.alpha = 0.6f

        // Handle password change
        binding.btnCreateAccount.setOnClickListener {
            handlePasswordChange()
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Check if user signed in with email/password
        val isEmailPasswordUser = currentUser.providerData.any {
            it.providerId == "password"
        }

        if (!isEmailPasswordUser) {
            Toast.makeText(
                this,
                "Password change is only available for email/password accounts",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        // Load email from Firebase Auth
        binding.etSignUpEmail.setText(currentUser.email ?: "")

        // Load username and display name from Firestore
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val username = document.getString("username") ?: ""
                    val displayName = document.getString("displayName") ?: ""

                    binding.etSignUpUsername.setText(username)
                    binding.etSignUpDisplayName.setText(displayName)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun handlePasswordChange() {
        val currentPassword = binding.etSignUpPassword.text.toString().trim()
        val newPassword = binding.etSignUpConfirmPassword.text.toString().trim()

        if (currentPassword.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, "Both password fields are required", Toast.LENGTH_SHORT).show()
            return
        }

        if (!newPassword.matches(Regex("\\d{6,8}"))) {
            Toast.makeText(
                this,
                "New password must be numeric and 6–8 digits long",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (currentPassword == newPassword) {
            Toast.makeText(
                this,
                "New password must be different from current password",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val user = auth.currentUser
        val email = user?.email

        if (user == null || email == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        // Re-authenticate user with current password
        val credential = EmailAuthProvider.getCredential(email, currentPassword)

        user.reauthenticate(credential)
            .addOnSuccessListener {
                // Update password
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Password updated successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Failed to update password: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Current password is incorrect",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}