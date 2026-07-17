package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsersFragment extends Fragment {
    private LinearLayout container, cardsContainer;
    private TextView countView;
    private Spinner filterSpinner;
    private List<SupabaseManager.Profile> profiles = new ArrayList<>();

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle saved) {
        ScrollView scroll = new ScrollView(requireContext());
        container = new LinearLayout(requireContext()); container.setOrientation(LinearLayout.VERTICAL); scroll.addView(container);
        filterSpinner = new Spinner(requireContext()); container.addView(filterSpinner);
        countView = new TextView(requireContext()); container.addView(countView);
        cardsContainer = new LinearLayout(requireContext()); cardsContainer.setOrientation(LinearLayout.VERTICAL); container.addView(cardsContainer);
        loadAccounts();
        return scroll;
    }

    private void loadAccounts() {
        SupabaseManager.getAllProfiles(new SupabaseManager.SupabaseCallbackWithProfiles() {
            @Override public void onResult(boolean success, List<SupabaseManager.Profile> list, String error) {
                if (success && list != null) {
                    profiles = list; displayProfiles();
                }
            }
        });
    }

    private void displayProfiles() {
        cardsContainer.removeAllViews();
        for (SupabaseManager.Profile p : profiles) {
            LinearLayout card = new LinearLayout(requireContext()); card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(30,30,30,30); card.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
            TextView info = new TextView(requireContext()); info.setText(p.getEmail() + " (" + p.getAccount_type() + ")");
            Button hold = new Button(requireContext()); hold.setText("Toggle Hold");
            hold.setOnClickListener(v -> {
                Map<String, Object> updates = new HashMap<>(); updates.put("account_type", "held");
                SupabaseManager.updateProfile(p.getId(), updates, new SupabaseManager.SupabaseCallback() {
                    @Override public void onResult(boolean s, String e) { if (s) loadAccounts(); }
                });
            });
            card.addView(info); card.addView(hold); cardsContainer.addView(card);
        }
    }
}
