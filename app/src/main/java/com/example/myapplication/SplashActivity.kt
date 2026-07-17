package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private var destinationIntent: Intent? = null
    private var isCheckComplete = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        supportActionBar?.hide()

        checkUserSession()

        // Delay for 2 seconds and then transition
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToDestination()
        }, 2000)
    }

    private fun checkUserSession() {
        val session = SupabaseManager.client.auth.currentSessionOrNull()
        if (session == null) {
            destinationIntent = Intent(this, LoginActivity::class.java)
            isCheckComplete = true
        } else {
            val user = session.user
            val userId = user?.id ?: ""
            checkAccountType(userId)
        }
    }

    private fun checkAccountType(userId: String) {
        lifecycleScope.launch {
            try {
                val profile = SupabaseManager.client.postgrest["profiles"]
                    .select {
                        filter {
                            eq("id", userId)
                        }
                    }
                    .decodeSingleOrNull<SupabaseManager.Profile>()

                if (profile == null) {
                    SupabaseManager.client.auth.signOut()
                    destinationIntent = Intent(this@SplashActivity, LoginActivity::class.java)
                } else {
                    val accountType = profile.account_type ?: "customer"
                    // Assuming isDisabled is a boolean in Profile
                    // val isDisabled = profile.isDisabled ?: false 
                    // For now, check what's in Profile data class
                    
                    destinationIntent = when (accountType) {
                        "moderator" -> Intent(this@SplashActivity, ModeratorActivity::class.java)
                        "seller" -> Intent(this@SplashActivity, SellerMainActivity::class.java).apply {
                            putExtra("accountType", "seller")
                            putExtra("email", profile.email)
                        }
                        else -> Intent(this@SplashActivity, LandingActivity::class.java).apply {
                            putExtra("accountType", accountType)
                            putExtra("email", profile.email)
                        }
                    }
                }
            } catch (e: Exception) {
                SupabaseManager.client.auth.signOut()
                destinationIntent = Intent(this@SplashActivity, LoginActivity::class.java)
            } finally {
                isCheckComplete = true
            }
        }
    }

    private fun navigateToDestination() {
        // If background check is not yet complete, wait for it
        if (!isCheckComplete) {
            Handler(Looper.getMainLooper()).postDelayed({
                navigateToDestination()
            }, 500)
            return
        }

        val intent = destinationIntent ?: Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}