package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class CategoryProductsFragment extends Fragment {
    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private List<SupabaseManager.ProductModel> productList;
    private String categoryName;
    private String mainCategory;

    public static CategoryProductsFragment newInstance(String category) {
        CategoryProductsFragment fragment = new CategoryProductsFragment();
        Bundle args = new Bundle();
        args.putString("category", category);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categoryName = getArguments().getString("category");
            mainCategory = getArguments().getString("mainCategory");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_seller_products, container, false);
        
        // Hide total products count text if visible in layout
        View totalProducts = view.findViewById(R.id.tvTotalProducts);
        if (totalProducts != null) totalProducts.setVisibility(View.GONE);
        View btnListItem = view.findViewById(R.id.btnListItem);
        if (btnListItem != null) btnListItem.setVisibility(View.GONE);

        recyclerView = view.findViewById(R.id.rvProducts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        productList = new ArrayList<>();
        adapter = new ProductAdapter(productList, false);
        recyclerView.setAdapter(adapter);
        
        loadProducts();
        
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProducts();
    }

    private void loadProducts() {
        if (mainCategory == null || categoryName == null) return;
        
        SupabaseManager.getProductsByCategory(mainCategory, categoryName, new SupabaseManager.SupabaseCallbackWithProducts() {
            @Override
            public void onResult(boolean success, List<SupabaseManager.ProductModel> products, String error) {
                if (success && products != null) {
                    productList.clear();
                    productList.addAll(products);
                    adapter.notifyDataSetChanged();
                } else if (!success) {
                    if (getContext() != null) {
                        android.widget.Toast.makeText(getContext(), "Error: " + error, android.widget.Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }
}
