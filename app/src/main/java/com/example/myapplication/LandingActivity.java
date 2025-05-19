package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.fragment.app.Fragment;

public class LandingActivity extends AppCompatActivity {
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
                    showForYou();
                return true;
            } else if (itemId == R.id.navigation_home) {
                showHome();
                return true;
            }
            return false;
        });
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