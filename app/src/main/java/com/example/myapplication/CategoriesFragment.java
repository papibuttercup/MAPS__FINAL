package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.res.Resources;
import androidx.fragment.app.Fragment;

public class CategoriesFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_categories, container, false);
        
        // Get selectedTab argument
        final String selectedTab = getArguments() != null ? 
            getArguments().getString("selectedTab", "Women") : "Women";

        // Find the LinearLayout for categories
        LinearLayout categoryList = view.findViewById(R.id.categoryList);
        categoryList.removeAllViews();
        int arrayResId;
        if (selectedTab.equals("Men")) {
            arrayResId = R.array.categories_men;
        } else if (selectedTab.equals("Kids")) {
            arrayResId = R.array.categories_kids;
        } else {
            arrayResId = R.array.categories_women;
        }
        String[] categories = getResources().getStringArray(arrayResId);
        for (String category : categories) {
            View card = inflater.inflate(R.layout.item_category_card, categoryList, false);
            TextView label = card.findViewById(R.id.categoryLabel);
            label.setText(category);
            // Set click listener to open CategoryProductsFragment
            card.setOnClickListener(v -> {
                CategoryProductsFragment fragment = new CategoryProductsFragment();
                Bundle args = new Bundle();
                args.putString("mainCategory", selectedTab);
                args.putString("category", category);
                fragment.setArguments(args);
                requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
            });
            categoryList.addView(card);
        }
        
        // Set up back button click listener
        ImageView backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            // Pop the fragment from the back stack
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
        
        return view;
    }
} 