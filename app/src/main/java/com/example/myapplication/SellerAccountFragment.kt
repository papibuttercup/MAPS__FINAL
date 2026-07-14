package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.myapplication.databinding.ActivitySellerAccountBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

class SellerAccountFragment : Fragment() {
    private var _binding: ActivitySellerAccountBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var shopDocId: String? = null
    private var coverPhotoUri: Uri? = null
    private var coverPhotoUrl: String? = null
    private val PICK_COVER_PHOTO_REQUEST = 101
    private var miniMapView: MapView? = null
    private var miniMapLibreMap: MapLibreMap? = null
    private var progressDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        MapLibre.getInstance(requireContext())
        _binding = ActivitySellerAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        miniMapView = binding.miniMapView
        miniMapView?.onCreate(savedInstanceState)

        binding.backButton.visibility = View.GONE

        loadUserData()
        setupButtons()
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("sellers").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (isAdded && document != null) {
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
                        binding.btnDeleteCoverPhoto.visibility = View.VISIBLE
                        binding.btnSetCoverPhoto.text = "Change Cover Photo"
                    } else {
                        binding.ivCoverPhoto.setImageResource(R.drawable.ic_add_photo)
                        binding.btnDeleteCoverPhoto.visibility = View.GONE
                        binding.btnSetCoverPhoto.text = "Set Cover Photo"
                    }

                    // Set current location if available
                    val latitude = document.getDouble("latitude")
                    val longitude = document.getDouble("longitude")
                    val locationName = document.getString("locationName") ?: "Unknown Location"
                    if (latitude != null && longitude != null) {
                        binding.tvCurrentLocation.text = "$locationName (Location set)"
                        binding.btnDeleteLocation.visibility = View.VISIBLE
                        binding.btnIdentifyLocation.visibility = View.GONE
                        binding.miniMapView.visibility = View.VISIBLE
                        showMiniMapLibrePreview(latitude, longitude)
                    } else {
                        binding.tvCurrentLocation.text = "No location set"
                        binding.btnDeleteLocation.visibility = View.GONE
                        binding.btnIdentifyLocation.visibility = View.VISIBLE
                        binding.miniMapView.visibility = View.GONE
                    }

