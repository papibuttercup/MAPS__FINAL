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

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import android.content.Intent;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.Arrays;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;

public class ForYouFragment extends Fragment {
    private RecyclerView recyclerView;
    private ProductListAdapter adapter;
    private List<Product> products;
    private FirebaseFirestore db;
    private Spinner mainCategorySpinner;
    private RecyclerView subcategoryRecyclerView;
    private CategoryAdapter subcategoryAdapter;
    private List<String> mainCategories = new ArrayList<>();
    private List<Integer> mainCategoryIcons = new ArrayList<>();
    private List<String> subCategories = new ArrayList<>();
    private List<Integer> subCategoryIcons = new ArrayList<>();
    private String selectedMainCategory = "Women";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_for_you, container, false);
        
        recyclerView = view.findViewById(R.id.recyclerProducts);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        
        products = new ArrayList<>();
        adapter = new ProductListAdapter(getContext(), products, false);
        recyclerView.setAdapter(adapter);
        
        // Set product click listener
        adapter.setOnProductClickListener(product -> {
            Intent intent = new Intent(getContext(), ProductDetailsActivity.class);
            intent.putExtra("productId", product.id);
            startActivity(intent);
        });
        
        db = FirebaseFirestore.getInstance();
        loadAllProducts();
        
        mainCategorySpinner = view.findViewById(R.id.mainCategorySpinner);
        subcategoryRecyclerView = view.findViewById(R.id.subcategoryRecyclerView);
        subcategoryRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        // Setup main categories
        mainCategories = Arrays.asList("Women", "Men", "Kids");
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, mainCategories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mainCategorySpinner.setAdapter(spinnerAdapter);
        mainCategorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMainCategory = mainCategories.get(position);
                loadSubCategories(selectedMainCategory);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Setup subcategory adapter
        subcategoryAdapter = new CategoryAdapter(subCategories, subCategoryIcons, subcategory -> {
            loadProductsForCategory(selectedMainCategory, subcategory);
        });
        subcategoryRecyclerView.setAdapter(subcategoryAdapter);

        // Load initial subcategories
        loadSubCategories(selectedMainCategory);
        
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
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

    private void loadSubCategories(String mainCategory) {
        subCategories.clear();
        subCategoryIcons.clear();
        int arrayId = 0;
        if (mainCategory.equals("Women")) {
            arrayId = R.array.categories_women;
        } else if (mainCategory.equals("Men")) {
            arrayId = R.array.categories_men;
        } else if (mainCategory.equals("Kids")) {
            arrayId = R.array.categories_kids;
        }
        if (arrayId != 0) {
            String[] subcats = getResources().getStringArray(arrayId);
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
    }

    private void loadProductsForCategory(String mainCategory, String subCategory) {
        db.collection("products")
            .whereEqualTo("mainCategory", mainCategory)
            .whereEqualTo("category", subCategory)
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