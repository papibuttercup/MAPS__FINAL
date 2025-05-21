package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.fragment.app.Fragment;
import androidx.annotation.Nullable;
import android.util.Log;

public class LandingActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        bottomNavigation = findViewById(R.id.bottomNavigation);

        handleIntent(getIntent());

        if (savedInstanceState == null && !getIntent().getBooleanExtra("openShopProducts", false)) {
            showHome();
        }

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_account) {
                startActivity(new Intent(this, AccountActivity.class));
                return true;
            } else if (itemId == R.id.navigation_for_you) {
                showForYou();
                return true;
            } else if (itemId == R.id.navigation_home) {
                showHome();
                return true;
            } else if (itemId == R.id.navigation_orders) {
                startActivity(new Intent(this, CustomerOrdersActivity.class));
                return true;
            }
            return false;
        });
        // Force the label to stay as 'For You'
        bottomNavigation.getMenu().findItem(R.id.navigation_for_you).setTitle("For You");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("openShopProducts", false)) {
            String sellerId = intent.getStringExtra("sellerId");
            String shopName = intent.getStringExtra("shopName");
            if (sellerId != null && shopName != null) {
                getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, com.example.myapplication.ShopProductsFragment.newInstance(sellerId, shopName))
                    .addToBackStack(null)
                    .commit();
            }
        }
    }

    private void showHome() {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, new HomeFragment())
            .commit();
    }

    private void showForYou() {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, new ForYouFragment())
            .commit();
    }
} 