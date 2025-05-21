package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.myapplication.databinding.ActivitySellerAccountBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher

class SellerAccountActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySellerAccountBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var shopDocId: String? = null
    private var coverPhotoUri: Uri? = null
    private var coverPhotoUrl: String? = null
    private val PICK_COVER_PHOTO_REQUEST = 101
    private var miniMapView: MapView? = null
    private var miniMapLibreMap: MapLibreMap? = null
    private var progressDialog: AlertDialog? = null
    private val markers = mutableListOf<Marker>()
    private lateinit var locationPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        binding = ActivitySellerAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Initialize map view
        miniMapView = binding.miniMapView
        miniMapView?.onCreate(savedInstanceState)

        // Initialize activity result launchers
        locationPickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                handleLocationPickerResult(result.data)
            }
        }

        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                handleImagePickerResult(result.data)
            }
        }

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

                    // Set cover photo if available
                    coverPhotoUrl = document.getString("coverPhotoUrl")
                    if (!coverPhotoUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(coverPhotoUrl)
                            .placeholder(R.drawable.ic_add_photo)
                            .error(R.drawable.ic_add_photo)
                            .into(binding.ivCoverPhoto)
                        binding.btnDeleteCoverPhoto.visibility = android.view.View.VISIBLE
                        binding.btnSetCoverPhoto.text = "Change Cover Photo"
                    } else {
                        binding.ivCoverPhoto.setImageResource(R.drawable.ic_add_photo)
                        binding.btnDeleteCoverPhoto.visibility = android.view.View.GONE
                        binding.btnSetCoverPhoto.text = "Set Cover Photo"
                    }

                    // Set current location if available
                    val latitude = document.getDouble("latitude")
                    val longitude = document.getDouble("longitude")
                    val locationName = document.getString("locationName") ?: "Unknown Location"
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
                    }

                    shopDocId = document.id
                }
            }
            .addOnFailureListener { e ->
                Log.e("SellerAccount", "Failed to load user data: ${e.message}")
                Toast.makeText(this, "Failed to load profile data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupButtons() {
        // Shop Location Button
        binding.btnIdentifyLocation.setOnClickListener {
            startLocationPicker()
        }

        // Set Cover Photo Button
        binding.btnSetCoverPhoto.setOnClickListener {
            startImagePicker()
        }

        // Delete Cover Photo Button
        binding.btnDeleteCoverPhoto.setOnClickListener {
            val userId = auth.currentUser?.uid ?: return@setOnClickListener
            showProgressDialog("Deleting cover photo...")

            // Delete from Firestore
            db.collection("sellers").document(userId)
                .update("coverPhotoUrl", null)
                .addOnSuccessListener {
                    // Delete from Storage if URL exists
                    if (!coverPhotoUrl.isNullOrEmpty()) {
                        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(coverPhotoUrl!!)
                        storageRef.delete()
                            .addOnSuccessListener {
                                hideProgressDialog()
                                binding.ivCoverPhoto.setImageResource(R.drawable.ic_add_photo)
                                binding.btnDeleteCoverPhoto.visibility = android.view.View.GONE
                                binding.btnSetCoverPhoto.text = "Set Cover Photo"
                                coverPhotoUrl = null
                                Toast.makeText(this, "Cover photo deleted successfully", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                hideProgressDialog()
                                Toast.makeText(this, "Failed to delete cover photo from storage", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        hideProgressDialog()
                        binding.ivCoverPhoto.setImageResource(R.drawable.ic_add_photo)
                        binding.btnDeleteCoverPhoto.visibility = android.view.View.GONE
                        binding.btnSetCoverPhoto.text = "Set Cover Photo"
                        coverPhotoUrl = null
                        Toast.makeText(this, "Cover photo deleted successfully", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    hideProgressDialog()
                    Toast.makeText(this, "Failed to delete cover photo", Toast.LENGTH_SHORT).show()
                }
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
            showProgressDialog("Deleting location...")
            
            db.collection("sellers").document(userId)
                .update(
                    mapOf(
                        "latitude" to null,
                        "longitude" to null,
                        "locationName" to null
                    )
                )
                .addOnSuccessListener {
                    hideProgressDialog()
                    binding.tvCurrentLocation.text = "No location set"
                    binding.btnDeleteLocation.visibility = android.view.View.GONE
                    binding.btnIdentifyLocation.visibility = android.view.View.VISIBLE
                    binding.miniMapView.visibility = android.view.View.GONE
                    Toast.makeText(this, "Location deleted successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    hideProgressDialog()
                    Toast.makeText(this, "Failed to delete location: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showMiniMapLibrePreview(latitude: Double, longitude: Double) {
        Log.d("MiniMapDebug", "showMiniMapLibrePreview called with lat=$latitude, lon=$longitude")
        binding.miniMapView.visibility = android.view.View.VISIBLE
        miniMapView?.onResume() // Ensure the map is resumed when shown
        val mapTilerApiKey = getString(R.string.maptiler_api_key)
        val styleUrl = "https://api.maptiler.com/maps/streets/style.json?key=$mapTilerApiKey"
        miniMapView?.getMapAsync { map ->
            miniMapLibreMap = map
            map.setStyle(Style.Builder().fromUri(styleUrl)) { style ->
                // Clear any existing markers
                removeMarkers()
                // Add new marker
                val location = LatLng(latitude, longitude)
                addMarkerToMap(location, "Shop Location")
                // Set camera position with animation
                map.animateCamera(
                    org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(
                        location,
                        15.0
                    ),
                    1000
                )
                Log.d("MiniMapDebug", "Marker added and camera animated")
            }
        }
    }

    private fun showProgressDialog(message: String) {
        progressDialog = AlertDialog.Builder(this)
            .setMessage(message)
            .setCancelable(false)
            .create()
        progressDialog?.show()
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun addMarkerToMap(latLng: LatLng, title: String) {
        miniMapLibreMap?.let { map ->
            val marker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(title)
            )
            marker?.let { markers.add(it) }
        }
    }

    private fun removeMarkers() {
        miniMapLibreMap?.let { map ->
            markers.forEach { marker ->
                map.removeMarker(marker)
            }
            markers.clear()
        }
    }

    private fun startLocationPicker() {
        val intent = Intent(this, Maps::class.java)
        intent.putExtra("locationName", "Shop Location")
        locationPickerLauncher.launch(intent)
    }

    private fun startImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        imagePickerLauncher.launch(intent)
    }

    private fun handleLocationPickerResult(data: Intent?) {
        val latitude = data?.getDoubleExtra("latitude", 0.0) ?: 0.0
        val longitude = data?.getDoubleExtra("longitude", 0.0) ?: 0.0
        val locationName = data?.getStringExtra("locationName") ?: "Location"
        
        if (latitude != 0.0 && longitude != 0.0) {
            // Immediately update UI with new location
            binding.tvCurrentLocation.text = "$locationName (Location set)"
            binding.btnDeleteLocation.visibility = android.view.View.VISIBLE
            binding.btnIdentifyLocation.visibility = android.view.View.GONE
            binding.miniMapView.visibility = android.view.View.VISIBLE
            showMiniMapLibrePreview(latitude, longitude)
            
            // Save location to Firestore
            val userId = auth.currentUser?.uid ?: return
            db.collection("sellers").document(userId)
                .update(
                    mapOf(
                        "latitude" to latitude,
                        "longitude" to longitude,
                        "locationName" to locationName
                    )
                )
                .addOnSuccessListener {
                    Toast.makeText(this, "Location saved successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to save location: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun handleImagePickerResult(data: Intent?) {
        data?.data?.let { uri ->
            coverPhotoUri = uri
            binding.ivCoverPhoto.setImageURI(uri)
            binding.btnDeleteCoverPhoto.visibility = android.view.View.VISIBLE
            binding.btnSetCoverPhoto.text = "Change Cover Photo"
        }
    }

    private fun saveShopProfile() {
        val userId = auth.currentUser?.uid ?: return
        val shopName = binding.etShopName.text.toString().trim()
        val shopDescription = binding.etShopDescription.text.toString().trim()

        if (shopName.isEmpty()) {
            Toast.makeText(this, "Please enter a shop name", Toast.LENGTH_SHORT).show()
            return
        }

        showProgressDialog("Saving profile...")

        val updates = mutableMapOf(
            "shopName" to shopName,
            "shopDescription" to shopDescription
        )

        // Handle cover photo upload if changed
        coverPhotoUri?.let { uri ->
            val storageRef = FirebaseStorage.getInstance().reference
                .child("cover_photos")
                .child(userId)
                .child("cover_photo.jpg")

            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        updates["coverPhotoUrl"] = downloadUri.toString()
                        saveToFirestore(userId, updates)
                    }
                }
                .addOnFailureListener { e ->
                    hideProgressDialog()
                    Toast.makeText(this, "Failed to upload cover photo: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            saveToFirestore(userId, updates)
        }
    }

    private fun saveToFirestore(userId: String, updates: Map<String, Any>) {
        db.collection("sellers").document(userId)
            .update(updates)
            .addOnSuccessListener {
                hideProgressDialog()
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                hideProgressDialog()
                Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
        removeMarkers()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        miniMapView?.onSaveInstanceState(outState)
    }
} 