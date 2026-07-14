package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class SellerOrdersFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_seller_orders, container, false);

        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        ViewPager2 viewPager = view.findViewById(R.id.viewPager);
        viewPager.setAdapter(new SellerOrdersPagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) tab.setText("Pending");
            else if (position == 1) tab.setText("Delivering");
            else if (position == 2) tab.setText("Finished");
            else tab.setText("Canceled");
        }).attach();

        return view;
    }

    private static class SellerOrdersPagerAdapter extends FragmentStateAdapter {
        public SellerOrdersPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
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