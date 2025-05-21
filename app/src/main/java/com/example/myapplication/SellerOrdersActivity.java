package com.example.myapplication;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class SellerOrdersActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_orders);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new SellerOrdersPagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) tab.setText("Pending");
            else if (position == 1) tab.setText("Delivering");
            else if (position == 2) tab.setText("Finished");
            else tab.setText("Canceled");
        }).attach();
    }

    private static class SellerOrdersPagerAdapter extends FragmentStateAdapter {
        public SellerOrdersPagerAdapter(@NonNull AppCompatActivity activity) {
            super(activity);
        }
        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) return new PendingOrdersFragment();
            else if (position == 1) return new DeliveringOrdersFragment();
            else if (position == 2) return new FinishedOrdersFragment();
            else return new CanceledOrdersFragment();
        }
        @Override
        public int getItemCount() { return 4; }
    }
} 