package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.myapplication.databinding.ActivitySellerMainBinding
import com.example.myapplication.databinding.BottomSheetAccountBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayoutMediator

class SellerMainActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySellerMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySellerMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupBottomNavigation()
        setupFloatingActionButton()
    }

    private fun setupViewPager() {
        val pagerAdapter = SellerPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Dashboard"
                1 -> "Products"
                else -> null
            }
        }.attach()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_sales -> {
                    // Handle sales navigation
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

    private fun setupFloatingActionButton() {
        binding.fab.setOnClickListener {
            // Handle FAB click - Add new item
            // You can show a dialog or navigate to add item screen
        }
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