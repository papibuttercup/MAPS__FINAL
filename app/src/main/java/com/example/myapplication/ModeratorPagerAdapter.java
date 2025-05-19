package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ModeratorPagerAdapter extends FragmentStateAdapter {
    public ModeratorPagerAdapter(@NonNull FragmentActivity fa) {
        super(fa);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new UsersFragment(); // Replace with your actual users management fragment
        } else {
            return new VerifyAccountsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
} 