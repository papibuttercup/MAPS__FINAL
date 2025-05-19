package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private BottomNavigationView bottomNavigationView;
    private Button category1, category2, category3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Initialize category buttons
        initializeCategoryButtons();

        // Initialize bottom navigation
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.navigation_for_you);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_for_you) {
                // Already on For You tab
                return true;
            } else if (itemId == R.id.navigation_home) {
                finish(); // or navigate to home/main screen
                return true;
            } else if (itemId == R.id.navigation_account) {
                startActivity(new Intent(MapsActivity.this, AccountActivity.class));
                return true;
            }
            return false;
        });
    }

    private void initializeCategoryButtons() {
        category1 = findViewById(R.id.category1);
        category2 = findViewById(R.id.category2);
        category3 = findViewById(R.id.category3);

        // Set up category button click listeners
        category1.setOnClickListener(v -> {
            // Handle category 1 selection
            updateCategorySelection(category1);
        });

        category2.setOnClickListener(v -> {
            // Handle category 2 selection
            updateCategorySelection(category2);
        });

        category3.setOnClickListener(v -> {
            // Handle category 3 selection
            updateCategorySelection(category3);
        });
    }

    private void updateCategorySelection(Button selectedButton) {
        // Reset all buttons
        category1.setSelected(false);
        category2.setSelected(false);
        category3.setSelected(false);

        // Set selected button
        selectedButton.setSelected(true);

        // Update map markers or filters based on selected category
        // Add your category-specific logic here
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Add your map initialization code here
    }
} 