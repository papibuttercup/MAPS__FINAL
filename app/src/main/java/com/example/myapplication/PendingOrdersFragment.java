package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class PendingOrdersFragment extends Fragment {
    private RecyclerView recyclerView;
    private OrdersAdapter adapter;
    private List<Order> orders = new ArrayList<>();
    private FirebaseFirestore db;
    private String sellerId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_orders_list, container, false);
        recyclerView = view.findViewById(R.id.recyclerOrders);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OrdersAdapter(orders, false);
        recyclerView.setAdapter(adapter);
        db = FirebaseFirestore.getInstance();
        sellerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        loadOrders();
        return view;
    }

    private void loadOrders() {
        android.util.Log.d("PendingOrdersFragment", "Loading pending orders for seller: " + sellerId);
        
        db.collection("orders")
            .whereEqualTo("sellerId", sellerId)
            .whereEqualTo("status", "pending")  // Only get pending orders
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    android.util.Log.e("PendingOrdersFragment", "Error loading pending orders", error);
                    Toast.makeText(getContext(), "Failed to load orders", Toast.LENGTH_SHORT).show();
                    return;
                }

                orders.clear();
                if (value != null) {
                    android.util.Log.d("PendingOrdersFragment", "Received " + value.size() + " pending orders");
                    for (var doc : value) {
                        Order order = doc.toObject(Order.class);
                        if (order != null) {
                            order.orderId = doc.getId();
                            orders.add(order);
                            android.util.Log.d("PendingOrdersFragment", "Added order: " + order.orderId + 
                                ", Status: " + order.status);
                        }
                    }
                } else {
                    android.util.Log.d("PendingOrdersFragment", "No pending orders found");
                }
                adapter.notifyDataSetChanged();
            });
    }
} 