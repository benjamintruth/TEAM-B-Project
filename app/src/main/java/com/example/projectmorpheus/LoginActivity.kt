package com.example.projectmorpheus

// Imports //
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.example.projectmorpheus.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.userProfileChangeRequest


import kotlinx.coroutines.launch
import android.view.inputmethod.EditorInfo
import java.util.regex.Pattern

class LoginActivity : AppCompatActivity() {
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private lateinit var binding: ActivityLoginBinding
    private var isSignUp = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Skip login if user already logged in
        firebaseAuth.currentUser?.let {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // Toggle between Login and Sign Up
        binding.toggleSignUpTextView.setOnClickListener {
            isSignUp = !isSignUp
            binding.emailActionButton.text = if (isSignUp) "Sign Up" else "Login"
            binding.toggleSignUpTextView.text =
                if (isSignUp) "Already have an Account? Login"
                else "Don't have an account? Sign Up"
        }

        // Press enter on password submits form
        binding.passwordEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.emailActionButton.performClick()
                true
            } else false
        }

        // Email login/sign-up button
        binding.emailActionButton.setOnClickListener {
            clearErrorMessage()
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString()

            if (!validateInputs(email, password)) return@setOnClickListener

            if (isSignUp) createAccount(email, password)
            else signInWithEmail(email, password)
        }

        // Google Sign-In
        binding.googleSignInButton.setOnClickListener { signInWithGoogle() }
    }

    // Input validation
    private fun validateInputs(email: String, password: String): Boolean {
        val emailPattern =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")

        return when {
            email.isEmpty() -> {
                showError("Email is required"); false
            }
            !emailPattern.matcher(email).matches() -> {
                showError("Enter a valid email address"); false
            }
            password.isEmpty() -> {
                showError("Password is required"); false
            }
            password.length < 6 -> {
                showError("Password must be at least 6 characters long"); false
            }
            else -> true
        }
    }

    // Google sign-in
    private fun signInWithGoogle() {
        lifecycleScope.launch {
            val credentialManager = CredentialManager.create(this@LoginActivity)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId("411527177263-bd1afb8rgsshhuk44ue2kgf5rmbgq3rm.apps.googleusercontent.com")
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            try {
                val result = credentialManager.getCredential(this@LoginActivity, request)
                val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
                val idToken = credential.idToken
                firebaseAuthWithGoogle(idToken)
            } catch (e: GetCredentialException) {
                showError("Google sign-in failed. Please try again.")
                Log.e("GoogleSignIn", "Failure", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String?) {
        if (idToken == null) return
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    clearErrorMessage()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    firebaseError(task.exception)
                }
            }
    }

    //  Updated: Create account & save display name
    private fun createAccount(email: String, password: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    val nameFromEmail = email.substringBefore('@').replaceFirstChar { it.uppercaseChar() }

                    // Set name to Firebase profile
                    val profileUpdates = userProfileChangeRequest {
                        displayName = nameFromEmail
                    }
                    user?.updateProfile(profileUpdates)

                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    firebaseError(task.exception)
                }
            }
    }

    // Sign in
    private fun signInWithEmail(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    clearErrorMessage()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    firebaseError(task.exception)
                }
            }
    }

    // Firebase error handler
    private fun firebaseError(exception: Exception?) {
        val message = when (exception) {
            is FirebaseAuthInvalidUserException ->
                "No account found with this email."
            is FirebaseAuthInvalidCredentialsException ->
                "Invalid password. Please check and try again."
            is FirebaseAuthUserCollisionException ->
                "This email is already registered. Please log in."
            is FirebaseAuthWeakPasswordException ->
                "Password must be at least 6 characters long."
            else ->
                exception?.localizedMessage ?: "Authentication failed. Please try again."
        }
        showError(message)
        Log.e("FirebaseAuthError", "Error: ${exception?.message}", exception)
    }

    private fun showError(message: String) {
        binding.errorTextView.text = message
        binding.errorTextView.alpha = 1f
    }

    private fun clearErrorMessage() {
        binding.errorTextView.text = ""
    }
}
