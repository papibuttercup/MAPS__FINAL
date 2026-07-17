package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityCustomerProfileBinding

class CustomerProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCustomerProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        val userId = SupabaseManager.getCurrentUserId() ?: return
        
        SupabaseManager.getUserProfile(userId, object : SupabaseManager.SupabaseCallbackWithProfile {
            override fun onResult(success: Boolean, profile: SupabaseManager.Profile?, error: String?) {
                if (success && profile != null) {
                    // In your previous Firebase logic, you checked "switchedFromSeller".
                    // Assuming account_type is stored in Supabase Profile.
                    // If the user is currently in CustomerProfileActivity, they are a customer.
                    // We check if they have a shop_name or other seller fields to decide if they can switch back.
                    val isPotentiallySeller = profile.account_type == "seller" || !profile.shop_name.isNullOrEmpty()
                    
                    if (isPotentiallySeller) {
                        binding.btnSwitchToSeller.visibility = View.VISIBLE
                        binding.tvSwitchToSellerInfo.visibility = View.VISIBLE
                    } else {
                        binding.btnSwitchToSeller.visibility = View.GONE
                        binding.tvSwitchToSellerInfo.visibility = View.GONE
                    }
                } else if (error != null) {
                    Toast.makeText(this@CustomerProfileActivity, "Error loading profile: $error", Toast.LENGTH_SHORT).show()
                }
            }
        })
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
            SupabaseManager.signOut(object : SupabaseManager.SupabaseCallback {
                override fun onResult(success: Boolean, error: String?) {
                    if (success) {
                        val intent = Intent(this@CustomerProfileActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@CustomerProfileActivity, "Sign out failed: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            })
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
        val userId = SupabaseManager.getCurrentUserId() ?: return
        val userSession = SupabaseManager.getCurrentSession()
        val userEmail = userSession?.user?.email ?: ""

        SupabaseManager.getUserProfile(userId, object : SupabaseManager.SupabaseCallbackWithProfile {
            override fun onResult(success: Boolean, profile: SupabaseManager.Profile?, error: String?) {
                if (success && profile != null) {
                    // Start the seller main activity
                    val intent = Intent(this@CustomerProfileActivity, SellerMainActivity::class.java)
                    intent.putExtra("accountType", "seller")
                    intent.putExtra("email", userEmail)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@CustomerProfileActivity, "Seller profile not found: $error", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
}
