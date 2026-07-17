package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import android.util.Log;
import android.widget.Toast;
import android.app.AlertDialog;

public class SellerCanceledOrdersActivity extends AppCompatActivity {
    private RecyclerView recyclerOrders;
    private OrdersAdapter adapter;
    private List<SupabaseManager.Order> orderList = new ArrayList<>();
    private String sellerId;
    private TextView emptyView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_canceled_orders);

        recyclerOrders = findViewById(R.id.recyclerOrders);
        recyclerOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrdersAdapter(orderList, false);  // Set isCustomerView to false
        recyclerOrders.setAdapter(adapter);

        sellerId = SupabaseManager.getCurrentUserId();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Canceled Orders");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        emptyView = findViewById(R.id.emptyView);

        adapter.setOnOrderClickListener(order -> showOrderDetailsDialog(order));

        if (sellerId != null) {
            loadCanceledOrders();
        } else {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadCanceledOrders() {
        SupabaseManager.getOrders(sellerId, true, new SupabaseManager.SupabaseCallbackWithOrders() {
            @Override
            public void onResult(boolean success, List<SupabaseManager.Order> orders, String error) {
                if (success && orders != null) {
                    orderList.clear();
                    for (SupabaseManager.Order order : orders) {
                        if ("canceled".equalsIgnoreCase(order.getStatus())) {
                            orderList.add(order);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    updateVisibility();
                } else {
                    Log.e("Orders", "Error loading canceled orders: " + error);
                    Toast.makeText(SellerCanceledOrdersActivity.this, "Failed to load: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateVisibility() {
        if (orderList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerOrders.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerOrders.setVisibility(View.VISIBLE);
        }
    }

    private void showOrderDetailsDialog(SupabaseManager.Order order) {
        StringBuilder details = new StringBuilder();
        details.append("Order ID: ").append(order.getId()).append("\n");
        details.append("Status: Canceled\n");
        details.append("Total: ₱").append(order.getTotal_amount()).append("\n");
        details.append("Delivery: ").append(order.getDelivery_address()).append("\n");
        details.append("Date: ").append(order.getCreated_at() != null ? order.getCreated_at() : "").append("\n\n");
        
        // SupabaseManager.Order doesn't currently store product list details in the data class
        // but it has the total amount.
        
        new AlertDialog.Builder(this)
            .setTitle("Canceled Order Details")
            .setMessage(details.toString())
            .setPositiveButton("OK", null)
            .show();
    }
}
