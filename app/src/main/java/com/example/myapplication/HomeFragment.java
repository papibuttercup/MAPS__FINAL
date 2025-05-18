package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.widget.HorizontalScrollView;
import androidx.recyclerview.widget.RecyclerView;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // See all categories
        TextView seeAllCategories = view.findViewById(R.id.seeAllCategories);
        seeAllCategories.setOnClickListener(v -> {
            CategoriesFragment fragment = new CategoriesFragment();
            requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
        });

        // Setup categories carousel
        LinearLayout carousel = view.findViewById(R.id.categoriesCarousel);
        if (carousel != null) {
            String[] categories = getResources().getStringArray(R.array.categories_women);
            for (String category : categories) {
                addCategoryCard(carousel, "Women", category);
            }
        }

        return view;
    }

    private void addCategoryCard(LinearLayout carousel, String mainCategory, String categoryName) {
        if (carousel == null || getContext() == null) return;
        
        try {
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
            card.setOnClickListener(v -> {
                // Navigate to For You page with the selected category
                ForYouFragment fragment = new ForYouFragment();
                Bundle args = new Bundle();
                args.putString("category", categoryName);
                args.putString("mainCategory", mainCategory);
                fragment.setArguments(args);
                requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
            });
            carousel.addView(card);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 