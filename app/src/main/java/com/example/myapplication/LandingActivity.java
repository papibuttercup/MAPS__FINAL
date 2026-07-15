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
                showAccount();
                return true;
            } else if (itemId == R.id.navigation_favorites) {
                showFavorites();
                return true;
            } else if (itemId == R.id.navigation_categories) {
                showCategories();
                return true;
            } else if (itemId == R.id.navigation_home) {
                showHome();
                return true;
            }
            return false;
        });
    }

    public void showOrders() {
        showOrders(0);
    }

    public void showOrders(int tabIndex) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, CustomerOrdersFragment.newInstance(tabIndex))
            .addToBackStack(null)
            .commit();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            if (intent.getBooleanExtra("openShopProducts", false)) {
                String sellerId = intent.getStringExtra("sellerId");
                String shopName = intent.getStringExtra("shopName");
                if (sellerId != null && shopName != null) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, com.example.myapplication.ShopProductsFragment.newInstance(sellerId, shopName))
                            .addToBackStack(null)
                            .commit();
                }
            } else if ("orders".equals(intent.getStringExtra("navigateTo"))) {
                showOrders();
            }
        }
    }

    private void showHome() {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, new HomeFragment())
            .commit();
    }

    private void showAccount() {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, new AccountFragment())
            .commit();
    }

    private void showFavorites() {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, new FavoritesFragment())
            .commit();
    }

    private void showCategories() {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, new CategoriesFragment())
            .commit();
    }
}