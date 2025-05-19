package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import android.widget.Toast;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class ForYouFragment extends Fragment {
    private RecyclerView recyclerView;
    private ProductListAdapter adapter;
    private List<Product> products;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_for_you, container, false);
        
        // Change bottom navigation text to "Home"
        if (getActivity() != null) {
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottomNavigation);
            if (bottomNav != null) {
                bottomNav.getMenu().findItem(R.id.navigation_for_you).setTitle("Home");
            }
        }
        
        recyclerView = view.findViewById(R.id.recyclerProducts);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        
        products = new ArrayList<>();
        adapter = new ProductListAdapter(getContext(), products, false);
        recyclerView.setAdapter(adapter);
        
        db = FirebaseFirestore.getInstance();
        loadAllProducts();
        
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Reset bottom navigation text back to "For You" when leaving this fragment
        if (getActivity() != null) {
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottomNavigation);
            if (bottomNav != null) {
                bottomNav.getMenu().findItem(R.id.navigation_for_you).setTitle("For You");
            }
        }
    }

    private void loadAllProducts() {
        db.collection("products")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                products.clear();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Product product = document.toObject(Product.class);
                    product.id = document.getId();
                    products.add(product);
                }
                adapter.notifyDataSetChanged();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed to load products", Toast.LENGTH_SHORT).show();
            });
    }
} 