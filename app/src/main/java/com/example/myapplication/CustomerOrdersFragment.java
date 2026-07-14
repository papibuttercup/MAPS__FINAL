package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class CustomerOrdersFragment extends Fragment {
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_orders, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Check if user is logged in
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Please log in to view orders", Toast.LENGTH_SHORT).show();
            return view;
        }

        // Initialize views
        viewPager = view.findViewById(R.id.viewPager);
        tabLayout = view.findViewById(R.id.tabLayout);

        // Setup ViewPager
        CustomerOrdersPagerAdapter pagerAdapter = new CustomerOrdersPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Connect TabLayout with ViewPager
        new TabLayoutMediator(tabLayout, viewPager,
            (tab, position) -> {
                switch (position) {
                    case 0: tab.setText("Pending"); break;
                    case 1: tab.setText("Delivered"); break;
                    case 2: tab.setText("Canceled"); break;
                    case 3: tab.setText("Rejected"); break;
                }
            }
        ).attach();

        return view;
    }

    private static class CustomerOrdersPagerAdapter extends FragmentStateAdapter {
        public CustomerOrdersPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
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