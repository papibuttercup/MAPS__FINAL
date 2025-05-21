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

        // Remove or comment out references to editProfileButton since the button was removed from the layout
        // binding.editProfileButton.setOnClickListener {
        //     val intent = Intent(this, EditProfileActivity::class.java)
        //     startActivity(intent)
        // }

        binding.settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        binding.helpButton.setOnClickListener {
            Toast.makeText(this, "Help & Support coming soon!", Toast.LENGTH_SHORT).show()
        }

        binding.switchToSellerButton.setOnClickListener {
            showSwitchToSellerConfirmation()
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
                        val wasSeller = documentSnapshot.getBoolean("switchedFromSeller") ?: false
                        if (wasSeller) {
                            binding.switchToSellerButton.visibility = android.view.View.VISIBLE
                        } else {
                            binding.switchToSellerButton.visibility = android.view.View.GONE
                        }
                    } else {
                        binding.userName.text = "User"
                        binding.switchToSellerButton.visibility = android.view.View.GONE
                    }
                }
                .addOnFailureListener {
                    binding.userName.text = "User"
                    binding.switchToSellerButton.visibility = android.view.View.GONE
                    Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show()
                }
        } else {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun showSwitchToSellerConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Switch to Seller Mode")
            .setMessage("Are you sure you want to switch back to seller mode? You can switch to customer mode anytime from your seller profile.")
            .setPositiveButton("Switch") { _, _ ->
                switchToSellerMode()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun switchToSellerMode() {
        val userId = auth.currentUser?.uid ?: return
        val userEmail = auth.currentUser?.email ?: return
        db.collection("sellers").document(userId)
            .get()
            .addOnSuccessListener { sellerDoc ->
                if (sellerDoc != null && sellerDoc.exists()) {
                    val intent = Intent(this, SellerMainActivity::class.java)
                    intent.putExtra("accountType", "seller")
                    intent.putExtra("email", userEmail)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Seller account not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to switch to seller mode: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
} 