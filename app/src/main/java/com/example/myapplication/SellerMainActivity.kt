package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.ActivitySellerMainBinding
import com.example.myapplication.databinding.BottomSheetAccountBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class SellerMainActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySellerMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySellerMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        
        // Set default fragment
        if (savedInstanceState == null) {
            loadFragment(SellerDashboardFragment())
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    loadFragment(SellerDashboardFragment())
                    true
                }
                R.id.navigation_products -> {
                    loadFragment(SellerProductsFragment())
                    true
                }
                R.id.navigation_orders -> {
                    startActivity(Intent(this, SellerOrdersActivity::class.java))
                    true
                }
                R.id.navigation_account -> {
                    startActivity(Intent(this, SellerAccountActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun showAccountBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetBinding = BottomSheetAccountBinding.inflate(layoutInflater)
        
        bottomSheetBinding.logoutButton.setOnClickListener {
            // Handle logout
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setContentView(bottomSheetBinding.root)
        bottomSheetDialog.show()
    }
} 