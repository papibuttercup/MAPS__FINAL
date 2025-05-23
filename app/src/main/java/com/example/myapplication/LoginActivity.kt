package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityLoginBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // Initialize Firebase Auth
        auth = Firebase.auth
        firestore = FirebaseFirestore.getInstance()

        // Check if user is already logged in
        checkCurrentUser()

        setupClickListeners()
    }

    private fun checkCurrentUser() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d(TAG, "User already logged in: ${currentUser.email}")
            checkAccountType(currentUser.email ?: "")
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
        val rememberMe = binding.rememberMe.isChecked

        Log.d(TAG, "Attempting login for email: $email")

        if (validateLoginForm(email, password)) {
            showLoading(true)

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        Log.d(TAG, "Firebase Auth successful")
                        checkAccountType(email)
                    } else {
                        showLoading(false)
                        Log.e(TAG, "Firebase Auth failed", authTask.exception)
                        Toast.makeText(
                            this,
                            "Authentication failed: ${authTask.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }

    private fun checkAccountType(email: String) {
        Log.d(TAG, "Checking account type for: $email")
        // First check if it's a seller account
        firestore.collection("sellers")
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .addOnSuccessListener { sellerDocuments ->
                Log.d(TAG, "Seller check result - Empty: ${sellerDocuments.isEmpty}")
                if (!sellerDocuments.isEmpty) {
                    val sellerDoc = sellerDocuments.documents[0]
                    val verificationStatus = sellerDoc.getString("verificationStatus")
                    val isDisabled = sellerDoc.getBoolean("isDisabled") ?: false
                    Log.d(TAG, "Seller verification status: $verificationStatus, isDisabled: $isDisabled")
                    
                    if (isDisabled) {
                        showLoading(false)
                        Toast.makeText(
                            this,
                            "This account has been disabled. Please contact support for assistance.",
                            Toast.LENGTH_LONG
                        ).show()
                        auth.signOut()
                        return@addOnSuccessListener
                    }
                    
                    when (verificationStatus) {
                        "pending" -> {
                            showLoading(false)
                            Toast.makeText(
                                this,
                                "Your seller account is pending verification. Please wait for moderator approval.",
                                Toast.LENGTH_LONG
                            ).show()
                            auth.signOut()
                        }
                        "approved" -> {
                            Log.d(TAG, "Seller approved, proceeding to SellerMainActivity")
                            val intent = Intent(this@LoginActivity, SellerMainActivity::class.java)
                            intent.putExtra("accountType", "seller")
                            intent.putExtra("email", email)
                            startActivity(intent)
                            finish()
                        }
                        "rejected" -> {
                            showLoading(false)
                            Toast.makeText(
                                this,
                                "Your seller account has been rejected. Please contact support for more information.",
                                Toast.LENGTH_LONG
                            ).show()
                            auth.signOut()
                        }
                        else -> {
                            Log.d(TAG, "No seller verification status found, checking regular user")
                            checkRegularUser(email)
                        }
                    }
                } else {
                    Log.d(TAG, "No seller account found, checking regular user")
                    // Not a seller account, check regular user
                    checkRegularUser(email)
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Log.e(TAG, "Error checking seller account", exception)
                Toast.makeText(
                    this,
                    "Error checking account type: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
                auth.signOut()
            }
    }

    private fun checkRegularUser(email: String) {
        Log.d(TAG, "Checking regular user account for: $email")
        firestore.collection("users")
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                showLoading(false)
                Log.d(TAG, "Regular user check result - Empty: ${documents.isEmpty}")
                if (documents.isEmpty) {
                    Log.e(TAG, "No user data found in Firestore")
                    Toast.makeText(
                        this,
                        "No user data found",
                        Toast.LENGTH_LONG
                    ).show()
                    auth.signOut()
                } else {
                    val userDoc = documents.documents[0]
                    val accountType = userDoc.getString("accountType") ?: "user"
                    val isDisabled = userDoc.getBoolean("isDisabled") ?: false
                    Log.d(TAG, "User account type: $accountType, isDisabled: $isDisabled")

                    if (isDisabled) {
                        Log.d(TAG, "Account is disabled")
                        Toast.makeText(
                            this,
                            "This account has been disabled. Please contact support for assistance.",
                            Toast.LENGTH_LONG
                        ).show()
                        auth.signOut()
                        return@addOnSuccessListener
                    }

                    when (accountType) {
                        "moderator" -> {
                            Log.d(TAG, "Redirecting to ModeratorActivity")
                            val intent = Intent(this@LoginActivity, ModeratorActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                        "seller" -> {
                            Log.d(TAG, "Redirecting to SellerMainActivity")
                            val intent = Intent(this@LoginActivity, SellerMainActivity::class.java)
                            intent.putExtra("accountType", "seller")
                            intent.putExtra("email", email)
                            startActivity(intent)
                            finish()
                        }
                        else -> {
                            Log.d(TAG, "Redirecting to LandingActivity as regular user")
                            val intent = Intent(this@LoginActivity, LandingActivity::class.java)
                            intent.putExtra("accountType", accountType)
                            intent.putExtra("email", email)
                            startActivity(intent)
                            finish()
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                showLoading(false)
                Log.e(TAG, "Error checking regular user account", exception)
                Toast.makeText(
                    this,
                    "Error checking account type: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
                auth.signOut()
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