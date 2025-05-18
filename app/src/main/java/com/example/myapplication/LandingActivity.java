package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.fragment.app.Fragment;

public class LandingActivity extends AppCompatActivity {
    private boolean isOnForYou = false;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        bottomNavigation = findViewById(R.id.bottomNavigation);

        if (savedInstanceState == null) {
            showHome();
        }

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_account) {
                startActivity(new Intent(this, AccountActivity.class));
                return true;
            } else if (itemId == R.id.navigation_for_you) {
                if (!isOnForYou) {
                    showForYou();
                } else {
                    showHome();
                }
                return true;
            } else if (itemId == R.id.navigation_location) {
                startActivity(new Intent(this, Maps.class));
                return true;
            }
            return false;
        });
    }

    private void showHome() {
        isOnForYou = false;
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, new HomeFragment())
            .commit();
        // Set nav label to 'For You'
        if (bottomNavigation != null) {
            bottomNavigation.getMenu().findItem(R.id.navigation_for_you).setTitle("For You");
        }
    }

    private void showForYou() {
        isOnForYou = true;
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, new ForYouFragment())
            .commit();
        // Set nav label to 'Home'
        if (bottomNavigation != null) {
            bottomNavigation.getMenu().findItem(R.id.navigation_for_you).setTitle("Home");
        }
    }
} 