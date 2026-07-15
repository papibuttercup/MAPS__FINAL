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

public class CustomerOrderListFragment extends Fragment {
    private RecyclerView recyclerView;
    private OrdersAdapter adapter;
    private List<Order> orders = new ArrayList<>();
    private FirebaseFirestore db;
    private String customerId;
    private String statusFilter;

    public static CustomerOrderListFragment newInstance(String status) {
        CustomerOrderListFragment fragment = new CustomerOrderListFragment();
        Bundle args = new Bundle();
        args.putString("status", status);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_orders_list, container, false);
        
        if (getArguments() != null) {
            statusFilter = getArguments().getString("status");
        }

        recyclerView = view.findViewById(R.id.recyclerOrders);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OrdersAdapter(orders, true);
        recyclerView.setAdapter(adapter);
        
        db = FirebaseFirestore.getInstance();
        customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        loadOrders();
        return view;
    }

    private void loadOrders() {
        Query query = db.collection("orders")
            .whereEqualTo("customerId", customerId);

        if (statusFilter != null && !statusFilter.equals("all")) {
            query = query.whereEqualTo("status", statusFilter);
        }

        query.orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to load orders: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                orders.clear();
                if (value != null) {
                    for (var doc : value) {
                        Order order = doc.toObject(Order.class);
                        order.orderId = doc.getId();
                        orders.add(order);
                    }
                }
                adapter.notifyDataSetChanged();
            });
    }
}