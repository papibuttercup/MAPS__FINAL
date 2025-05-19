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
            startActivity(new Intent(this, ListNewItemActivity.class));
        });
        cardShopLocation.setOnClickListener(v -> {
            startActivity(new Intent(this, ShopLocationActivity.class));
        });
        cardOrders.setOnClickListener(v -> {
            startActivity(new Intent(this, SellerOrdersActivity.class));
        });

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_dashboard) {
                // Already on dashboard
                return true;
            } else if (itemId == R.id.nav_products) {
                startActivity(new Intent(this, SellerMainActivity.class));
                return true;
            } else if (itemId == R.id.nav_orders) {
                startActivity(new Intent(this, SellerOrdersActivity.class));
                return true;
            } else if (itemId == R.id.nav_account) {
                startActivity(new Intent(this, SellerAccountActivity.class));
                return true;
            }
            return false;
        });
    }
} 