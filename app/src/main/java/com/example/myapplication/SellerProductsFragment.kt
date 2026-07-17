package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.myapplication.databinding.FragmentSellerProductsBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class SellerProductsFragment : Fragment() {
    private var _binding: FragmentSellerProductsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ProductAdapter
    private val productList = mutableListOf<SupabaseManager.ProductModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSellerProductsBinding.inflate(inflater, container, false)
        
        setupRecyclerView()
        setupButtons()
        setupSwipeRefresh()
        loadProducts()

        return binding.root
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadProducts()
        }
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter(productList, true)
        binding.rvProducts.layoutManager = GridLayoutManager(context, 2)
        binding.rvProducts.adapter = adapter
        adapter.setOnEditProductListener(object : ProductAdapter.OnEditProductListener {
            override fun onEditProduct(product: SupabaseManager.ProductModel) {
                val intent = Intent(requireContext(), ListNewItemActivity::class.java)
                intent.putExtra("productId", product.id)
                startActivity(intent)
            }
        })
    }

    private fun setupButtons() {
        binding.btnListItem.setOnClickListener {
            startActivity(Intent(requireContext(), ListNewItemActivity::class.java))
        }
        binding.fabAddProduct.setOnClickListener {
            startActivity(Intent(requireContext(), ListNewItemActivity::class.java))
        }
    }

    private fun loadProducts() {
        val session = SupabaseManager.client.auth.currentSessionOrNull()
        if (session == null) {
            Log.d("SellerProductsFragment", "No active session found")
            binding.swipeRefresh.isRefreshing = false
            return
        }
        val userId = session.user?.id ?: return
        val userEmail = session.user?.email ?: "Unknown"
        Log.d("SellerProductsFragment", "Loading products for userId: $userId ($userEmail)")

        lifecycleScope.launch {
            try {
                val response = SupabaseManager.client.postgrest["products"]
                    .select {
                        filter {
                            eq("seller_id", userId)
                        }
                    }
                
                Log.d("SellerProductsFragment", "Raw response data: ${response.data}")
                val products = response.decodeList<SupabaseManager.ProductModel>()
                Log.d("SellerProductsFragment", "Decoded ${products.size} products")

                if (_binding == null) return@launch
                
                productList.clear()
                productList.addAll(products)
                adapter.notifyDataSetChanged()
                binding.tvTotalProducts.text = productList.size.toString()
                binding.swipeRefresh.isRefreshing = false

                if (products.isEmpty()) {
                    Log.d("SellerProductsFragment", "No products found for user $userId")
                }
            } catch (e: Exception) {
                Log.e("SellerProductsFragment", "Error loading products", e)
                if (_binding != null) {
                    binding.swipeRefresh.isRefreshing = false
                }
                context?.let {
                    android.widget.Toast.makeText(it, "Error loading products: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadProducts()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 