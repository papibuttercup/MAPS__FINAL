package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.myapplication.databinding.FragmentSellerProductsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SellerProductsFragment : Fragment() {
    private var _binding: FragmentSellerProductsBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: ProductAdapter
    private val productList = mutableListOf<SellerProduct>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSellerProductsBinding.inflate(inflater, container, false)
        
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setupRecyclerView()
        setupButtons()
        loadProducts()

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter(productList)
        binding.rvProducts.layoutManager = GridLayoutManager(context, 2)
        binding.rvProducts.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnListItem.setOnClickListener {
            startActivity(Intent(requireContext(), ListNewItemActivity::class.java))
        }
    }

    private fun loadProducts() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("products")
            .whereEqualTo("sellerId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                productList.clear()
                snapshot?.forEach { doc ->
                    val product = SellerProduct(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        stock = (doc.getLong("stock") ?: 0L).toInt(),
                        imageUrl = doc.getString("imageUrl") ?: "",
                        status = doc.getString("status") ?: ""
                    )
                    productList.add(product)
                }
                adapter.notifyDataSetChanged()
                binding.tvTotalProducts.text = productList.size.toString()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 