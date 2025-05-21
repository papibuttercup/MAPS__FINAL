package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;
import android.util.Log;
import android.widget.Toast;
import android.app.AlertDialog;

public class SellerCanceledOrdersActivity extends AppCompatActivity {
    private RecyclerView recyclerOrders;
    private OrdersAdapter adapter;
    private List<Order> orderList = new ArrayList<>();
    private FirebaseFirestore db;
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

        db = FirebaseFirestore.getInstance();
        sellerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Canceled Orders");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        emptyView = findViewById(R.id.emptyView);

        adapter.setOnOrderClickListener(order -> showOrderDetailsDialog(order));

        loadCanceledOrders();
    }

    private void loadCanceledOrders() {
        db.collection("orders")
            .whereEqualTo("sellerId", sellerId)
            .whereEqualTo("status", "canceled")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                orderList.clear();
                Log.d("Orders", "Canceled orders found: " + queryDocumentSnapshots.size());
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    Order order = doc.toObject(Order.class);
                    if (order != null) {
                        order.orderId = doc.getId();  // Set the orderId from document ID
                        orderList.add(order);
                    }
                }
                adapter.notifyDataSetChanged();
                if (orderList.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                    recyclerOrders.setVisibility(View.GONE);
                } else {
                    emptyView.setVisibility(View.GONE);
                    recyclerOrders.setVisibility(View.VISIBLE);
                }
            })
            .addOnFailureListener(e -> {
                Log.e("Orders", "Error loading canceled orders", e);
                Toast.makeText(this, "Failed to load canceled orders: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void showOrderDetailsDialog(Order order) {
        StringBuilder details = new StringBuilder();
        details.append("Order ID: ").append(order.orderId).append("\n");
        details.append("Status: Canceled\n");
        details.append("Total: ₱").append(order.totalPrice).append("\n");
        details.append("Delivery: ").append(order.deliveryAddress).append("\n");
        details.append("Date: ").append(order.timestamp != null ? order.timestamp.toDate() : "").append("\n\n");
        details.append("Products:\n");
        if (order.products != null) {
            for (int i = 0; i < order.products.size(); i++) {
                Object name = order.products.get(i).get("name");
                Object qty = order.products.get(i).get("quantity");
                details.append("- ").append(name != null ? name : "").append(" x").append(qty != null ? qty : "1").append("\n");
            }
        }
        new AlertDialog.Builder(this)
            .setTitle("Canceled Order Details")
            .setMessage(details.toString())
            .setPositiveButton("OK", null)
            .show();
    }
} 