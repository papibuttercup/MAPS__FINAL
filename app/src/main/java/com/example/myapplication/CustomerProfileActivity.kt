package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityCustomerProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CustomerProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCustomerProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupToolbar()
        loadUserData()
        setupButtons()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    // Check if user was previously a seller
                    val wasSeller = document.getBoolean("switchedFromSeller") ?: false
                    if (wasSeller) {
                        binding.btnSwitchToSeller.visibility = View.VISIBLE
                        binding.tvSwitchToSellerInfo.visibility = View.VISIBLE
                    } else {
                        binding.btnSwitchToSeller.visibility = View.GONE
                        binding.tvSwitchToSellerInfo.visibility = View.GONE
                    }
                }
            }
    }

    private fun setupButtons() {
        binding.btnEditProfile.setOnClickListener {
            // TODO: Implement edit profile functionality
            Toast.makeText(this, "Edit profile coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnChangePassword.setOnClickListener {
            // TODO: Implement change password functionality
            Toast.makeText(this, "Change password coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        binding.btnSwitchToSeller.setOnClickListener {
            showSwitchToSellerConfirmation()
        }
    }

    private fun showSwitchToSellerConfirmation() {
        AlertDialog.Builder(this)
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

        // Get the seller data from the sellers collection
        db.collection("sellers").document(userId)
            .get()
            .addOnSuccessListener { sellerDoc ->
                if (sellerDoc != null && sellerDoc.exists()) {
                    // Start the seller main activity
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