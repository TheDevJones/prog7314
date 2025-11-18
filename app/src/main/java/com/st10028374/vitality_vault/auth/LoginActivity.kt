package com.st10028374.vitality_vault.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.st10028374.vitality_vault.R
import com.st10028374.vitality_vault.databinding.ActivityLoginBinding
import com.st10028374.vitality_vault.main.MainActivity
import com.st10028374.vitality_vault.main.SpotifyManager
import com.st10028374.vitality_vault.utils.BiometricHelper
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var credentialManager: CredentialManager
    private lateinit var biometricHelper: BiometricHelper

    private fun sha256(input: String): String {
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        biometricHelper = BiometricHelper(this)

        // Check authentication status
        val sharedPrefs = getSharedPreferences("VitalityVaultPrefs", Context.MODE_PRIVATE)
        val isAppLockEnabled = sharedPrefs.getBoolean("app_lock_enabled", false)
        val currentUser = auth.currentUser

        // If user is logged in
        if (currentUser != null) {
            // If biometric is enabled, redirect to biometric auth
            if (biometricHelper.isBiometricEnabled() && biometricHelper.isBiometricAvailable()) {
                navigateToBiometric()
                return
            }
            // If App Lock is disabled, go directly to home
            else if (!isAppLockEnabled) {
                navigateToHome()
                return
            }
            // Otherwise stay on login for app lock
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        credentialManager = CredentialManager.create(this)

        // Initialize tab selection
        setTabSelected(true)

        // Tab switching
        binding.btnLoginSignIn.setOnClickListener { setTabSelected(true) }
        binding.btnLoginSignUp.setOnClickListener {
            setTabSelected(false)
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }

        // Email login
        binding.btnLoginMainSignIn.setOnClickListener {
            handleEmailLogin()
        }

        // Google SSO
        binding.btnLoginGoogleSignIn.setOnClickListener {
            loginWithGoogle()
        }

        // GitHub SSO
        binding.btnLoginGithubSignIn.setOnClickListener {
            loginWithGitHub()
        }
    }

    private fun handleEmailLogin() {
        val email = binding.etLoginEmail.text.toString().trim()
        val password = binding.etLoginPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and password are required", Toast.LENGTH_SHORT).show()
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                    checkBiometricAndNavigate()
                } else {
                    Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loginWithGoogle() {
        val rawNonce = UUID.randomUUID().toString()
        val nonce = sha256(rawNonce)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id))
            .setNonce(nonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = this@LoginActivity,
                    request = request
                )
                handleGoogleLoginResult(result)
            } catch (_: GetCredentialException) {
                Toast.makeText(this@LoginActivity, "Google login failed, please try again", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleGoogleLoginResult(result: GetCredentialResponse) {
        try {
            val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
            val idToken = credential.idToken

            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(firebaseCredential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        saveUserData(user?.uid, user?.displayName ?: "Anonymous", user?.email ?: "")
                        Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                        checkBiometricAndNavigate()
                    } else {
                        Toast.makeText(this, "Google login failed, please try again", Toast.LENGTH_SHORT).show()
                    }
                }
        } catch (_: GoogleIdTokenParsingException) {
            Toast.makeText(this, "Google login failed, please try again", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loginWithGitHub() {
        val provider = OAuthProvider.newBuilder("github.com")
            .setScopes(listOf("user:email"))
            .build()

        auth.startActivityForSignInWithProvider(this, provider)
            .addOnSuccessListener { result ->
                val user = result.user
                saveUserData(
                    user?.uid,
                    user?.displayName ?: user?.email?.substringBefore('@') ?: "GitHub User",
                    user?.email ?: ""
                )
                Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                checkBiometricAndNavigate()
            }
            .addOnFailureListener {
                Toast.makeText(this, "GitHub login failed, please try again", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserData(uid: String?, displayName: String, email: String) {
        if (uid == null) return
        val username = displayName.replace(" ", "_") + System.currentTimeMillis()
        val userData = hashMapOf(
            "username" to username,
            "displayName" to displayName,
            "email" to email
        )
        db.collection("users").document(uid).set(userData)
    }

    private fun checkBiometricAndNavigate() {
        // Check if biometric is enabled and available
        if (biometricHelper.isBiometricEnabled() && biometricHelper.isBiometricAvailable()) {
            navigateToBiometric()
        } else {
            navigateToHome()
        }
    }

    private fun navigateToBiometric() {
        val intent = Intent(this, BiometricAuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun navigateToHome() {
        SpotifyManager.stopPlaybackAndDisconnect()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun setTabSelected(isLogin: Boolean) {
        val selectedBg = ContextCompat.getDrawable(this, R.drawable.tab_selected_background)
        val unselectedBg = ContextCompat.getDrawable(this, android.R.color.transparent)
        binding.btnLoginSignIn.background = if (isLogin) selectedBg else unselectedBg
        binding.btnLoginSignUp.background = if (!isLogin) selectedBg else unselectedBg
    }
}