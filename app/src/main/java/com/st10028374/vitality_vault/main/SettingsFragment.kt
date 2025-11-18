package com.st10028374.vitality_vault.main

import android.net.Uri
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.st10028374.vitality_vault.R
import com.st10028374.vitality_vault.auth.ChangePasswordActivity
import com.st10028374.vitality_vault.auth.LoginActivity
import com.st10028374.vitality_vault.database.VitalityVaultDatabase
import com.st10028374.vitality_vault.databinding.FragmentSettingsBinding
import com.st10028374.vitality_vault.utils.BiometricHelper
import com.st10028374.vitality_vault.utils.SystemThemeHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val sharedPreferences by lazy {
        requireContext().getSharedPreferences("VitalityVaultPrefs", Context.MODE_PRIVATE)
    }

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private lateinit var biometricHelper: BiometricHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        biometricHelper = BiometricHelper(requireContext())

        displayUserInfo()
        setupAppLock()
        setupBiometric()
        setupOtherSwitches()
        setupChangePassword()
        setupHelpCenter()
        setupSystemThemeSpinner()
        setupDeleteAccount()

        binding.btnResetSettings.setOnClickListener {
            // Turn off all switches in UI
            binding.switchAppLock.isChecked = false
            binding.switchBiometric.isChecked = false
            binding.switchShareWorkouts.isChecked = false
            binding.switchAllowComments.isChecked = false
            binding.switchHapticFeedback.isChecked = false

            // Reset all local settings in SharedPreferences
            sharedPreferences.edit()
                .putBoolean("app_lock_enabled", false)
                .putBoolean("biometric_enabled", false)
                .putBoolean("share_workouts_enabled", false)
                .putBoolean("allow_comments_enabled", false)
                .putBoolean("haptic_feedback_enabled", false)
                .apply()

            // Reset biometric helper
            biometricHelper.setBiometricEnabled(false)

            Toast.makeText(requireContext(), "Settings have been reset to default", Toast.LENGTH_SHORT).show()
        }
    }

    /** Display the user's display name and email */
    private fun displayUserInfo() {
        val user = auth.currentUser
        binding.tvUsername.text = user?.displayName ?: "User"
        binding.tvEmail.text = user?.email ?: "user@example.com"

        val firstLetter = (user?.displayName?.firstOrNull() ?: user?.email?.firstOrNull() ?: 'U')
        binding.tvAvatar.text = firstLetter.toString().uppercase()
    }

    private fun setupAppLock() {
        val isAppLockEnabled = sharedPreferences.getBoolean("app_lock_enabled", false)
        binding.switchAppLock.isChecked = isAppLockEnabled

        binding.switchAppLock.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit()
                .putBoolean("app_lock_enabled", isChecked)
                .apply()

            val message = if (isChecked) {
                "App Lock enabled - Authentication required on next launch"
            } else {
                "App Lock disabled"
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBiometric() {
        val isBiometricAvailable = biometricHelper.isBiometricAvailable()
        val isBiometricEnabled = biometricHelper.isBiometricEnabled()
        binding.switchBiometric.isChecked = isBiometricEnabled

        if (!isBiometricAvailable) {
            binding.switchBiometric.isEnabled = false
            Toast.makeText(requireContext(), biometricHelper.getBiometricStatus(), Toast.LENGTH_SHORT).show()
        }

        binding.switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                biometricHelper.showBiometricPrompt(
                    activity = requireActivity(),
                    title = "Enable Biometric Authentication",
                    subtitle = "Verify your biometric credential",
                    negativeButtonText = "Cancel",
                    onSuccess = {
                        biometricHelper.setBiometricEnabled(true)
                        sharedPreferences.edit().putBoolean("biometric_enabled", true).apply()
                        Toast.makeText(requireContext(), "Biometric authentication enabled successfully", Toast.LENGTH_SHORT).show()
                    },
                    onError = { error ->
                        binding.switchBiometric.isChecked = false
                        Toast.makeText(requireContext(), "Failed to enable biometric: $error", Toast.LENGTH_SHORT).show()
                    },
                    onFailed = {
                        binding.switchBiometric.isChecked = false
                        Toast.makeText(requireContext(), "Biometric authentication failed", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                biometricHelper.setBiometricEnabled(false)
                sharedPreferences.edit().putBoolean("biometric_enabled", false).apply()
                Toast.makeText(requireContext(), "Biometric authentication disabled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Persist other switches: Share Workouts, Allow Comments, Haptic Feedback */
    private fun setupOtherSwitches() {
        val isHapticEnabled = sharedPreferences.getBoolean("haptic_feedback_enabled", true)
        binding.switchHapticFeedback.isChecked = isHapticEnabled
        binding.switchHapticFeedback.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("haptic_feedback_enabled", isChecked).apply()
        }

        val isShareEnabled = sharedPreferences.getBoolean("share_workouts_enabled", true)
        binding.switchShareWorkouts.isChecked = isShareEnabled
        binding.switchShareWorkouts.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("share_workouts_enabled", isChecked).apply()
        }

        val isCommentsEnabled = sharedPreferences.getBoolean("allow_comments_enabled", true)
        binding.switchAllowComments.isChecked = isCommentsEnabled
        binding.switchAllowComments.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("allow_comments_enabled", isChecked).apply()
        }
    }

    private fun setupChangePassword() {
        binding.btnChangePassword.setOnClickListener {
            startActivity(Intent(requireContext(), ChangePasswordActivity::class.java))
        }
    }

    private fun setupHelpCenter() {
        binding.btnHelpCenter.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://cathe.com/forum/")))
        }
    }

    private fun setupSystemThemeSpinner() {
        updateThemeSpinner()
        binding.spinnerTheme.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val currentThemeIndex = SystemThemeHelper.getCurrentThemeIndex(requireContext())
                if (position != currentThemeIndex) {
                    Toast.makeText(requireContext(), "Opening system display settings to change theme...", Toast.LENGTH_LONG).show()
                    SystemThemeHelper.openSystemDisplaySettings(requireContext())
                    binding.spinnerTheme.setSelection(currentThemeIndex)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateThemeSpinner() {
        val currentThemeIndex = SystemThemeHelper.getCurrentThemeIndex(requireContext())
        binding.spinnerTheme.setSelection(currentThemeIndex)
    }

    private fun setupDeleteAccount() {
        binding.btnDeleteAccount.setOnClickListener { showDeleteAccountConfirmation() }
    }

    private fun showDeleteAccountConfirmation() {
        val user = auth.currentUser
        val isEmailPasswordUser = user?.providerData?.any { it.providerId == "password" } ?: false

        AlertDialog.Builder(requireContext())
            .setTitle("Delete Account")
            .setMessage(
                "Are you sure you want to permanently delete your account?\n\n" +
                        "This will:\n" +
                        "• Delete all your workouts\n" +
                        "• Delete all your routes\n" +
                        "• Remove all your data\n" +
                        "• Sign you out\n\n" +
                        "This action CANNOT be undone!"
            )
            .setPositiveButton("Delete") { _, _ ->
                if (isEmailPasswordUser) showReauthenticationDialog() else deleteAccount()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showReauthenticationDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_reauthenticate, null)
        val etPassword = dialogView.findViewById<EditText>(R.id.etPassword)

        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Your Identity")
            .setMessage("Please enter your password to confirm account deletion:")
            .setView(dialogView)
            .setPositiveButton("Confirm") { dialog, _ ->
                val password = etPassword.text.toString()
                if (password.isEmpty()) {
                    Toast.makeText(requireContext(), "Password is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                reauthenticateAndDelete(password)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .show()
    }

    private fun reauthenticateAndDelete(password: String) {
        val user = auth.currentUser ?: return
        val email = user.email ?: return

        lifecycleScope.launch {
            try {
                showLoadingDialog()
                val credential = EmailAuthProvider.getCredential(email, password)
                user.reauthenticate(credential).await()
                deleteAccount()
            } catch (e: Exception) {
                dismissLoadingDialog()
                Toast.makeText(requireContext(), "Authentication failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun deleteAccount() {
        val user = auth.currentUser ?: return
        val userId = user.uid

        lifecycleScope.launch {
            try {
                showLoadingDialog()
                deleteFirestoreData(userId)
                deleteLocalData(userId)
                user.delete().await()
                dismissLoadingDialog()
                Toast.makeText(requireContext(), "Account deleted successfully", Toast.LENGTH_LONG).show()
                navigateToLogin()
            } catch (e: Exception) {
                dismissLoadingDialog()
                Toast.makeText(requireContext(), "Failed to delete account: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun deleteFirestoreData(userId: String) {
        try {
            firestore.collection("workouts").whereEqualTo("userId", userId).get().await().documents.forEach { it.reference.delete().await() }
            firestore.collection("routes").whereEqualTo("userId", userId).get().await().documents.forEach { it.reference.delete().await() }
            firestore.collection("users").document(userId).delete().await()
        } catch (e: Exception) {
            android.util.Log.e("DeleteAccount", "Error deleting Firestore data", e)
        }
    }

    private suspend fun deleteLocalData(userId: String) {
        try {
            val database = VitalityVaultDatabase.getDatabase(requireContext())
            database.workoutDao().getAllWorkouts(userId).forEach { database.workoutDao().deleteWorkout(it.id) }
            database.routeDao().getAllRoutes(userId).forEach { database.routeDao().deleteRoute(it.id) }
            sharedPreferences.edit().clear().apply()
            requireContext().getSharedPreferences("BiometricPrefs", Context.MODE_PRIVATE).edit().clear().apply()
        } catch (e: Exception) {
            android.util.Log.e("DeleteAccount", "Error deleting local data", e)
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
    }

    private var loadingDialog: AlertDialog? = null
    private fun showLoadingDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_loading, null)
        builder.setView(dialogView).setCancelable(false)
        loadingDialog = builder.create()
        loadingDialog?.show()
    }
    private fun dismissLoadingDialog() { loadingDialog?.dismiss(); loadingDialog = null }

    override fun onResume() {
        super.onResume()
        updateThemeSpinner()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
