package com.example.myapplication

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.myapplication.databinding.ActivitySellerAccountBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import androidx.fragment.app.FragmentManager
import com.google.android.gms.maps.SupportMapFragment
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.geometry.LatLng as MLLatLng
import org.maplibre.android.annotations.MarkerOptions as MLMarkerOptions
import android.util.Log

class SellerAccountActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivitySellerAccountBinding
    private lateinit var map: GoogleMap
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var shopDocId: String? = null
    private var coverPhotoUri: Uri? = null
    private var coverPhotoUrl: String? = null
    private val PICK_COVER_PHOTO_REQUEST = 101
    private lateinit var progressDialog: ProgressDialog
    private var miniMapView: MapView? = null
    private var miniMapLibreMap: MapLibreMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySellerAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        progressDialog = ProgressDialog(this)

        miniMapView = binding.miniMapView
        miniMapView?.onCreate(savedInstanceState)

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
                    val locationName = document.getString("locationName") ?: "No location set"
                    if (latitude != null && longitude != null) {
                        binding.tvCurrentLocation.text = "$locationName (Location set)"
                        binding.btnDeleteLocation.visibility = android.view.View.VISIBLE
                        binding.btnIdentifyLocation.visibility = android.view.View.GONE
                        binding.miniMapView.visibility = android.view.View.VISIBLE
                        showMiniMapLibrePreview(latitude, longitude)
                    } else {
                        binding.tvCurrentLocation.text = "No location set"
                        binding.btnDeleteLocation.visibility = android.view.View.GONE
                        binding.btnIdentifyLocation.visibility = android.view.View.VISIBLE
                        binding.miniMapView.visibility = android.view.View.GONE
                        destroyMiniMapLibre()
                    }

                    shopDocId = document.id
                }
            }
    }

    private fun setupButtons() {
        // Shop Location Button
        binding.btnIdentifyLocation.setOnClickListener {
            val intent = Intent(this, Maps::class.java)
            intent.putExtra("locationName", "Shop Location")
            startActivityForResult(intent, 200)
        }

        // Save/Update Button
        binding.btnEditProfile.setOnClickListener {
            saveShopProfile()
        }

        // Change Password Button
        binding.btnChangePassword.setOnClickListener {
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

        // Delete Location Button
        binding.btnDeleteLocation.setOnClickListener {
            val userId = auth.currentUser?.uid ?: return@setOnClickListener
            val updates = hashMapOf<String, Any?>(
                "latitude" to null,
                "longitude" to null,
                "locationName" to null
            )
            db.collection("sellers").document(userId)
                .update(updates)
                .addOnSuccessListener {
                    binding.tvCurrentLocation.text = "No location set"
                    binding.btnDeleteLocation.visibility = android.view.View.GONE
                    binding.btnIdentifyLocation.visibility = android.view.View.VISIBLE
                    binding.miniMapView.visibility = android.view.View.GONE
                    destroyMiniMapLibre()
                    Toast.makeText(this, "Location deleted", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to delete location", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveShopProfile() {
        val userId = auth.currentUser?.uid ?: return
        val shopName = binding.etShopName.text.toString().trim()
        val shopDescription = binding.etShopDescription.text.toString().trim()
        val data = hashMapOf<String, Any>(
            "shopName" to shopName,
            "shopDescription" to shopDescription
        )
        db.collection("sellers").document(userId)
            .update(data as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(this, "Shop profile updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Handle result from Maps activity
        if (requestCode == 200 && resultCode == Activity.RESULT_OK && data != null) {
            val lat = data.getDoubleExtra("latitude", 0.0)
            val lng = data.getDoubleExtra("longitude", 0.0)
            val locationName = data.getStringExtra("locationName") ?: ""
            val userId = auth.currentUser?.uid ?: return
            val update = hashMapOf<String, Any>(
                "latitude" to lat,
                "longitude" to lng,
                "locationName" to locationName
            )
            db.collection("sellers").document(userId)
                .update(update as Map<String, Any>)
                .addOnSuccessListener {
                    binding.tvCurrentLocation.text = "$locationName (Location set)"
                    binding.btnDeleteLocation.visibility = android.view.View.VISIBLE
                    binding.btnIdentifyLocation.visibility = android.view.View.GONE
                    binding.miniMapView.visibility = android.view.View.VISIBLE
                    showMiniMapLibrePreview(lat, lng)
                    Toast.makeText(this, "Shop location updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update location", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showMiniMapLibrePreview(latitude: Double, longitude: Double) {
        val TAG = "MiniMapLibre"
        val validLat = if (latitude == 0.0 && longitude == 0.0) 16.4023 else latitude
        val validLng = if (latitude == 0.0 && longitude == 0.0) 120.5960 else longitude
        val shopLatLng = MLLatLng(validLat, validLng)
        val styleUrl = "https://api.maptiler.com/maps/streets/style.json?key=" + getString(R.string.maptiler_api_key)
        Log.d(TAG, "MiniMap coordinates: $validLat, $validLng")
        Log.d(TAG, "MiniMap styleUrl: $styleUrl")
        Toast.makeText(this, "MiniMap: $validLat, $validLng", Toast.LENGTH_SHORT).show()
        miniMapView?.getMapAsync { mapLibreMap ->
            miniMapLibreMap = mapLibreMap
            mapLibreMap.setStyle(styleUrl) { style ->
                mapLibreMap.clear()
                mapLibreMap.addMarker(MLMarkerOptions().position(shopLatLng).title("Shop Location"))
                mapLibreMap.moveCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(shopLatLng, 16.0))
                mapLibreMap.uiSettings.isScrollGesturesEnabled = false
                mapLibreMap.uiSettings.isZoomGesturesEnabled = false
                mapLibreMap.uiSettings.isTiltGesturesEnabled = false
                mapLibreMap.uiSettings.isRotateGesturesEnabled = false
                mapLibreMap.uiSettings.isCompassEnabled = false
                mapLibreMap.uiSettings.isAttributionEnabled = false
                mapLibreMap.uiSettings.isLogoEnabled = false
                // Error handling: check if style loaded
                if (style == null) {
                    Toast.makeText(this, "MiniMap style failed to load", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "MiniMap style failed to load")
                }
            }
        }
    }

    private fun destroyMiniMapLibre() {
        miniMapView?.onPause()
        miniMapView?.onStop()
        miniMapView?.onDestroy()
        miniMapLibreMap = null
    }

    override fun onStart() {
        super.onStart()
        miniMapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        miniMapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        miniMapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        miniMapView?.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        miniMapView?.onDestroy()
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