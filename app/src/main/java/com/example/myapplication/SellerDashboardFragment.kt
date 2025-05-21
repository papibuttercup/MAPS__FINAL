package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.FragmentSellerDashboardBinding

class SellerDashboardFragment : Fragment() {
    private var _binding: FragmentSellerDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSellerDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set up click listeners
        binding.cardListItem.setOnClickListener {
            startActivity(Intent(requireContext(), ListNewItemActivity::class.java))
        }
        
        binding.cardOrders.setOnClickListener {
            startActivity(Intent(requireContext(), SellerOrdersActivity::class.java))
        }
        
        binding.btnSellerChats.setOnClickListener {
            startActivity(Intent(requireContext(), SellerChatListActivity::class.java))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 