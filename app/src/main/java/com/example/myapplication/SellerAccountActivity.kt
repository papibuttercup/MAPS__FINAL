package com.example.myapplication

import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivitySellerAccountBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SellerAccountActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivitySellerAccountBinding
    private lateinit var map: GoogleMap
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var shopDocId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySellerAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setupToolbar()
        loadUserData()
        setupButtons()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("sellers").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    // Set shop name
                    val shopName = document.getString("shopName") ?: ""
                    binding.etShopName.setText(shopName)

                    // Set shop description
                    val shopDescription = document.getString("shopDescription") ?: ""
                    binding.etShopDescription.setText(shopDescription)

                    // Set current location if available
                    val latitude = document.getDouble("latitude")
                    val longitude = document.getDouble("longitude")
                    if (latitude != null && longitude != null) {
                        binding.tvCurrentLocation.text = "Location set"
                    }

                    shopDocId = document.id
                }
            }
    }

    private fun setupButtons() {
        // Shop Location Button
        binding.btnIdentifyLocation.setOnClickListener {
            val intent = Intent(this, Maps::class.java)
            startActivity(intent)
        }

        // Edit Profile Button
        binding.btnEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        // Change Password Button
        binding.btnChangePassword.setOnClickListener {
            // TODO: Implement password change functionality
            Toast.makeText(this, "Password change coming soon", Toast.LENGTH_SHORT).show()
        }

        // Logout Button
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        val userId = auth.currentUser?.uid ?: return
        db.collection("sellers").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val latitude = document.getDouble("latitude")
                val longitude = document.getDouble("longitude")
                if (latitude != null && longitude != null) {
                    val location = LatLng(latitude, longitude)
                    map.addMarker(MarkerOptions().position(location).title("Your Location"))
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
                }
            }
    }
} 