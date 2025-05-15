package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

public class SellerDashboardActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_dashboard);

        MaterialCardView cardListItem = findViewById(R.id.cardListItem);
        MaterialCardView cardShopLocation = findViewById(R.id.cardShopLocation);
        MaterialCardView cardOrders = findViewById(R.id.cardOrders);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        cardListItem.setOnClickListener(v -> {
            // TODO: Navigate to product posting screen
            Toast.makeText(this, "Navigate to List an Item", Toast.LENGTH_SHORT).show();
        });
        cardShopLocation.setOnClickListener(v -> {
            // TODO: Navigate to map/location picker screen
            Toast.makeText(this, "Navigate to Shop Location Picker", Toast.LENGTH_SHORT).show();
        });
        cardOrders.setOnClickListener(v -> {
            // TODO: Navigate to orders list screen
            Toast.makeText(this, "Navigate to Orders", Toast.LENGTH_SHORT).show();
        });

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_pending_orders) {
                // TODO: Navigate to pending orders screen
                Toast.makeText(this, "Pending Orders", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_location) {
                // TODO: Navigate to location/map screen
                Toast.makeText(this, "Location", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_account) {
                // TODO: Navigate to seller account/profile screen
                Toast.makeText(this, "Account", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }
} 