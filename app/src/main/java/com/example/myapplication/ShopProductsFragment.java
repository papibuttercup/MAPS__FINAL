package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ShopProductsFragment extends Fragment implements CategoryAdapter.OnCategoryClickListener {
    private static final String ARG_SELLER_ID = "sellerId";
    private static final String ARG_SHOP_NAME = "shopName";

    private String sellerId;
    private String shopName;
    private RecyclerView categoryRecyclerView, productRecyclerView;
    private CategoryAdapter categoryAdapter;
    private ProductListAdapter productAdapter;
    private List<String> categories = new ArrayList<>();
    private List<Product> products = new ArrayList<>();
    private FirebaseFirestore db;
    private String selectedCategory = null;
    private TextView shopTitle;

    public static ShopProductsFragment newInstance(String sellerId, String shopName) {
        ShopProductsFragment fragment = new ShopProductsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SELLER_ID, sellerId);
        args.putString(ARG_SHOP_NAME, shopName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shop_products, container, false);
        if (getArguments() != null) {
            sellerId = getArguments().getString(ARG_SELLER_ID);
            shopName = getArguments().getString(ARG_SHOP_NAME);
        }
        db = FirebaseFirestore.getInstance();
        shopTitle = view.findViewById(R.id.shopTitle);
        shopTitle.setText(shopName);
        categoryRecyclerView = view.findViewById(R.id.categoryRecyclerView);
        productRecyclerView = view.findViewById(R.id.productRecyclerView);
        categoryRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        productRecyclerView.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(getContext(), 2));
        categoryAdapter = new CategoryAdapter(categories, this);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("sellers")
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                boolean isSeller = documentSnapshot.exists();
                productAdapter = new ProductListAdapter(getContext(), products, isSeller);
                productRecyclerView.setAdapter(productAdapter);
                productAdapter.setOnDeleteProductListener(product -> {
                    android.widget.Toast.makeText(getContext(), "Delete clicked for: " + product.name, android.widget.Toast.LENGTH_SHORT).show();
                    FirebaseFirestore.getInstance().collection("products")
                        .document(product.id)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            int index = -1;
                            for (int i = 0; i < products.size(); i++) {
                                if (products.get(i).id.equals(product.id)) {
                                    index = i;
                                    break;
                                }
                            }
                            if (index != -1) {
                                products.remove(index);
                                productAdapter.notifyItemRemoved(index);
                            }
                            android.widget.Toast.makeText(getContext(), "Product deleted successfully!", android.widget.Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            android.widget.Toast.makeText(getContext(), "Failed to delete product: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                        });
                });
            });
        loadAllProductsForSeller();
        loadCategories();
        return view;
    }

    private void loadCategories() {
        db.collection("products")
            .whereEqualTo("sellerId", sellerId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                Set<String> categorySet = new HashSet<>();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    String category = doc.getString("category");
                    if (category != null && !category.isEmpty()) {
                        categorySet.add(category);
                    }
                }
                categories.clear();
                categories.addAll(categorySet);
                categoryAdapter.notifyDataSetChanged();
                if (!categories.isEmpty()) {
                    selectCategory(categories.get(0));
                }
            });
    }

    private void selectCategory(String category) {
        selectedCategory = category;
        loadProductsForCategory();
    }

    private void loadProductsForCategory() {
        db.collection("products")
            .whereEqualTo("sellerId", sellerId)
            .whereEqualTo("category", selectedCategory)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                products.clear();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Product product = doc.toObject(Product.class);
                    products.add(product);
                }
                productAdapter.notifyDataSetChanged();
            });
    }

    private void loadAllProductsForSeller() {
        db.collection("products")
            .whereEqualTo("sellerId", sellerId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                products.clear();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Product product = doc.toObject(Product.class);
                    products.add(product);
                }
                productAdapter.notifyDataSetChanged();
            });
    }

    @Override
    public void onCategoryClick(String category) {
        selectCategory(category);
    }
} 