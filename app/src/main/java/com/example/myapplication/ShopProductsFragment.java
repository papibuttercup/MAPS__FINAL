package com.example.myapplication;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import com.google.firebase.firestore.ListenerRegistration;

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
    private ImageButton btnMessageShop;
    private ListenerRegistration productListenerRegistration;

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
        btnMessageShop = view.findViewById(R.id.btnMessageShop);
        btnMessageShop.setOnClickListener(v -> {
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            if (currentUserId != null && sellerId != null && !currentUserId.equals(sellerId)) {
                Intent intent = new Intent(getActivity(), ChatActivity.class);
                intent.putExtra("otherUserId", sellerId);
                startActivity(intent);
            } else if (currentUserId != null && currentUserId.equals(sellerId)) {
                Toast.makeText(getContext(), "You cannot message your own shop.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Unable to start chat.", Toast.LENGTH_SHORT).show();
            }
        });
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
                if (mainCategory.equals("Men") && subcat.equals("Shirts")) {
                    subCategoryIcons.add(R.drawable.men_shirt);
                } else if (mainCategory.equals("Men") && subcat.equals("T-Shirts")) {
                    subCategoryIcons.add(R.drawable.men_tshirt);
                } else if (mainCategory.equals("Men") && subcat.equals("Pants")) {
                    subCategoryIcons.add(R.drawable.men_pants);
                } else if (mainCategory.equals("Men") && subcat.equals("Shorts")) {
                    subCategoryIcons.add(R.drawable.men_shorts);
                } else if (mainCategory.equals("Men") && subcat.equals("Jackets")) {
                    subCategoryIcons.add(R.drawable.men_jacket);
                } else if (mainCategory.equals("Men") && subcat.equals("Suits")) {
                    subCategoryIcons.add(R.drawable.men_suit);
                } else if (mainCategory.equals("Men") && subcat.equals("Sweaters")) {
                    subCategoryIcons.add(R.drawable.men_sweater);
                } else if (mainCategory.equals("Men") && subcat.equals("Hoodies")) {
                    subCategoryIcons.add(R.drawable.men_hoodie);
                } else if (mainCategory.equals("Men") && subcat.equals("Activewear")) {
                    subCategoryIcons.add(R.drawable.men_activewear);
                } else if (mainCategory.equals("Men") && subcat.equals("Underwear")) {
                    subCategoryIcons.add(R.drawable.men_underwear);
                } else if (mainCategory.equals("Men") && subcat.equals("Sleepwear")) {
                    subCategoryIcons.add(R.drawable.men_sleepwear);
                } else if (mainCategory.equals("Men") && subcat.equals("Jeans")) {
                    subCategoryIcons.add(R.drawable.men_jeans);
                } else if (mainCategory.equals("Women") && subcat.equals("Jackets")) {
                    subCategoryIcons.add(R.drawable.women_jacket);
                } else if (mainCategory.equals("Women") && subcat.equals("Tops")) {
                    subCategoryIcons.add(R.drawable.women_top);
                } else if (mainCategory.equals("Women") && subcat.equals("T-Shirts")) {
                    subCategoryIcons.add(R.drawable.women_tshirt);
                } else if (mainCategory.equals("Women") && subcat.equals("Dress")) {
                    subCategoryIcons.add(R.drawable.women_dress);
                } else if (mainCategory.equals("Women") && subcat.equals("Suits")) {
                    subCategoryIcons.add(R.drawable.women_suit);
                } else if (mainCategory.equals("Women") && subcat.equals("Sweaters")) {
                    subCategoryIcons.add(R.drawable.women_sweater);
                } else if (mainCategory.equals("Women") && subcat.equals("Hoodies")) {
                    subCategoryIcons.add(R.drawable.women_hoodie);
                } else if (mainCategory.equals("Women") && subcat.equals("Activewear")) {
                    subCategoryIcons.add(R.drawable.women_activewear);
                } else if (mainCategory.equals("Women") && subcat.equals("Beach wear")) {
                    subCategoryIcons.add(R.drawable.women_beachwear);
                } else if (mainCategory.equals("Women") && subcat.equals("Underwear")) {
                    subCategoryIcons.add(R.drawable.women_underwear);
                } else if (mainCategory.equals("Women") && subcat.equals("Sleepwear")) {
                    subCategoryIcons.add(R.drawable.women_sleepwear);
                } else if (mainCategory.equals("Women") && subcat.equals("Jeans")) {
                    subCategoryIcons.add(R.drawable.women_jean);
                } else if (mainCategory.equals("Women") && subcat.equals("Shorts")) {
                    subCategoryIcons.add(R.drawable.women_short);
                } else if (mainCategory.equals("Women") && subcat.equals("Skirts")) {
                    subCategoryIcons.add(R.drawable.women_skirt);
                } else if (mainCategory.equals("Women") && subcat.equals("Outerwear")) {
                    subCategoryIcons.add(R.drawable.women_outwear);
                } else if (mainCategory.equals("Women") && subcat.equals("Pants")) {
                    subCategoryIcons.add(R.drawable.women_pants);
                } else if (mainCategory.equals("Women") && subcat.equals("Shirts")) {
                    subCategoryIcons.add(R.drawable.women_shirt);
                } else if (mainCategory.equals("Kids") && subcat.equals("Jackets")) {
                    subCategoryIcons.add(R.drawable.kid_jacket);
                } else if (mainCategory.equals("Kids") && subcat.equals("T-Shirts")) {
                    subCategoryIcons.add(R.drawable.kid_tshirt);
                } else if (mainCategory.equals("Kids") && (subcat.equals("Dress") || subcat.equals("Dresses"))) {
                    subCategoryIcons.add(R.drawable.kid_dress);
                } else if (mainCategory.equals("Kids") && subcat.equals("Sweaters")) {
                    subCategoryIcons.add(R.drawable.kid_sweater);
                } else if (mainCategory.equals("Kids") && subcat.equals("Hoodies")) {
                    subCategoryIcons.add(R.drawable.kid_hoodie);
                } else if (mainCategory.equals("Kids") && subcat.equals("Underwear")) {
                    subCategoryIcons.add(R.drawable.kid_underwear);
                } else if (mainCategory.equals("Kids") && subcat.equals("Sleepwear")) {
                    subCategoryIcons.add(R.drawable.kid_sleepwear);
                } else if (mainCategory.equals("Kids") && subcat.equals("Shorts")) {
                    subCategoryIcons.add(R.drawable.kid_short);
                } else if (mainCategory.equals("Kids") && subcat.equals("Skirts")) {
                    subCategoryIcons.add(R.drawable.kid_skirt);
                } else if (mainCategory.equals("Kids") && subcat.equals("Pants")) {
                    subCategoryIcons.add(R.drawable.kid_pants);
                } else if (mainCategory.equals("Kids") && subcat.equals("Shirts")) {
                    subCategoryIcons.add(R.drawable.kid_shirt);
                } else {
                    subCategoryIcons.add(R.drawable.placeholder_image);
                }
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
        if (sellerId == null) return;

        // Stop previous listener if it exists
        if (productListenerRegistration != null) {
            productListenerRegistration.remove();
        }

        productListenerRegistration = db.collection("products")
            .whereEqualTo("sellerId", sellerId)
            .addSnapshotListener((queryDocumentSnapshots, error) -> {
                if (error != null) {
                    Toast.makeText(getContext(), "Error loading products: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                products.clear();
                if (queryDocumentSnapshots != null) {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Product product = doc.toObject(Product.class);
                        if (product != null) {
                            product.id = doc.getId();
                            products.add(product);
                        }
                    }
                }
                productAdapter.notifyDataSetChanged();
            });
    }

    @Override
    public void onCategoryClick(String category) {
        selectSubCategory(category);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove the snapshot listener when the view is destroyed
        if (productListenerRegistration != null) {
            productListenerRegistration.remove();
        }
    }
} 