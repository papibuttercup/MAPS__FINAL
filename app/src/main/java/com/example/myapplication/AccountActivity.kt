package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.databinding.ActivityAccountBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class AccountActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAccountBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        loadUserData()
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener { finish() }

        binding.logoutButton.setOnClickListener {
            lifecycleScope.launch {
                SupabaseManager.client.auth.signOut()
                getSharedPreferences("user_prefs", MODE_PRIVATE).edit().clear().apply()
                val intent = Intent(this@AccountActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }

        binding.editProfileButton.setOnClickListener {
             val intent = Intent(this, EditProfileActivity::class.java)
             startActivity(intent)
        }

        binding.viewOrderHistory.setOnClickListener {
            // Navigate to orders (which is in LandingActivity)
            val intent = Intent(this, LandingActivity::class.java)
            intent.putExtra("navigateTo", "orders")
            startActivity(intent)
            finish()
        }

        val comingSoonListener = android.view.View.OnClickListener {
            Toast.makeText(this, "Coming soon!", Toast.LENGTH_SHORT).show()
        }

        binding.statusUnpaid.setOnClickListener(comingSoonListener)
        binding.statusProcessing.setOnClickListener(comingSoonListener)
        binding.statusShipped.setOnClickListener(comingSoonListener)
        binding.statusReturns.setOnClickListener(comingSoonListener)

        binding.itemAbout.itemTitle.text = "About Thrifty"
        binding.itemShipping.itemTitle.text = "Shipping policy"
        binding.itemPayment.itemTitle.text = "Payment methods"
        binding.itemTerms.itemTitle.text = "Terms and conditions"
        binding.itemPrivacy.itemTitle.text = "Privacy policy"
        binding.itemSocial.itemTitle.text = "Social responsibility"
        binding.itemCareers.itemTitle.text = "Careers"

        binding.itemAbout.root.setOnClickListener {
            val intent = Intent(this, FAQActivity::class.java)
            startActivity(intent)
        }
        binding.itemShipping.root.setOnClickListener(comingSoonListener)
        binding.itemPayment.root.setOnClickListener(comingSoonListener)
        binding.itemTerms.root.setOnClickListener(comingSoonListener)
        binding.itemPrivacy.root.setOnClickListener(comingSoonListener)
        binding.itemSocial.root.setOnClickListener(comingSoonListener)
        binding.itemCareers.root.setOnClickListener(comingSoonListener)

        binding.switchToSellerButton.setOnClickListener {
            showSwitchToSellerConfirmation()
        }
    }

    private fun loadUserData() {
        val session = SupabaseManager.client.auth.currentSessionOrNull()
        if (session != null) {
            val user = session.user
            binding.userEmail.text = user?.email
            
            lifecycleScope.launch {
                try {
                    val userId = user?.id ?: return@launch
                    val profile = SupabaseManager.client.postgrest["profiles"]
                        .select {
                            filter {
                                eq("id", userId)
                            }
                        }
                        .decodeSingleOrNull<SupabaseManager.Profile>()

                    if (profile != null) {
                        val fullName = when {
                            !profile.first_name.isNullOrEmpty() && !profile.last_name.isNullOrEmpty() -> 
                                "${profile.first_name} ${profile.last_name}"
                            !profile.first_name.isNullOrEmpty() -> profile.first_name
                            !profile.last_name.isNullOrEmpty() -> profile.last_name
                            else -> "User"
                        }
                        binding.userName.text = fullName
                        
                        val isSeller = profile.account_type == "seller"
                        if (isSeller) {
                            binding.switchToSellerButton.visibility = android.view.View.VISIBLE
                        } else {
                            binding.switchToSellerButton.visibility = android.view.View.GONE
                        }
                    } else {
                        binding.userName.text = "User"
                        binding.switchToSellerButton.visibility = android.view.View.GONE
                    }
                } catch (e: Exception) {
                    binding.userName.text = "User"
                    binding.switchToSellerButton.visibility = android.view.View.GONE
                    Toast.makeText(this@AccountActivity, "Error loading user data", Toast.LENGTH_SHORT).show()
                }
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
        val session = SupabaseManager.client.auth.currentSessionOrNull() ?: return
        val userId = session.user?.id ?: return
        val userEmail = session.user?.email ?: return

        lifecycleScope.launch {
            try {
                val profile = SupabaseManager.client.postgrest["profiles"]
                    .select {
                        filter {
                            eq("id", userId)
                            eq("account_type", "seller")
                        }
                    }
                    .decodeSingleOrNull<SupabaseManager.Profile>()

                if (profile != null) {
                    val intent = Intent(this@AccountActivity, SellerMainActivity::class.java)
                    intent.putExtra("accountType", "seller")
                    intent.putExtra("email", userEmail)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@AccountActivity, "Seller account not found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AccountActivity, "Failed to switch to seller mode: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
} 