                    shopDocId = document.id
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    Log.e("SellerAccount", "Failed to load user data: ${e.message}")
                    Toast.makeText(requireContext(), "Failed to load profile data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun setupButtons() {
        // Shop Location Button
        binding.btnIdentifyLocation.setOnClickListener {
            val intent = Intent(requireContext(), Maps::class.java)
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
                                if (isAdded) {
                                    binding.ivCoverPhoto.setImageResource(R.drawable.ic_add_photo)
                                    binding.btnDeleteCoverPhoto.visibility = View.GONE
                                    binding.btnSetCoverPhoto.text = "Set Photo"
                                    binding.btnSave.visibility = View.GONE
                                    coverPhotoUrl = null
                                    Toast.makeText(requireContext(), "Cover photo deleted successfully", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener {
                                hideProgressDialog()
                                if (isAdded) Toast.makeText(requireContext(), "Failed to delete cover photo from storage", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        hideProgressDialog()
                        if (isAdded) {
                            binding.ivCoverPhoto.setImageResource(R.drawable.ic_add_photo)
                            binding.btnDeleteCoverPhoto.visibility = View.GONE
                            binding.btnSetCoverPhoto.text = "Set Photo"
                            binding.btnSave.visibility = View.GONE
                            coverPhotoUrl = null
                            Toast.makeText(requireContext(), "Cover photo deleted successfully", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener {
                    hideProgressDialog()
                    if (isAdded) Toast.makeText(requireContext(), "Failed to delete cover photo", Toast.LENGTH_SHORT).show()
                }
        }

        // Logout Button
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
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
                    if (isAdded) {
                        binding.tvCurrentLocation.text = "No location set"
                        binding.btnDeleteLocation.visibility = View.GONE
                        binding.btnIdentifyLocation.visibility = View.VISIBLE
                        binding.miniMapView.visibility = View.GONE
                        Toast.makeText(requireContext(), "Location deleted successfully", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    hideProgressDialog()
                    if (isAdded) Toast.makeText(requireContext(), "Failed to delete location: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showMiniMapLibrePreview(latitude: Double, longitude: Double) {
        binding.miniMapView.visibility = View.VISIBLE
        miniMapView?.onResume()
        val mapTilerApiKey = getString(R.string.maptiler_api_key)
        val styleUrl = "https://api.maptiler.com/maps/streets/style.json?key=$mapTilerApiKey"
        miniMapView?.getMapAsync { map ->
            miniMapLibreMap = map
            map.setStyle(Style.Builder().fromUri(styleUrl)) { style ->
                map.markers.forEach { map.removeMarker(it) }
                val location = LatLng(latitude, longitude)
                map.addMarker(MarkerOptions()
                    .position(location)
                    .title("Shop Location"))
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(location, 15.0),
                    1000
                )
            }
        }
    }

    private fun showProgressDialog(message: String) {
        progressDialog = AlertDialog.Builder(requireContext())
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
                        binding.btnSave.visibility = View.VISIBLE
                        binding.btnDeleteCoverPhoto.visibility = View.GONE
                        binding.btnSetCoverPhoto.text = "Change Cover Photo"
                        Toast.makeText(requireContext(), "Cover photo selected. Click Save Photo to save your changes!", Toast.LENGTH_LONG).show()
                    }
                }
                200 -> {
                    val latitude = data?.getDoubleExtra("latitude", 0.0) ?: 0.0
                    val longitude = data?.getDoubleExtra("longitude", 0.0) ?: 0.0
                    val locationName = data?.getStringExtra("locationName") ?: "Location"
                    
                    if (latitude != 0.0 && longitude != 0.0) {
                        binding.tvCurrentLocation.text = "$locationName (Location set)"
                        binding.btnDeleteLocation.visibility = View.VISIBLE
                        binding.btnIdentifyLocation.visibility = View.GONE
                        binding.miniMapView.visibility = View.VISIBLE
                        showMiniMapLibrePreview(latitude, longitude)
                        
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
                                if (isAdded) Toast.makeText(requireContext(), "Location saved successfully", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                if (isAdded) Toast.makeText(requireContext(), "Failed to save location: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }
        }
    }

    private fun saveShopProfile() {
        val userId = auth.currentUser?.uid ?: return
        val shopName = binding.etShopName.text.toString().trim()
        val shopDescription = binding.etShopDescription.text.toString().trim()

        if (shopName.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a shop name", Toast.LENGTH_SHORT).show()
            return
        }

        showProgressDialog("Saving profile...")

        val updates = mutableMapOf(
            "shopName" to shopName,
            "shopDescription" to shopDescription
        )

        if (coverPhotoUri != null) {
            val storageRef = FirebaseStorage.getInstance().reference
                .child("cover_photos")
                .child(userId)
                .child("cover_photo.jpg")

            storageRef.putFile(coverPhotoUri!!)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        updates["coverPhotoUrl"] = downloadUri.toString()
                        saveToFirestore(userId, updates)
                        if (isAdded) {
                            binding.btnSave.visibility = View.GONE
                            binding.btnDeleteCoverPhoto.visibility = View.VISIBLE
                            binding.btnSetCoverPhoto.text = "Change Cover Photo"
                            coverPhotoUrl = downloadUri.toString()
                        }
                    }.addOnFailureListener { e ->
                        hideProgressDialog()
                        if (isAdded) Toast.makeText(requireContext(), "Failed to get cover photo URL: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    hideProgressDialog()
                    if (isAdded) Toast.makeText(requireContext(), "Failed to upload cover photo: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            saveToFirestore(userId, updates)
        }
    }

    private fun saveToFirestore(userId: String, updates: Map<String, Any>) {
        db.collection("sellers").document(userId)
            .update(updates)
            .addOnSuccessListener {
                hideProgressDialog()
                if (isAdded) {
                    if (updates.containsKey("coverPhotoUrl")) {
                        Toast.makeText(requireContext(), "Profile and cover photo updated successfully!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    }
                    coverPhotoUri = null
                }
            }
            .addOnFailureListener { e ->
                hideProgressDialog()
                if (isAdded) Toast.makeText(requireContext(), "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showSwitchToCustomerConfirmation() {
        AlertDialog.Builder(requireContext())
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

        db.collection("users").document(userId)
            .update("switchedFromSeller", true)
            .addOnSuccessListener {
                if (isAdded) {
                    val intent = Intent(requireContext(), LandingActivity::class.java)
                    intent.putExtra("accountType", "customer")
                    intent.putExtra("email", userEmail)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    activity?.finish()
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) Toast.makeText(requireContext(), "Failed to switch to customer mode: ${e.message}", Toast.LENGTH_SHORT).show()
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

    override fun onDestroyView() {
        super.onDestroyView()
        miniMapView?.onDestroy()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        miniMapView?.onSaveInstanceState(outState)
    }
}
