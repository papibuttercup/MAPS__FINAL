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

public class CanceledOrdersFragment extends Fragment {
    private OrdersAdapter adapter;
    private List<SupabaseManager.Order> orders = new ArrayList<>();
    private final OrderRepository repository = new OrderRepository();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_orders_list, container, false);
        RecyclerView rv = view.findViewById(R.id.recyclerOrders);
        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new OrdersAdapter(orders, false);
            rv.setAdapter(adapter);
        }
        loadOrders();
        return view;
    }

    private void loadOrders() {
        repository.listenToSellerOrders((list, error) -> {
            if (!isAdded()) return;
            if (error != null) { 
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show(); 
                return; 
            }
            orders.clear();
            if (list != null) {
                for (SupabaseManager.Order o : list) {
                    if ("canceled".equals(o.getStatus())) {
                        orders.add(o);
                    }
                }
            }
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        });
    }
}
