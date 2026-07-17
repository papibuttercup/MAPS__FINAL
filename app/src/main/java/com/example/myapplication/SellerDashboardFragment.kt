package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.myapplication.databinding.FragmentSellerDashboardBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch

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
        
        setupClickListeners()
        loadDashboardData()
    }

    private fun setupClickListeners() {
        binding.cardMyProducts.setOnClickListener {
            navigateToTab(R.id.navigation_products)
        }
        
        binding.btnSeeAllOrders.setOnClickListener {
            navigateToTab(R.id.navigation_orders)
        }
        
        binding.btnSellerChats.setOnClickListener {
            startActivity(Intent(requireContext(), SellerChatListActivity::class.java))
        }
    }

    private fun navigateToTab(itemId: Int) {
        (activity as? SellerMainActivity)?.let { mainActivity ->
            val bottomNav = mainActivity.findViewById<BottomNavigationView>(R.id.bottomNavigation)
            bottomNav?.selectedItemId = itemId
        }
    }

    private fun loadDashboardData() {
        val userId = SupabaseManager.getCurrentUserId() ?: return

        lifecycleScope.launch {
            try {
                // Load product count
                val products = SupabaseManager.client.postgrest["products"]
                    .select {
                        filter {
                            eq("seller_id", userId)
                        }
                    }
                    .decodeList<SupabaseManager.ProductModel>()
                
                binding.tvTotalProducts.text = products.size.toString()
                binding.tvItemsListed.text = "${products.size} items listed"

                // Load active orders (Pending or Accepted)
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

                binding.tvActiveOrders.text = activeOrders.size.toString()

                // Load 2 most recent pending orders for "Needs your attention"
                val pendingOrders = SupabaseManager.client.postgrest["orders"]
                    .select {
                        filter {
                            eq("seller_id", userId)
                            eq("status", "pending")
                        }
                        order("created_at", Order.DESCENDING)
                        limit(2)
                    }
                    .decodeList<SupabaseManager.Order>()

                populatePendingOrders(pendingOrders)

            } catch (e: Exception) {
                Log.e("SellerDashboard", "Failed to load dashboard data: ${e.message}")
            }
        }
    }

    private fun populatePendingOrders(orders: List<SupabaseManager.Order>) {
        binding.ordersContainer.removeAllViews()
        
        if (orders.isEmpty()) {
            val emptyView = TextView(requireContext()).apply {
                text = "No pending orders"
                setTextColor(resources.getColor(R.color.gray_400))
                textSize = 14f
                gravity = android.view.Gravity.CENTER
                setPadding(0, 24, 0, 24)
            }
            binding.ordersContainer.addView(emptyView)
            return
        }

        for (order in orders) {
            val orderView = layoutInflater.inflate(R.layout.item_dashboard_order, binding.ordersContainer, false)
            
            val tvOrderNumber = orderView.findViewById<TextView>(R.id.tvOrderNumber)
            val tvOrderDetails = orderView.findViewById<TextView>(R.id.tvOrderDetails)
            val tvStatusBadge = orderView.findViewById<TextView>(R.id.tvStatusBadge)
            val ivOrderImage = orderView.findViewById<ImageView>(R.id.ivOrderImage)

            tvOrderNumber.text = "Order #${order.id?.takeLast(4) ?: ""}"
            tvOrderDetails.text = order.customer_name ?: "Unknown Customer"
            tvStatusBadge.text = "Pending"
            
            // In a real app, we'd load the first product image of the order. 
            // For now, using a placeholder.
            
            orderView.setOnClickListener {
                navigateToTab(R.id.navigation_orders)
            }
            
            binding.ordersContainer.addView(orderView)
        }
    }

    private val Int.sp: Float get() = this * resources.displayMetrics.scaledDensity

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
