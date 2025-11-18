package com.st10028374.vitality_vault.auth

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
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.st10028374.vitality_vault.R
import com.st10028374.vitality_vault.databinding.ActivitySignUpBinding
import com.st10028374.vitality_vault.main.MainActivity
import com.st10028374.vitality_vault.utils.BiometricHelper
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.*

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var credentialManager: CredentialManager
    private lateinit var biometricHelper: BiometricHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        credentialManager = CredentialManager.create(this)
        biometricHelper = BiometricHelper(this)

        setTabSelected(false)

        binding.btnCreateAccount.setOnClickListener { handleEmailSignUp() }
        binding.btnSignUpScreenGoogleSignIn.setOnClickListener { signUpWithGoogle() }
        binding.btnSignUpScreenGithubSignIn.setOnClickListener { signUpWithGitHub() }
        binding.btnSignUpSignIn.setOnClickListener {
            setTabSelected(true)
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    /**Email/Password*/
    private fun handleEmailSignUp() {
        val email = binding.etSignUpEmail.text.toString().trim()
        val password = binding.etSignUpPassword.text.toString().trim()
        val confirmPassword = binding.etSignUpConfirmPassword.text.toString().trim()
        val displayName = binding.etSignUpDisplayName.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || displayName.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            return
        }

        if (!password.matches(Regex("\\d{6,8}"))) {
            Toast.makeText(this, "Password must be numeric and 6–8 digits long", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser ?: return@addOnCompleteListener
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()

                user.updateProfile(profileUpdates).addOnCompleteListener {
                    saveUserToFirestore(user.uid, displayName, email)
                }
            } else {
                if (task.exception is FirebaseAuthUserCollisionException) {
                    Toast.makeText(this, "User already exists. Please sign in.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Sign up failed. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**Google SSO*/
    private fun signUpWithGoogle() {
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
                val result = credentialManager.getCredential(this@SignUpActivity, request)
                handleGoogleSignUpResult(result)
            } catch (_: GetCredentialException) {
                Toast.makeText(this@SignUpActivity, "Google sign-in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleGoogleSignUpResult(result: GetCredentialResponse) {
        try {
            val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
            val idToken = credential.idToken
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

            auth.signInWithCredential(firebaseCredential).addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    saveUserToFirestore(
                        user?.uid ?: return@addOnCompleteListener,
                        user.displayName ?: "Anonymous",
                        user.email ?: ""
                    )
                } else {
                    if (task.exception is FirebaseAuthUserCollisionException) {
                        Toast.makeText(this, "User already exists. Please sign in.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Authentication failed. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (_: GoogleIdTokenParsingException) {
            Toast.makeText(this, "Invalid Google ID token", Toast.LENGTH_SHORT).show()
        }
    }

    /**GitHub SSO*/
    private fun signUpWithGitHub() {
        val provider = OAuthProvider.newBuilder("github.com")
            .setScopes(listOf("user:email"))
            .build()

        auth.startActivityForSignInWithProvider(this, provider)
            .addOnSuccessListener { result ->
                val user = result.user
                saveUserToFirestore(
                    user?.uid ?: return@addOnSuccessListener,
                    user?.displayName ?: user?.email?.substringBefore('@') ?: "GitHub User",
                    user?.email ?: ""
                )
            }
            .addOnFailureListener {
                Toast.makeText(this, "GitHub sign-in failed", Toast.LENGTH_SHORT).show()
            }
    }

    /**Helpers*/
    private fun saveUserToFirestore(uid: String, displayName: String, email: String) {
        val username = displayName.replace(" ", "_") + System.currentTimeMillis()
        val userData = hashMapOf(
            "username" to username,
            "displayName" to displayName,
            "email" to email
        )

        db.collection("users").document(uid).set(userData)
            .addOnSuccessListener {
                Toast.makeText(this, "Sign up successful!", Toast.LENGTH_SHORT).show()
                checkBiometricAndNavigate()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show()
            }
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
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun sha256(input: String): String {
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    private fun setTabSelected(isLogin: Boolean) {
        val selectedBg = ContextCompat.getDrawable(this, R.drawable.tab_selected_background)
        val unselectedBg = ContextCompat.getDrawable(this, android.R.color.transparent)

        binding.btnSignUpSignIn.background = if (isLogin) selectedBg else unselectedBg
        binding.btnSignUpScreenSignUp.background = if (!isLogin) selectedBg else unselectedBg
    }
}