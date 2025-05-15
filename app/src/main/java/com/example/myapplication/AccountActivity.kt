package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityAccountBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AccountActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var binding: ActivityAccountBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupClickListeners()
        loadUserData()
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener { finish() }

        binding.logoutButton.setOnClickListener {
            auth.signOut()
            getSharedPreferences("user_prefs", MODE_PRIVATE).edit().clear().apply()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        binding.editProfileButton.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        binding.settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        binding.helpButton.setOnClickListener {
            Toast.makeText(this, "Help & Support coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            binding.userEmail.text = currentUser.email
            db.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val firstName = documentSnapshot.getString("firstName")
                        val lastName = documentSnapshot.getString("lastName")
                        val fullName = when {
                            !firstName.isNullOrEmpty() && !lastName.isNullOrEmpty() -> "$firstName $lastName"
                            !firstName.isNullOrEmpty() -> firstName
                            !lastName.isNullOrEmpty() -> lastName
                            else -> "User"
                        }
                        binding.userName.text = fullName
                    } else {
                        binding.userName.text = "User"
                    }
                }
                .addOnFailureListener {
                    binding.userName.text = "User"
                    Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show()
                }
        } else {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
} 