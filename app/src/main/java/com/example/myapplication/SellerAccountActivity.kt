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
import org.maplibre.android.annotations.MarkerOptions
import android.util.Log

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        binding = ActivitySellerAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        miniMapView = binding.miniMapView
        miniMapView?.onCreate(savedInstanceState)

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
            val intent = Intent(this, Maps::class.java)
            intent.putExtra("locationName", "Shop Location")
            startActivityForResult(intent, 200)
        }

        // Set Cover Photo Button - Always enabled
        binding.btnSetCoverPhoto.isEnabled = true
        binding.btnSetCoverPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_COVER_PHOTO_REQUEST)
        }

        // Save Button
        binding.btnSave.setOnClickListener {
            saveShopProfile()
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
                                binding.btnSave.visibility = android.view.View.GONE
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
                        binding.btnSave.visibility = android.view.View.GONE
                        coverPhotoUrl = null
                        Toast.makeText(this, "Cover photo deleted successfully", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    hideProgressDialog()
                    Toast.makeText(this, "Failed to delete cover photo", Toast.LENGTH_SHORT).show()
                }
        }

        // Edit Profile Button - Now just enables editing
        binding.btnEditProfile.setOnClickListener {
            enableEditing()
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

        // Switch to Customer Mode Button
        binding.btnSwitchToCustomer.setOnClickListener {
            showSwitchToCustomerConfirmation()
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
                map.markers.forEach { map.removeMarker(it) }
                // Add new marker
                val location = LatLng(latitude, longitude)
                map.addMarker(MarkerOptions()
                    .position(location)
                    .title("Shop Location"))
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_COVER_PHOTO_REQUEST -> {
                    data?.data?.let { uri ->
                        coverPhotoUri = uri
                        binding.ivCoverPhoto.setImageURI(uri)
                        // Show save button when a new photo is selected
                        binding.btnSave.visibility = android.view.View.VISIBLE
                        binding.btnDeleteCoverPhoto.visibility = android.view.View.GONE
                        binding.btnSetCoverPhoto.text = "Change Cover Photo"
                        Toast.makeText(this, "Cover photo selected. Click Save Photo to save your changes!", Toast.LENGTH_LONG).show()
                    }
                }
                200 -> {
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
            }
        }
    }

    private fun enableEditing() {
        // Enable editing of fields
        binding.etShopName.isEnabled = true
        binding.etShopDescription.isEnabled = true
        binding.btnIdentifyLocation.isEnabled = true
        
        // Don't disable the cover photo button
        binding.btnSetCoverPhoto.isEnabled = true
        
        Toast.makeText(this, "Edit mode enabled. Make your changes and click Save when done.", Toast.LENGTH_LONG).show()
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
        Log.d("SellerAccount", "Starting profile save for user: $userId")

        val updates = mutableMapOf(
            "shopName" to shopName,
            "shopDescription" to shopDescription
        )

        // Handle cover photo upload if changed
        if (coverPhotoUri != null) {
            Log.d("SellerAccount", "Cover photo URI exists, starting upload")
            val storageRef = FirebaseStorage.getInstance().reference
                .child("cover_photos")
                .child(userId)
                .child("cover_photo.jpg")

            storageRef.putFile(coverPhotoUri!!)
                .addOnSuccessListener {
                    Log.d("SellerAccount", "Cover photo upload successful, getting download URL")
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        Log.d("SellerAccount", "Got download URL: ${downloadUri.toString()}")
                        updates["coverPhotoUrl"] = downloadUri.toString()
                        saveToFirestore(userId, updates)
                        // Update UI after successful save
                        binding.btnSave.visibility = android.view.View.GONE
                        binding.btnDeleteCoverPhoto.visibility = android.view.View.VISIBLE
                        binding.btnSetCoverPhoto.text = "Change Cover Photo"
                        coverPhotoUrl = downloadUri.toString()
                    }.addOnFailureListener { e ->
                        Log.e("SellerAccount", "Failed to get download URL", e)
                        hideProgressDialog()
                        Toast.makeText(this, "Failed to get cover photo URL: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("SellerAccount", "Failed to upload cover photo", e)
                    hideProgressDialog()
                    Toast.makeText(this, "Failed to upload cover photo: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                .addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                    Log.d("SellerAccount", "Upload progress: $progress%")
                }
        } else {
            Log.d("SellerAccount", "No cover photo to upload, saving to Firestore directly")
            saveToFirestore(userId, updates)
        }
    }

    private fun saveToFirestore(userId: String, updates: Map<String, Any>) {
        Log.d("SellerAccount", "Saving to Firestore with updates: $updates")
        db.collection("sellers").document(userId)
            .update(updates)
            .addOnSuccessListener {
                Log.d("SellerAccount", "Profile updated successfully in Firestore")
                hideProgressDialog()
                // Check if a cover photo was updated
                if (updates.containsKey("coverPhotoUrl")) {
                    Toast.makeText(this, "Profile and cover photo updated successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                }
                // Clear the cover photo URI after successful save
                coverPhotoUri = null
            }
            .addOnFailureListener { e ->
                Log.e("SellerAccount", "Failed to update profile in Firestore", e)
                hideProgressDialog()
                Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showSwitchToCustomerConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Switch to Customer Mode")
            .setMessage("Are you sure you want to switch to customer mode? You can switch back to seller mode anytime from your customer profile.")
            .setPositiveButton("Switch") { _, _ ->
                switchToCustomerMode()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun switchToCustomerMode() {
        val userId = auth.currentUser?.uid ?: return
        val userEmail = auth.currentUser?.email ?: return

        // Update user document to indicate they were previously a seller
        db.collection("users").document(userId)
            .update("switchedFromSeller", true)
            .addOnSuccessListener {
                // Start the landing activity (customer mode)
                val intent = Intent(this, LandingActivity::class.java)
                intent.putExtra("accountType", "customer")
                intent.putExtra("email", userEmail)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to switch to customer mode: ${e.message}", Toast.LENGTH_SHORT).show()
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
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        miniMapView?.onSaveInstanceState(outState)
    }
} 