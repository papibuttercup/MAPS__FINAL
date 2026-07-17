package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class CategoriesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_categories, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnSearchHeader).setOnClickListener(v -> 
            Toast.makeText(requireContext(), "Search clicked", Toast.LENGTH_SHORT).show());
        
        view.findViewById(R.id.btnCartHeader).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CartActivity.class);
            startActivity(intent);
        });

        setupBanner(view.findViewById(R.id.bannerWoman), "Women");
        setupBanner(view.findViewById(R.id.bannerMen), "Men");
        setupBanner(view.findViewById(R.id.bannerKids), "Kids");
        setupBanner(view.findViewById(R.id.bannerHome), "Home");
    }

    private void setupBanner(View banner, String categoryName) {
        if (banner != null) {
            banner.setOnClickListener(v -> openSubCategory(categoryName));
        }
    }

    private void openSubCategory(String categoryName) {
        SubCategoryFragment fragment = new SubCategoryFragment();
        Bundle args = new Bundle();
        args.putString("categoryName", categoryName);
        fragment.setArguments(args);

        requireActivity().getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit();
    }
}
