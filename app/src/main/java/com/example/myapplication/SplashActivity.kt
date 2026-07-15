package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var destinationIntent: Intent? = null
    private var isCheckComplete = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        checkUserSession()

        // Delay for 2 seconds and then transition
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToDestination()
        }, 2000)
    }

    private fun checkUserSession() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            destinationIntent = Intent(this, LoginActivity::class.java)
            isCheckComplete = true
        } else {
            val email = currentUser.email ?: ""
            checkAccountType(email)
        }
    }

    private fun checkAccountType(email: String) {
        db.collection("sellers")
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .addOnSuccessListener { sellerDocuments ->
                if (!sellerDocuments.isEmpty) {
                    val sellerDoc = sellerDocuments.documents[0]
                    val verificationStatus = sellerDoc.getString("verificationStatus")
                    val isDisabled = sellerDoc.getBoolean("isDisabled") ?: false
                    
                    if (isDisabled || verificationStatus == "pending" || verificationStatus == "rejected") {
                        auth.signOut()
                        destinationIntent = Intent(this, LoginActivity::class.java)
                    } else if (verificationStatus == "approved") {
                        destinationIntent = Intent(this, SellerMainActivity::class.java).apply {
                            putExtra("accountType", "seller")
                            putExtra("email", email)
                        }
                    } else {
                        checkRegularUser(email)
                        return@addOnSuccessListener
                    }
                    isCheckComplete = true
                } else {
                    checkRegularUser(email)
                }
            }
            .addOnFailureListener {
                auth.signOut()
                destinationIntent = Intent(this, LoginActivity::class.java)
                isCheckComplete = true
            }
    }

    private fun checkRegularUser(email: String) {
        db.collection("users")
            .whereEqualTo("email", email)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    auth.signOut()
                    destinationIntent = Intent(this, LoginActivity::class.java)
                } else {
                    val userDoc = documents.documents[0]
                    val accountType = userDoc.getString("accountType") ?: "user"
                    val isDisabled = userDoc.getBoolean("isDisabled") ?: false

                    if (isDisabled) {
                        auth.signOut()
                        destinationIntent = Intent(this, LoginActivity::class.java)
                    } else {
                        destinationIntent = when (accountType) {
                            "moderator" -> Intent(this, ModeratorActivity::class.java)
                            "seller" -> Intent(this, SellerMainActivity::class.java).apply {
                                putExtra("accountType", "seller")
                                putExtra("email", email)
                            }
                            else -> Intent(this, LandingActivity::class.java).apply {
                                putExtra("accountType", accountType)
                                putExtra("email", email)
                            }
                        }
                    }
                }
                isCheckComplete = true
            }
            .addOnFailureListener {
                auth.signOut()
                destinationIntent = Intent(this, LoginActivity::class.java)
                isCheckComplete = true
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