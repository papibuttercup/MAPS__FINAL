package com.example.myapplication

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
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
import android.content.Intent
import android.widget.Toast

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

        // Setup map
        val mapFragment = SupportMapFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.mapContainer, mapFragment)
            .commit()
        mapFragment.getMapAsync(this)

        loadUserData()
        setupShopNameEdit()
        setupButtons()
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("sellers").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val firstName = document.getString("firstName") ?: ""
                    val lastName = document.getString("lastName") ?: ""
                    val fullName = "$firstName $lastName"
                    val shopName = document.getString("shopName") ?: ""
                    binding.userNameText.text = fullName
                    binding.shopNameText.text = shopName
                    shopDocId = document.id
                }
            }
    }

    private fun setupShopNameEdit() {
        val editListener = {
            val currentShopName = binding.shopNameText.text.toString()
            val editText = EditText(this)
            editText.setText(currentShopName)
            AlertDialog.Builder(this)
                .setTitle("Edit Shop Name")
                .setView(editText)
                .setPositiveButton("Save") { dialog, _ ->
                    val newShopName = editText.text.toString().trim()
                    val userId = auth.currentUser?.uid ?: return@setPositiveButton
                    db.collection("sellers").document(userId)
                        .update("shopName", newShopName)
                        .addOnSuccessListener {
                            binding.shopNameText.text = newShopName
                            Toast.makeText(this, "Shop name updated", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to update shop name", Toast.LENGTH_SHORT).show()
                        }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .show()
        }
        binding.shopNameText.setOnClickListener { editListener() }
        binding.editShopNameIcon.setOnClickListener { editListener() }
    }

    private fun setupButtons() {
        binding.accountDetailsButton.setOnClickListener {
            // TODO: Navigate to Account Details Activity
            Toast.makeText(this, "Account Details clicked", Toast.LENGTH_SHORT).show()
        }
        binding.productsButton.setOnClickListener {
            // TODO: Navigate to Seller's Products Activity
            Toast.makeText(this, "Products clicked", Toast.LENGTH_SHORT).show()
        }
        binding.logoutButton.setOnClickListener {
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