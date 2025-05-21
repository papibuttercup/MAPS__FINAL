package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import android.widget.ImageButton;

public class SellerDashboardActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_dashboard);

        // Set up the Toolbar
        Toolbar toolbar = findViewById(R.id.sellerToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle("Seller Dashboard");
        }

        // Make sure the toolbar is visible
        toolbar.setVisibility(View.VISIBLE);
        toolbar.bringToFront();

        MaterialCardView cardListItem = findViewById(R.id.cardListItem);
        MaterialCardView cardOrders = findViewById(R.id.cardOrders);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        ImageButton btnSellerChats = findViewById(R.id.btnSellerChats);

        cardListItem.setOnClickListener(v -> {
            startActivity(new Intent(this, ListNewItemActivity.class));
        });
        cardOrders.setOnClickListener(v -> {
            startActivity(new Intent(this, SellerOrdersActivity.class));
        });
        btnSellerChats.setOnClickListener(v -> {
            Intent intent = new Intent(this, SellerChatListActivity.class);
            startActivity(intent);
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