package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.widget.HorizontalScrollView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private static final String ARG_SELECTED_TAB = "selected_tab";
    private String selectedTab = "Women";

    public static HomeFragment newInstance(String selectedTab) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SELECTED_TAB, selectedTab);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        if (getArguments() != null) {
            selectedTab = getArguments().getString(ARG_SELECTED_TAB, "Women");
        }

        // Tab buttons
        Button btnAll = view.findViewById(R.id.btnAll);
        Button btnWomen = view.findViewById(R.id.btnWomen);
        Button btnMen = view.findViewById(R.id.btnMen);
        Button btnKids = view.findViewById(R.id.btnKids);

        // Underlines
        View underlineAll = view.findViewById(R.id.underlineAll);
        View underlineWomen = view.findViewById(R.id.underlineWomen);
        View underlineMen = view.findViewById(R.id.underlineMen);
        View underlineKids = view.findViewById(R.id.underlineKids);

        // Highlight selected tab
        underlineAll.setVisibility(selectedTab.equals("All") ? View.VISIBLE : View.GONE);
        underlineWomen.setVisibility(selectedTab.equals("Women") ? View.VISIBLE : View.GONE);
        underlineMen.setVisibility(selectedTab.equals("Men") ? View.VISIBLE : View.GONE);
        underlineKids.setVisibility(selectedTab.equals("Kids") ? View.VISIBLE : View.GONE);

        // Set up tab click listeners
        btnAll.setOnClickListener(v -> reloadWithTab("All"));
        btnWomen.setOnClickListener(v -> reloadWithTab("Women"));
        btnMen.setOnClickListener(v -> reloadWithTab("Men"));
        btnKids.setOnClickListener(v -> reloadWithTab("Kids"));

        // See all categories
        TextView seeAllCategories = view.findViewById(R.id.seeAllCategories);
        seeAllCategories.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new CategoriesFragment())
                .addToBackStack(null)
                .commit();
        });

        // Setup RecyclerView for All tab
        RecyclerView recyclerAllProducts = view.findViewById(R.id.recyclerAllProducts);
        recyclerAllProducts.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Store reference for filterCategoryContent
        this.recyclerAllProducts = recyclerAllProducts;

        // Filter content based on selectedTab
        filterCategoryContent(view, selectedTab);

        return view;
    }

    private RecyclerView recyclerAllProducts;

    private void reloadWithTab(String tab) {
        requireActivity().getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, HomeFragment.newInstance(tab))
            .commit();
    }

    private void filterCategoryContent(View view, String tab) {
        LinearLayout carousel = null;
        View parent = view.findViewById(R.id.categorySection);
        View categorySection = view.findViewById(R.id.categorySection);
        View homeContentSection = view.findViewById(R.id.homeContentSection);
        if (parent != null) {
            ViewGroup root = (ViewGroup) parent.getParent();
            int idx = root.indexOfChild(parent);
            if (idx != -1 && idx + 1 < root.getChildCount()) {
                View maybeCarouselScroll = root.getChildAt(idx + 1);
                if (maybeCarouselScroll instanceof HorizontalScrollView) {
                    HorizontalScrollView hsv = (HorizontalScrollView) maybeCarouselScroll;
                    if (hsv.getChildCount() > 0 && hsv.getChildAt(0) instanceof LinearLayout) {
                        carousel = (LinearLayout) hsv.getChildAt(0);
                    }
                }
            }
        }
        if (recyclerAllProducts == null) {
            recyclerAllProducts = view.findViewById(R.id.recyclerAllProducts);
        }
        if (tab.equals("All")) {
            if (carousel != null) ((View)carousel.getParent()).setVisibility(View.GONE);
            recyclerAllProducts.setVisibility(View.VISIBLE);
            if (categorySection != null) categorySection.setVisibility(View.GONE);
            if (homeContentSection != null) homeContentSection.setVisibility(View.GONE);
            // Sample data
            List<Product> products = new ArrayList<>();
            products.add(new Product(R.drawable.ic_image_placeholder, "Brooklyn T-shirt", "R$ 49,99", Arrays.asList(0xFF000000, 0xFFFFFFFF, 0xFFB87333)));
            products.add(new Product(R.drawable.ic_image_placeholder, "T Shirt Cropped Oversized", "R$ 18,90", Arrays.asList(0xFF000000, 0xFF808080)));
            products.add(new Product(R.drawable.ic_image_placeholder, "Winner Sport T-shirt", "R$ 72,90", Arrays.asList(0xFF000000, 0xFF0000FF, 0xFF008000)));
            products.add(new Product(R.drawable.ic_image_placeholder, "Animated Loose T-shirt", "R$ 51,95", Arrays.asList(0xFF000000, 0xFFFF0000)));
            ProductAdapter adapter = new ProductAdapter(getContext(), products);
            recyclerAllProducts.setAdapter(adapter);
        } else {
            if (carousel != null) ((View)carousel.getParent()).setVisibility(View.VISIBLE);
            recyclerAllProducts.setVisibility(View.GONE);
            if (categorySection != null) categorySection.setVisibility(View.VISIBLE);
            if (homeContentSection != null) homeContentSection.setVisibility(View.VISIBLE);
            carousel.removeAllViews();
            if (tab.equals("Men")) {
                addCategoryCard(carousel, "Shirts");
                addCategoryCard(carousel, "T-Shirts");
                addCategoryCard(carousel, "Pants");
                addCategoryCard(carousel, "Jeans");
                addCategoryCard(carousel, "Shorts");
                addCategoryCard(carousel, "Jackets");
                addCategoryCard(carousel, "Suits");
                addCategoryCard(carousel, "Sweaters");
                addCategoryCard(carousel, "Hoodies");
                addCategoryCard(carousel, "Activewear");
                addCategoryCard(carousel, "Underwear");
                addCategoryCard(carousel, "Sleepwear");
            } else if (tab.equals("Kids")) {
                addCategoryCard(carousel, "T-Shirts");
                addCategoryCard(carousel, "Shirts");
                addCategoryCard(carousel, "Dresses");
                addCategoryCard(carousel, "Pants");
                addCategoryCard(carousel, "Shorts");
                addCategoryCard(carousel, "Skirts");
                addCategoryCard(carousel, "Jackets");
                addCategoryCard(carousel, "Sweaters");
                addCategoryCard(carousel, "Hoodies");
                addCategoryCard(carousel, "Sleepwear");
                addCategoryCard(carousel, "Underwear");
            } else { // Women (default)
                addCategoryCard(carousel, "Dress");
                addCategoryCard(carousel, "Tops");
                addCategoryCard(carousel, "T-Shirts");
                addCategoryCard(carousel, "Shirts");
                addCategoryCard(carousel, "Beach wear");
                addCategoryCard(carousel, "Pants");
                addCategoryCard(carousel, "Jeans");
                addCategoryCard(carousel, "Shorts");
                addCategoryCard(carousel, "Skirts");
                addCategoryCard(carousel, "Outerwear");
                addCategoryCard(carousel, "Jackets");
                addCategoryCard(carousel, "Suits");
                addCategoryCard(carousel, "Sweaters");
                addCategoryCard(carousel, "Hoodies");
                addCategoryCard(carousel, "Activewear");
                addCategoryCard(carousel, "Sleepwear");
                addCategoryCard(carousel, "Underwear");
            }
        }
    }

    private void addCategoryCard(LinearLayout carousel, String categoryName) {
        // Dynamically create a category card (similar to the XML)
        android.content.Context context = carousel.getContext();
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                (int) (90 * context.getResources().getDisplayMetrics().density),
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMarginEnd((int) (12 * context.getResources().getDisplayMetrics().density));
        card.setLayoutParams(cardParams);

        android.widget.ImageView image = new android.widget.ImageView(context);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                (int) (72 * context.getResources().getDisplayMetrics().density),
                (int) (72 * context.getResources().getDisplayMetrics().density));
        image.setLayoutParams(imgParams);
        image.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        image.setImageResource(R.drawable.ic_image_placeholder);
        image.setBackgroundResource(R.drawable.category_card_bg);
        image.setContentDescription(categoryName);
        image.setPadding(0, (int) (4 * context.getResources().getDisplayMetrics().density), 0, (int) (4 * context.getResources().getDisplayMetrics().density));

        android.widget.TextView label = new android.widget.TextView(context);
        label.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        label.setText(categoryName);
        label.setTextSize(13);
        label.setTextColor(context.getResources().getColor(R.color.colorPrimary));

        card.addView(image);
        card.addView(label);
        carousel.addView(card);
    }
} 