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
import java.util.ArrayList;
import java.util.List;

public class CustomerOrderListFragment extends Fragment {
    private RecyclerView recyclerView;
    private OrdersAdapter adapter;
    private List<SupabaseManager.Order> orders = new ArrayList<>();
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
        // Note: OrdersAdapter might need to be updated to accept SupabaseManager.Order
        adapter = new OrdersAdapter(orders, true);
        recyclerView.setAdapter(adapter);
        
        customerId = SupabaseManager.getCurrentUserId();
        
        loadOrders();
        return view;
    }

    private void loadOrders() {
        if (customerId == null) return;

        SupabaseManager.getOrders(customerId, false, new SupabaseManager.SupabaseCallbackWithOrders() {
            @Override
            public void onResult(boolean success, List<SupabaseManager.Order> orderList, String error) {
                if (success && orderList != null) {
                    orders.clear();
                    if (statusFilter != null && !statusFilter.equals("all")) {
                        for (SupabaseManager.Order order : orderList) {
                            if (order.getStatus().equalsIgnoreCase(statusFilter)) {
                                orders.add(order);
                            }
                        }
                    } else {
                        orders.addAll(orderList);
                    }
                    adapter.notifyDataSetChanged();
                } else if (error != null) {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to load orders: " + error, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }
}
