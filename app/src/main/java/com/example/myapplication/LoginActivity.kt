package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.databinding.ActivityLoginBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val TAG = "LoginActivity"

    @Serializable
    data class Profile(
        val id: String,
        val email: String,
        val account_type: String? = null,
        val is_disabled: Boolean = false
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // Check if user is already logged in
        checkCurrentUser()

        setupClickListeners()
    }

    private fun checkCurrentUser() {
        val session = SupabaseManager.client.auth.currentSessionOrNull()
        if (session != null) {
            val user = session.user
            Log.d(TAG, "User already logged in: ${user?.email}")
            checkAccountType(user?.email ?: "")
        }
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener { handleLogin() }
        binding.createCustomerAccountButton.setOnClickListener { 
            startActivity(Intent(this@LoginActivity, SignUpActivity::class.java))
        }
        binding.createSellerAccountButton.setOnClickListener {
            startActivity(Intent(this@LoginActivity, CreateSellerAccountActivity::class.java))
        }
        binding.forgotPassword.setOnClickListener { handleForgotPassword() }

        // Hidden shortcut to auto-fill moderator credentials
        binding.signInTitle.setOnLongClickListener {
            binding.emailEditText.setText("moderator@thrifty.com")
            binding.passwordEditText.setText("moderator123")
            Toast.makeText(this, "Moderator credentials filled", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun validateLoginForm(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            binding.emailEditText.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEditText.error = "Invalid email format"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.passwordEditText.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.passwordEditText.error = "Password must be at least 6 characters"
            isValid = false
        }

        return isValid
    }

    private fun handleLogin() {
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        Log.d(TAG, "Attempting login for email: $email")

        // Predefined moderator credentials for testing
        if (email == "moderator@thrifty.com" && password == "moderator123") {
            Log.d(TAG, "Predefined moderator login detected")
            startActivity(Intent(this, ModeratorActivity::class.java))
            finish()
            return
        }

        if (validateLoginForm(email, password)) {
            showLoading(true)

            lifecycleScope.launch {
                try {
                    SupabaseManager.client.auth.signInWith(Email) {
                        this.email = email
                        this.password = password
                    }
                    Log.d(TAG, "Supabase Auth successful")
                    checkAccountType(email)
                } catch (e: Exception) {
                    showLoading(false)
                    Log.e(TAG, "Supabase Auth failed", e)
                    Toast.makeText(
                        this@LoginActivity,
                        "Authentication failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun checkAccountType(email: String) {
        Log.d(TAG, "Checking account type for: $email")
        
        lifecycleScope.launch {
            try {
                val profile = SupabaseManager.client.postgrest["profiles"]
                    .select {
                        filter {
                            eq("email", email)
                        }
                    }
                    .decodeSingleOrNull<Profile>()

                if (profile == null) {
                    showLoading(false)
                    Log.e(TAG, "Profile not found for email: $email")
                    Toast.makeText(
                        this@LoginActivity,
                        "Profile not found. Please contact support.",
                        Toast.LENGTH_LONG
                    ).show()
                    SupabaseManager.client.auth.signOut()
                    return@launch
                }

                showLoading(false)
                Log.d(TAG, "Profile found: $profile")

                if (profile.is_disabled) {
                    Toast.makeText(
                        this@LoginActivity,
                        "This account has been disabled. Please contact support for assistance.",
                        Toast.LENGTH_LONG
                    ).show()
                    SupabaseManager.client.auth.signOut()
                    return@launch
                }

                when (profile.account_type) {
                    "moderator" -> {
                        startActivity(Intent(this@LoginActivity, ModeratorActivity::class.java))
                        finish()
                    }
                    "seller" -> {
                        val intent = Intent(this@LoginActivity, SellerMainActivity::class.java)
                        intent.putExtra("accountType", "seller")
                        intent.putExtra("email", email)
                        startActivity(intent)
                        finish()
                    }
                    else -> {
                        val intent = Intent(this@LoginActivity, LandingActivity::class.java)
                        intent.putExtra("accountType", profile.account_type)
                        intent.putExtra("email", email)
                        startActivity(intent)
                        finish()
                    }
                }
            } catch (e: Exception) {
                showLoading(false)
                Log.e(TAG, "Error checking profile", e)
                Toast.makeText(
                    this@LoginActivity,
                    "Error checking account type: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                SupabaseManager.client.auth.signOut()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.loginButton.isEnabled = !show
    }

    private fun handleForgotPassword() {
        startActivity(Intent(this@LoginActivity, ForgotPasswordActivity::class.java))
    }
}