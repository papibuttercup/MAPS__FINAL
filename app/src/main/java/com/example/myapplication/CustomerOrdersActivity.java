package com.example.myapplication;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import androidx.appcompat.widget.Toolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import android.util.Log;
import android.widget.Toast;

public class CustomerOrdersActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ViewPager2.OnPageChangeCallback pageChangeCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_orders);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Orders");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Check if user is logged in
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in to view orders", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);

        // Setup ViewPager
        CustomerOrdersPagerAdapter pagerAdapter = new CustomerOrdersPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Setup page change callback
        pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                tabLayout.selectTab(tabLayout.getTabAt(position));
            }
        };
        viewPager.registerOnPageChangeCallback(pageChangeCallback);

        // Connect TabLayout with ViewPager
        new TabLayoutMediator(tabLayout, viewPager,
            (tab, position) -> {
                switch (position) {
                    case 0:
                        tab.setText("Pending");
                        break;
                    case 1:
                        tab.setText("Delivered");
                        break;
                    case 2:
                        tab.setText("Canceled");
                        break;
                    case 3:
                        tab.setText("Rejected");
                        break;
                }
            }
        ).attach();

        // Setup tab selection listener
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (viewPager != null && pageChangeCallback != null) {
            viewPager.unregisterOnPageChangeCallback(pageChangeCallback);
        }
    }

    private static class CustomerOrdersPagerAdapter extends FragmentStateAdapter {
        public CustomerOrdersPagerAdapter(@NonNull AppCompatActivity activity) {
            super(activity);
        }
        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) return new CustomerPendingOrdersFragment();
            else if (position == 1) return new CustomerDeliveredOrdersFragment();
            else if (position == 2) return new CustomerCanceledOrdersFragment();
            else return new CustomerRejectedOrdersFragment();
        }
        @Override
        public int getItemCount() { return 4; }
    }
} 