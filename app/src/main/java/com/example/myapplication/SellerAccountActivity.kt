package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.myapplication.databinding.ActivitySellerAccountBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class SellerAccountActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySellerAccountBinding
    private var coverPhotoUri: Uri? = null
    private val PICK_COVER_PHOTO_REQUEST = 101
    private val PICK_LOCATION_REQUEST = 200
    private var miniMapView: MapView? = null
    private var progressDialog: AlertDialog? = null
    private var currentProfile: SupabaseManager.Profile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        binding = ActivitySellerAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        miniMapView = binding.miniMapView
        miniMapView?.onCreate(savedInstanceState)

        setupToolbar()
        loadUserData()
        setupButtons()
    }

    private fun setupToolbar() {
        binding.backButton.setOnClickListener { finish() }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadUserData() {
        val userId = SupabaseManager.getCurrentUserId() ?: return
        
        SupabaseManager.getUserProfile(userId, object : SupabaseManager.SupabaseCallbackWithProfile {
            override fun onResult(success: Boolean, profile: SupabaseManager.Profile?, error: String?) {
                lifecycleScope.launch {
                    if (profile != null) {
                        currentProfile = profile
                        
                        binding.tvShopName.text = profile.shop_name ?: "Shop Name"
                        binding.etShopName.setText(profile.shop_name ?: "")
                        
                        binding.tvShopDescription.text = profile.shop_description ?: "No description set"
                        binding.etShopDescription.setText(profile.shop_description ?: "")
                        
                        if (!profile.shop_location.isNullOrEmpty() && profile.shop_location.startsWith("http")) {
                            binding.tvNoCover.visibility = View.GONE
                            Glide.with(this@SellerAccountActivity)
                                .load(profile.shop_location)
                                .placeholder(R.drawable.ic_image_placeholder)
                                .into(binding.ivCoverPhoto)
                        } else {
                            binding.tvNoCover.visibility = View.VISIBLE
                            binding.ivCoverPhoto.setImageResource(R.drawable.ic_image_placeholder)
                        }

                        loadStats(userId)
                        loadLocationData(userId)
                    } else if (error != null) {
                        Log.e("SellerAccount", "Failed to load user data: $error")
                    }
                }
            }
        })
    }

    private fun loadStats(userId: String) {
        lifecycleScope.launch {
            try {
                val products = SupabaseManager.client.postgrest["products"]
                    .select { filter { eq("seller_id", userId) } }
                    .decodeList<SupabaseManager.ProductModel>()
                
                val activeOrders = SupabaseManager.client.postgrest["orders"]
                    .select {
                        filter {
                            eq("seller_id", userId)
                            or {
                                eq("status", "pending")
                                eq("status", "accepted")
                            }
                        }
                    }
                    .decodeList<SupabaseManager.Order>()

                binding.tvProductsCount.text = products.size.toString()
                binding.tvActiveOrdersCount.text = activeOrders.size.toString()
            } catch (e: Exception) {
                Log.e("SellerAccount", "Failed to load stats: ${e.message}")
            }
        }
    }

    private fun loadLocationData(userId: String) {
        lifecycleScope.launch {
            try {
                val markers = SupabaseManager.client.postgrest["seller_markers"]
                    .select { filter { eq("seller_id", userId) } }
                    .decodeList<SupabaseManager.SellerMarker>()

                if (markers.isNotEmpty()) {
                    val marker = markers[0]
                    binding.tvCurrentLocation.text = "Location set: ${marker.latitude}, ${marker.longitude}"
                    showMiniMapLibrePreview(marker.latitude, marker.longitude)
                } else {
                    binding.tvCurrentLocation.text = "Not set · buyers can't estimate delivery"
                    binding.miniMapView.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e("SellerAccount", "Failed to load location: ${e.message}")
            }
        }
    }

    private fun setupButtons() {
        binding.btnToggleEdit.setOnClickListener { toggleEditMode(true) }
        binding.btnCancelEdit.setOnClickListener { toggleEditMode(false) }
        binding.btnSaveInline.setOnClickListener { saveProfileInline() }

        binding.btnIdentifyLocation.setOnClickListener {
            startActivityForResult(Intent(this, Maps::class.java).putExtra("mode", "pick_location"), PICK_LOCATION_REQUEST)
        }

        binding.btnSetCoverPhoto.setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI), PICK_COVER_PHOTO_REQUEST)
        }

        binding.btnChangePassword.setOnClickListener { showChangePasswordDialog() }

        binding.btnLogout.setOnClickListener {
            lifecycleScope.launch {
                SupabaseManager.client.auth.signOut()
                startActivity(Intent(this@SellerAccountActivity, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                finish()
            }
        }

        binding.btnSwitchToCustomer.setOnClickListener { showSwitchToCustomerConfirmation() }
        
        binding.btnNotifications.setOnClickListener {
            Toast.makeText(this, "Notifications settings coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleEditMode(editing: Boolean) {
        binding.btnToggleEdit.visibility = if (editing) View.GONE else View.VISIBLE
        binding.btnCancelEdit.visibility = if (editing) View.VISIBLE else View.GONE
        binding.tvShopName.visibility = if (editing) View.GONE else View.VISIBLE
        binding.tvShopDescription.visibility = if (editing) View.GONE else View.VISIBLE
        binding.statsRow.visibility = if (editing) View.GONE else View.VISIBLE
        binding.editFields.visibility = if (editing) View.VISIBLE else View.GONE
        
        if (editing) {
            binding.etShopName.setText(currentProfile?.shop_name ?: "")
            binding.etShopDescription.setText(currentProfile?.shop_description ?: "")
        }
    }

    private fun saveProfileInline() {
        val userId = SupabaseManager.getCurrentUserId() ?: return
        val shopName = binding.etShopName.text.toString().trim()
        val shopDescription = binding.etShopDescription.text.toString().trim()

        if (shopName.isEmpty()) {
            Toast.makeText(this, "Shop name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        showProgressDialog("Saving profile...")
        val updates = mapOf<String, Any>("shop_name" to shopName, "shop_description" to shopDescription)

        SupabaseManager.updateProfile(userId, updates, object : SupabaseManager.SupabaseCallback {
            override fun onResult(success: Boolean, error: String?) {
                lifecycleScope.launch {
                    hideProgressDialog()
                    if (success) {
                        Toast.makeText(this@SellerAccountActivity, "Profile saved successfully", Toast.LENGTH_SHORT).show()
                        toggleEditMode(false)
                        loadUserData()
                    } else {
                        Toast.makeText(this@SellerAccountActivity, "Failed to save profile: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val etNewPassword = dialogView.findViewById<android.widget.EditText>(R.id.newPasswordInput)
        val etConfirmPassword = dialogView.findViewById<android.widget.EditText>(R.id.confirmPasswordInput)

        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val newPass = etNewPassword.text.toString()
                val confirmPass = etConfirmPassword.text.toString()
                if (newPass.length < 6) {
                    Toast.makeText(this, "Password too short", Toast.LENGTH_SHORT).show()
                } else if (newPass == confirmPass) {
                    updatePassword(newPass)
                } else {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updatePassword(newPass: String) {
        showProgressDialog("Updating password...")
        SupabaseManager.updatePassword(newPass, object : SupabaseManager.SupabaseCallback {
            override fun onResult(success: Boolean, error: String?) {
                hideProgressDialog()
                if (success) {
                    Toast.makeText(this@SellerAccountActivity, "Password updated successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@SellerAccountActivity, "Error: $error", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_LOCATION_REQUEST -> {
                    val lat = data?.getDoubleExtra("latitude", 0.0) ?: 0.0
                    val lng = data?.getDoubleExtra("longitude", 0.0) ?: 0.0
                    if (lat != 0.0) saveLocation(lat, lng)
                }
                PICK_COVER_PHOTO_REQUEST -> {
                    data?.data?.let { uri ->
                        coverPhotoUri = uri
                        binding.ivCoverPhoto.setImageURI(uri)
                        uploadCoverPhoto(uri)
                    }
                }
            }
        }
    }

    private fun uploadCoverPhoto(uri: Uri) {
        val userId = SupabaseManager.getCurrentUserId() ?: return
        showProgressDialog("Uploading photo...")
        lifecycleScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }
                if (bytes != null) {
                    val path = "$userId/cover_${UUID.randomUUID()}.jpg"
                    SupabaseManager.uploadImage("thriftshop_db", path, bytes, object : SupabaseManager.SupabaseCallbackWithUrl {
                        override fun onResult(success: Boolean, url: String?, error: String?) {
                            if (success && url != null) {
                                // Delete old image if it exists
                                currentProfile?.shop_location?.let { oldUrl ->
                                    if (oldUrl.isNotEmpty() && oldUrl.startsWith("http")) {
                                        SupabaseManager.deleteImage("thriftshop_db", oldUrl)
                                    }
                                }
                                saveCoverUrlToProfile(userId, url)
                            } else {
                                hideProgressDialog()
                                Toast.makeText(this@SellerAccountActivity, "Upload failed: $error", Toast.LENGTH_SHORT).show()
                            }
                        }
                    })
                }
            } catch (e: Exception) { hideProgressDialog() }
        }
    }

    private fun saveCoverUrlToProfile(userId: String, url: String) {
        SupabaseManager.updateProfile(userId, mapOf("shop_location" to url), object : SupabaseManager.SupabaseCallback {
            override fun onResult(success: Boolean, error: String?) {
                lifecycleScope.launch {
                    hideProgressDialog()
                    if (success) {
                        Toast.makeText(this@SellerAccountActivity, "Cover photo updated", Toast.LENGTH_SHORT).show()
                        loadUserData()
                    } else {
                        Toast.makeText(this@SellerAccountActivity, "Failed to save URL: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun saveLocation(lat: Double, lng: Double) {
        val userId = SupabaseManager.getCurrentUserId() ?: return
        showProgressDialog("Saving location...")
        lifecycleScope.launch {
            try {
                SupabaseManager.client.postgrest["seller_markers"].delete { filter { eq("seller_id", userId) } }
                val marker = SupabaseManager.SellerMarker(seller_id = userId, latitude = lat, longitude = lng, title = currentProfile?.shop_name ?: "Shop")
                SupabaseManager.client.postgrest["seller_markers"].insert(marker)
                withContext(Dispatchers.Main) {
                    hideProgressDialog()
                    Toast.makeText(this@SellerAccountActivity, "Location saved", Toast.LENGTH_SHORT).show()
                    loadLocationData(userId)
                }
            } catch (e: Exception) { hideProgressDialog() }
        }
    }

    private fun showMiniMapLibrePreview(lat: Double, lon: Double) {
        binding.miniMapView.visibility = View.VISIBLE
        miniMapView?.onResume()
        miniMapView?.getMapAsync { map ->
            map.setStyle(Style.Builder().fromUri("https://api.maptiler.com/maps/streets/style.json?key=${getString(R.string.maptiler_api_key)}")) {
                val loc = LatLng(lat, lon)
                map.addMarker(MarkerOptions().position(loc).title("Shop Location"))
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15.0), 1000)
            }
        }
    }

    private fun showProgressDialog(msg: String) { progressDialog = AlertDialog.Builder(this).setMessage(msg).setCancelable(false).show() }
    private fun hideProgressDialog() { progressDialog?.dismiss() }

    private fun showSwitchToCustomerConfirmation() {
        AlertDialog.Builder(this).setTitle("Switch Mode").setMessage("Switch to customer mode?")
            .setPositiveButton("Switch") { _, _ ->
                startActivity(Intent(this, LandingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                finish()
            }.setNegativeButton("Cancel", null).show()
    }

    override fun onStart() { super.onStart(); miniMapView?.onStart() }
    override fun onResume() { super.onResume(); miniMapView?.onResume() }
    override fun onPause() { super.onPause(); miniMapView?.onPause() }
    override fun onStop() { super.onStop(); miniMapView?.onStop() }
}
