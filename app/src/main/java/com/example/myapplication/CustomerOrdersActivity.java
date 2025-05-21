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

public class CustomerOrdersActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_orders); // Changed from activity_seller_orders

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Orders");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new CustomerOrdersPagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) tab.setText("Pending");
            else if (position == 1) tab.setText("Delivered");
            else if (position == 2) tab.setText("Canceled");
            else tab.setText("Rejected");
        }).attach();
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