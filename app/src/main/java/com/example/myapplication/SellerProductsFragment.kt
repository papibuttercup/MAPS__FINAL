package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.FragmentSellerProductsBinding

class SellerProductsFragment : Fragment() {
    private var _binding: FragmentSellerProductsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSellerProductsBinding.inflate(inflater, container, false)

        binding.btnListItem.setOnClickListener {
            // Navigate to ListNewItemActivity
            startActivity(android.content.Intent(requireContext(), ListNewItemActivity::class.java))
        }
        binding.btnIdentifyLocation.setOnClickListener {
            // Navigate to Maps Activity (for shop location)
            startActivity(android.content.Intent(requireContext(), Maps::class.java))
        }
        binding.btnOrders.setOnClickListener {
            // Navigate to ListedItemsActivity (for orders)
            startActivity(android.content.Intent(requireContext(), ListedItemsActivity::class.java))
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 