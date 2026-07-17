package com.example.myapplication;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import android.widget.EditText;
import android.widget.ImageView;
import android.graphics.Color;
import androidx.core.content.ContextCompat;
import com.google.android.material.tabs.TabLayout;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Collectors;

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
    private String selectedCategory = null;
    private TextView shopTitle;
    private Spinner mainCategorySpinner;
    private Button btnMessageShop;
    private EditText etSearchInShop;
    private TextView txtShopRating, txtFollowersCount;
    private ImageView imgShopLogo;
    private View layoutProductsView, layoutCategoriesView;
    private TabLayout shopTabLayout;
    private TextView btnSortPopular, btnSortLatest, btnSortPrice;
    private Button btnFollowShop;
    private boolean isFollowing = false;
    private String currentSearchQuery = "";
    private String currentSortOrder = "latest"; // "popular", "latest", "price_asc", "price_desc"

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

        // Initialize Header & Shop Info
        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().onBackPressed());
        etSearchInShop = view.findViewById(R.id.etSearchInShop);
        shopTitle = view.findViewById(R.id.shopTitle);
        shopTitle.setText(shopName);
        txtShopRating = view.findViewById(R.id.txtShopRating);
        txtFollowersCount = view.findViewById(R.id.txtFollowersCount);
        imgShopLogo = view.findViewById(R.id.imgShopLogo);
        btnMessageShop = view.findViewById(R.id.btnMessageShop);
        btnFollowShop = view.findViewById(R.id.btnFollowShop);

        String currentUserId = SupabaseManager.getCurrentUserId();
        boolean isOwner = currentUserId != null && currentUserId.equals(sellerId);

        if (isOwner) {
            btnFollowShop.setVisibility(View.GONE);
            btnMessageShop.setVisibility(View.GONE);
        }

        // Action Buttons
        btnMessageShop.setOnClickListener(v -> {
            if (currentUserId != null && sellerId != null && !currentUserId.equals(sellerId)) {
                Intent intent = new Intent(getActivity(), ChatActivity.class);
                intent.putExtra("otherUserId", sellerId);
                startActivity(intent);
            } else if (currentUserId != null && currentUserId.equals(sellerId)) {
                Toast.makeText(getContext(), "You cannot message your own shop.", Toast.LENGTH_SHORT).show();
            } else if (currentUserId == null) {
                Toast.makeText(getContext(), "Please log in to message the shop.", Toast.LENGTH_SHORT).show();
            }
        });

        btnFollowShop.setOnClickListener(v -> toggleFollow());

        // Tabs
        shopTabLayout = view.findViewById(R.id.shopTabLayout);
        layoutProductsView = view.findViewById(R.id.layoutProductsView);
        layoutCategoriesView = view.findViewById(R.id.layoutCategoriesView);

        shopTabLayout.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    layoutProductsView.setVisibility(View.VISIBLE);
                    layoutCategoriesView.setVisibility(View.GONE);
                } else {
                    layoutProductsView.setVisibility(View.GONE);
                    layoutCategoriesView.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
        });

        // Search logic
        etSearchInShop.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase().trim();
                applyFiltersAndSort();
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // Sorting buttons
        btnSortPopular = view.findViewById(R.id.btnSortPopular);
        btnSortLatest = view.findViewById(R.id.btnSortLatest);
        btnSortPrice = view.findViewById(R.id.btnSortPrice);

        btnSortPopular.setOnClickListener(v -> updateSort("popular"));
        btnSortLatest.setOnClickListener(v -> updateSort("latest"));
        btnSortPrice.setOnClickListener(v -> {
            if (currentSortOrder.equals("price_asc")) updateSort("price_desc");
            else updateSort("price_asc");
        });

        // Product RecyclerView
        productRecyclerView = view.findViewById(R.id.productRecyclerView);
        productRecyclerView.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(getContext(), 2));
        
        productAdapter = new ProductListAdapter(getContext(), products, false);
        productAdapter.setOnProductClickListener(product -> {
            Intent intent = new Intent(getActivity(), ProductDetailsActivity.class);
            intent.putExtra("productId", product.id);
            startActivity(intent);
        });
        productRecyclerView.setAdapter(productAdapter);

        // Category UI
        mainCategorySpinner = view.findViewById(R.id.mainCategorySpinner);
        subcategoryRecyclerView = view.findViewById(R.id.subcategoryRecyclerView);
        subcategoryRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

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

        subcategoryAdapter = new CategoryAdapter(subCategories, subCategoryIcons, subcategory -> {
            selectSubCategory(subcategory);
        });
        subcategoryRecyclerView.setAdapter(subcategoryAdapter);

        loadSellerDetails();
        loadAllProductsForSeller();
        
        return view;
    }

    private void loadSellerDetails() {
        if (sellerId == null) return;
        SupabaseManager.getUserProfile(sellerId, new SupabaseManager.SupabaseCallbackWithProfile() {
            @Override
            public void onResult(boolean success, SupabaseManager.Profile profile, String error) {
                if (success && profile != null) {
                    // Update Shop Name
                    if (profile.getShop_name() != null && !profile.getShop_name().isEmpty()) {
                        shopTitle.setText(profile.getShop_name());
                    }

                    // Use shop_location for the shop profile picture (circular)
                    String profileUrl = profile.getShop_location();

                    if (profileUrl != null && !profileUrl.isEmpty() && profileUrl.startsWith("http")) {
                        if (getContext() != null) {
                            com.bumptech.glide.Glide.with(ShopProductsFragment.this)
                                .load(profileUrl)
                                .placeholder(R.drawable.ic_account)
                                .centerCrop()
                                .into(imgShopLogo);
                        }
                    } else {
                        imgShopLogo.setImageResource(R.drawable.ic_account);
                        imgShopLogo.setBackgroundColor(Color.BLACK);
                        imgShopLogo.setPadding(30, 30, 30, 30);
                        imgShopLogo.setColorFilter(Color.WHITE);
                    }
                }
            }
        });

        // checkFollowStatus(); // TODO: Implement if needed for Supabase
    }

    private void checkFollowStatus() {
        // TODO: Implement if needed for Supabase
    }

    private void updateFollowButtonUI() {
        if (isFollowing) {
            btnFollowShop.setText("Following");
            btnFollowShop.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.GRAY));
        } else {
            btnFollowShop.setText("Follow");
            btnFollowShop.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.colorAccent)));
        }
    }

    private void toggleFollow() {
        // TODO: Implement if needed for Supabase
    }

    private void updateSort(String order) {
        currentSortOrder = order;
        // Update UI colors
        btnSortPopular.setTextColor(order.equals("popular") ? ContextCompat.getColor(requireContext(), R.color.colorAccent) : Color.BLACK);
        btnSortLatest.setTextColor(order.equals("latest") ? ContextCompat.getColor(requireContext(), R.color.colorAccent) : Color.BLACK);
        btnSortPrice.setTextColor(order.startsWith("price") ? ContextCompat.getColor(requireContext(), R.color.colorAccent) : Color.BLACK);
        
        if (order.equals("price_asc")) btnSortPrice.setText("Price ↑");
        else if (order.equals("price_desc")) btnSortPrice.setText("Price ↓");
        else btnSortPrice.setText("Price ⇅");

        applyFiltersAndSort();
    }

    private List<Product> allProducts = new ArrayList<>();

    private void applyFiltersAndSort() {
        List<Product> filtered = new ArrayList<>();
        for (Product p : allProducts) {
            boolean matchesSearch = currentSearchQuery.isEmpty() || 
                                   (p.name != null && p.name.toLowerCase().contains(currentSearchQuery));
            if (matchesSearch) {
                filtered.add(p);
            }
        }

        // Sort
        java.util.Collections.sort(filtered, (p1, p2) -> {
            switch (currentSortOrder) {
                case "price_asc": return Double.compare(p1.price, p2.price);
                case "price_desc": return Double.compare(p2.price, p1.price);
                case "latest": return Long.compare(p2.createdAt, p1.createdAt);
                default: return 0;
            }
        });

        products.clear();
        products.addAll(filtered);
        productAdapter.notifyDataSetChanged();
    }

    private void loadAllProductsForSeller() {
        if (sellerId == null) return;

        SupabaseManager.getProductsForSeller(sellerId, new SupabaseManager.SupabaseCallbackWithProducts() {
            @Override
            public void onResult(boolean success, List<SupabaseManager.ProductModel> productModels, String error) {
                if (success && productModels != null) {
                    allProducts.clear();
                    for (SupabaseManager.ProductModel sp : productModels) {
                        Product p = new Product();
                        p.id = sp.getId();
                        p.name = sp.getName();
                        p.price = sp.getPrice();
                        p.stock = (long) sp.getStock();
                        p.coverPhotoUri = sp.getCover_photo_url();
                        p.sellerId = sellerId;
                        p.description = sp.getDescription();
                        p.mainCategory = sp.getMain_category();
                        p.category = sp.getCategory();
                        p.colors = sp.getColors();
                        p.sizes = sp.getSizes();
                        p.isAvailable = sp.is_available();

                        // Parse createdAt
                        if (sp.getCreated_at() != null) {
                            try {
                                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
                                java.util.Date date = sdf.parse(sp.getCreated_at());
                                if (date != null) p.createdAt = date.getTime();
                            } catch (Exception e) {
                                android.util.Log.e("ShopProductsFragment", "Error parsing date: " + sp.getCreated_at());
                            }
                        }

                        allProducts.add(p);
                    }
                    applyFiltersAndSort();
                } else {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error loading products: " + error, Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    private void setupMainCategories() {
        mainCategories.clear();
        mainCategories.add("Women");
        mainCategories.add("Men");
        mainCategories.add("Kids");
        mainCategories.add("Home");
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
        } else if (mainCategory.equals("Home")) {
            arrayId = R.array.categories_home;
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
                } else if (mainCategory.equals("Women") && subcat.equals("Dresses")) {
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
                } else if (mainCategory.equals("Kids") && (subcat.equals("Dresses"))) {
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
        applyFiltersAndSort();
    }



    @Override
    public void onCategoryClick(String category) {
        selectSubCategory(category);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAllProductsForSeller();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
} 