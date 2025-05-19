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

        // Set Cover Photo Button
        binding.btnSetCoverPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_COVER_PHOTO_REQUEST)
        }

        // Delete Cover Photo Button
        binding.btnDeleteCoverPhoto.setOnClickListener {
            val userId = auth.currentUser?.uid ?: return@setOnClickListener
            progressDialog.setMessage("Deleting cover photo...")
            progressDialog.show()

            // Delete from Firestore
            db.collection("sellers").document(userId)
                .update("coverPhotoUrl", null)
                .addOnSuccessListener {
                    // Delete from Storage if URL exists
                    if (!coverPhotoUrl.isNullOrEmpty()) {
                        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(coverPhotoUrl!!)
                        storageRef.delete()
                            .addOnSuccessListener {
                                progressDialog.dismiss()
                                binding.ivCoverPhoto.setImageResource(R.drawable.ic_add_photo)
                                binding.btnDeleteCoverPhoto.visibility = android.view.View.GONE
                                binding.btnSetCoverPhoto.text = "Set Cover Photo"
                                coverPhotoUrl = null
                                Toast.makeText(this, "Cover photo deleted successfully", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                progressDialog.dismiss()
                                Toast.makeText(this, "Failed to delete cover photo from storage", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        progressDialog.dismiss()
                        binding.ivCoverPhoto.setImageResource(R.drawable.ic_add_photo)
                        binding.btnDeleteCoverPhoto.visibility = android.view.View.GONE
                        binding.btnSetCoverPhoto.text = "Set Cover Photo"
                        coverPhotoUrl = null
                        Toast.makeText(this, "Cover photo deleted successfully", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    progressDialog.dismiss()
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
        
        progressDialog.setMessage("Updating profile...")
        progressDialog.show()

        val data = hashMapOf<String, Any>(
            "shopName" to shopName,
            "shopDescription" to shopDescription
        )

        // Only update cover photo if it's a new one
        if (coverPhotoUri != null) {
            saveCoverPhoto()
        }

        updateProfileData(userId, data)
    }

    private fun updateProfileData(userId: String, data: Map<String, Any>) {
        db.collection("sellers").document(userId)
            .update(data)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Shop profile updated", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Log.e("SellerAccount", "Failed to update profile: ${e.message}")
                Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveCoverPhoto() {
        val userId = auth.currentUser?.uid ?: return
        if (coverPhotoUri == null) return

        progressDialog.setMessage("Uploading cover photo...")
        progressDialog.show()

        val storageRef = FirebaseStorage.getInstance().reference
            .child("shop_covers")
            .child(userId)
            .child("cover_${System.currentTimeMillis()}.jpg")

        storageRef.putFile(coverPhotoUri!!)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    // Update Firestore with new cover photo URL
                    db.collection("sellers").document(userId)
                        .update("coverPhotoUrl", uri.toString())
                        .addOnSuccessListener {
                            progressDialog.dismiss()
                            coverPhotoUrl = uri.toString()
                            binding.btnDeleteCoverPhoto.visibility = android.view.View.VISIBLE
                            binding.btnSetCoverPhoto.text = "Change Cover Photo"
                            Toast.makeText(this, "Cover photo has been set successfully!", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener { e ->
                            progressDialog.dismiss()
                            Log.e("SellerAccount", "Failed to update cover photo URL: ${e.message}")
                            Toast.makeText(this, "Failed to save cover photo: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Log.e("SellerAccount", "Failed to upload cover photo: ${e.message}")
                Toast.makeText(this, "Failed to upload cover photo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_COVER_PHOTO_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            coverPhotoUri = data.data
            binding.ivCoverPhoto.setImageURI(coverPhotoUri)
            // Save cover photo immediately when selected
            saveCoverPhoto()
        } else if (requestCode == 200 && resultCode == Activity.RESULT_OK && data != null) {
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