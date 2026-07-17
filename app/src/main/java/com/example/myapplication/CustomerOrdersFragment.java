package com.example.myapplication;

import android.content.Intent;
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

public class CustomerOrdersFragment extends Fragment {
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private int startTab = 0;

    public static CustomerOrdersFragment newInstance(int tabIndex) {
        CustomerOrdersFragment fragment = new CustomerOrdersFragment();
        Bundle args = new Bundle();
        args.putInt("startTab", tabIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_customer_orders, container, false);

        if (getArguments() != null) {
            startTab = getArguments().getInt("startTab", 0);
        }

        // Check if user is logged in
        if (SupabaseManager.getCurrentUserId() == null) {
            Toast.makeText(getContext(), "Please log in to view orders", Toast.LENGTH_SHORT).show();
            return view;
        }

        // Initialize views
        viewPager = view.findViewById(R.id.viewPager);
        tabLayout = view.findViewById(R.id.tabLayout);
        View btnBack = view.findViewById(R.id.btnBack);
        View btnSearch = view.findViewById(R.id.btnSearch);
        View btnMessages = view.findViewById(R.id.btnMessages);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                } else if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
        }

        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), Maps.class);
                startActivity(intent);
            });
        }

        if (btnMessages != null) {
            btnMessages.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), CustomerChatListActivity.class);
                startActivity(intent);
            });
        }

        // Setup ViewPager
        CustomerOrdersPagerAdapter pagerAdapter = new CustomerOrdersPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Connect TabLayout with ViewPager
        new TabLayoutMediator(tabLayout, viewPager,
            (tab, position) -> {
                switch (position) {
                    case 0: tab.setText("All"); break;
                    case 1: tab.setText("To Pay"); break;
                    case 2: tab.setText("To Ship"); break;
                    case 3: tab.setText("To Receive"); break;
                    case 4: tab.setText("Completed"); break;
                    case 5: tab.setText("Returns"); break;
                    case 6: tab.setText("Cancelled"); break;
                }
            }
        ).attach();

        viewPager.setCurrentItem(startTab, false);

        return view;
    }

    private static class CustomerOrdersPagerAdapter extends FragmentStateAdapter {
        public CustomerOrdersPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }
        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0: return CustomerOrderListFragment.newInstance("all");
                case 1: return CustomerOrderListFragment.newInstance("unpaid");
                case 2: return CustomerOrderListFragment.newInstance("pending");
                case 3: return CustomerOrderListFragment.newInstance("accepted");
                case 4: return CustomerOrderListFragment.newInstance("completed");
                case 5: return CustomerOrderListFragment.newInstance("returned");
                case 6: return CustomerOrderListFragment.newInstance("canceled");
                default: return CustomerOrderListFragment.newInstance("all");
            }
        }
        @Override
        public int getItemCount() { return 7; }
    }
}
