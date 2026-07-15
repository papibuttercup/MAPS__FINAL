package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.ActivityAccountBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AccountFragment : Fragment() {
    private var _binding: ActivityAccountBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.backButton.visibility = View.GONE

        setupClickListeners()
        loadUserData()
    }

    private fun setupClickListeners() {
        binding.logoutButton.setOnClickListener {
            auth.signOut()
            requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit().clear().apply()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }

        binding.editProfileButton.setOnClickListener {
             val intent = Intent(requireContext(), AccountSettingsActivity::class.java)
             startActivity(intent)
        }

        binding.viewOrderHistory.setOnClickListener {
            (activity as? LandingActivity)?.showOrders(0) // All
        }

        binding.statusUnpaid.setOnClickListener {
            (activity as? LandingActivity)?.showOrders(1) // To Pay
        }
        binding.statusProcessing.setOnClickListener {
            (activity as? LandingActivity)?.showOrders(2) // To Ship
        }
        binding.statusShipped.setOnClickListener {
            (activity as? LandingActivity)?.showOrders(3) // To Receive
        }
        binding.statusReturns.setOnClickListener {
            (activity as? LandingActivity)?.showOrders(5) // Returns
        }

        val comingSoonListener = View.OnClickListener {
            Toast.makeText(requireContext(), "Coming soon!", Toast.LENGTH_SHORT).show()
        }

        binding.itemAbout.itemTitle.text = "About Thrifty"
        binding.itemShipping.itemTitle.text = "Shipping policy"
        binding.itemPayment.itemTitle.text = "Payment methods"
        binding.itemTerms.itemTitle.text = "Terms and conditions"
        binding.itemPrivacy.itemTitle.text = "Privacy policy"
        binding.itemSocial.itemTitle.text = "Social responsibility"
        binding.itemCareers.itemTitle.text = "Careers"

        binding.itemAbout.root.setOnClickListener {
            val intent = Intent(requireContext(), FAQActivity::class.java)
            startActivity(intent)
        }
        binding.itemShipping.root.setOnClickListener(comingSoonListener)
        binding.itemPayment.root.setOnClickListener(comingSoonListener)
        binding.itemTerms.root.setOnClickListener(comingSoonListener)
        binding.itemPrivacy.root.setOnClickListener(comingSoonListener)
        binding.itemSocial.root.setOnClickListener(comingSoonListener)
        binding.itemCareers.root.setOnClickListener(comingSoonListener)

        binding.switchToSellerButton.setOnClickListener {
            showSwitchToSellerConfirmation()
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            binding.userEmail.text = currentUser.email
            db.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    if (isAdded && documentSnapshot.exists()) {
                        val firstName = documentSnapshot.getString("firstName")
                        val lastName = documentSnapshot.getString("lastName")
                        val fullName = when {
                            !firstName.isNullOrEmpty() && !lastName.isNullOrEmpty() -> "$firstName $lastName"
                            !firstName.isNullOrEmpty() -> firstName
                            !lastName.isNullOrEmpty() -> lastName
                            else -> "User"
                        }
                        binding.userName.text = fullName
                        val wasSeller = documentSnapshot.getBoolean("switchedFromSeller") ?: false
                        if (wasSeller) {
                            binding.switchToSellerButton.visibility = View.VISIBLE
                        } else {
                            binding.switchToSellerButton.visibility = View.GONE
                        }
                    } else if (isAdded) {
                        binding.userName.text = "User"
                        binding.switchToSellerButton.visibility = View.GONE
                    }
                }
                .addOnFailureListener {
                    if (isAdded) {
                        binding.userName.text = "User"
                        binding.switchToSellerButton.visibility = View.GONE
                        Toast.makeText(requireContext(), "Error loading user data", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }
    }

    private fun showSwitchToSellerConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Switch to Seller Mode")
            .setMessage("Are you sure you want to switch back to seller mode? You can switch to customer mode anytime from your seller profile.")
            .setPositiveButton("Switch") { _, _ ->
                switchToSellerMode()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun switchToSellerMode() {
        val userId = auth.currentUser?.uid ?: return
        val userEmail = auth.currentUser?.email ?: return
        db.collection("sellers").document(userId)
            .get()
            .addOnSuccessListener { sellerDoc ->
                if (isAdded) {
                    if (sellerDoc != null && sellerDoc.exists()) {
                        val intent = Intent(requireContext(), SellerMainActivity::class.java)
                        intent.putExtra("accountType", "seller")
                        intent.putExtra("email", userEmail)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        activity?.finish()
                    } else {
                        Toast.makeText(requireContext(), "Seller account not found", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) Toast.makeText(requireContext(), "Failed to switch to seller mode: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
