package com.example.myapplication;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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
    private RecyclerView categoryRecyclerView, subcategoryRecyclerView, productRecyclerView;
    private CategoryAdapter categoryAdapter, subcategoryAdapter;
    private ProductListAdapter productAdapter;
    private List<String> mainCategories = new ArrayList<>();
    private List<Integer> mainCategoryIcons = new ArrayList<>();
    private List<String> subCategories = new ArrayList<>();
    private List<Integer> subCategoryIcons = new ArrayList<>();
    private List<Product> products = new ArrayList<>();
    private FirebaseFirestore db;
    private String selectedCategory = null;
    private TextView shopTitle;
    private Spinner mainCategorySpinner;

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
        mainCategorySpinner = view.findViewById(R.id.mainCategorySpinner);
        subcategoryRecyclerView = view.findViewById(R.id.subcategoryRecyclerView);
        productRecyclerView = view.findViewById(R.id.productRecyclerView);
        subcategoryRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        productRecyclerView.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(getContext(), 2));

        // Setup main categories
        setupMainCategories();
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, mainCategories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mainCategorySpinner.setAdapter(spinnerAdapter);
        mainCategorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadSubCategories(mainCategories.get(position));
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Setup subcategory adapter (empty initially)
        subcategoryAdapter = new CategoryAdapter(subCategories, subCategoryIcons, subcategory -> {
            selectSubCategory(subcategory);
        });
        subcategoryRecyclerView.setAdapter(subcategoryAdapter);

        // Load default (first) main category's subcategories
        if (!mainCategories.isEmpty()) {
            loadSubCategories(mainCategories.get(0));
        }

        // Initialize product adapter with click listener
        productAdapter = new ProductListAdapter(getContext(), products, false);
        productAdapter.setOnProductClickListener(product -> {
            Intent intent = new Intent(getActivity(), ProductDetailsActivity.class);
            intent.putExtra("productId", product.id);
            startActivity(intent);
        });
        productRecyclerView.setAdapter(productAdapter);
        loadAllProductsForSeller();
        return view;
    }

    private void setupMainCategories() {
        mainCategories.clear();
        mainCategories.add("Women");
        mainCategories.add("Men");
        mainCategories.add("Kids");
    }

    private void loadSubCategories(String mainCategory) {
        subCategories.clear();
        subCategoryIcons.clear();
        Resources res = getResources();
        int arrayId = 0;
        if (mainCategory.equals("Women")) {
            arrayId = R.array.categories_women;
        } else if (mainCategory.equals("Men")) {
            arrayId = R.array.categories_men;
        } else if (mainCategory.equals("Kids")) {
            arrayId = R.array.categories_kids;
        }
        if (arrayId != 0) {
            String[] subcats = res.getStringArray(arrayId);
            for (String subcat : subcats) {
                subCategories.add(subcat);
                subCategoryIcons.add(R.drawable.placeholder_image);
            }
        }
        subcategoryAdapter.notifyDataSetChanged();
        // Auto-select first subcategory
        if (!subCategories.isEmpty()) {
            selectSubCategory(subCategories.get(0));
        }
    }

    private void selectSubCategory(String subcategory) {
        selectedCategory = subcategory;
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
                    product.id = doc.getId();
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
                    product.id = doc.getId();
                    products.add(product);
                }
                productAdapter.notifyDataSetChanged();
            });
    }

    @Override
    public void onCategoryClick(String category) {
        selectSubCategory(category);
    }
} 