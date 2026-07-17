package com.example.myapplication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VerifyAccountsFragment extends Fragment {
    private LinearLayout container;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle saved) {
        ScrollView scroll = new ScrollView(requireContext());
        container = new LinearLayout(requireContext()); container.setOrientation(LinearLayout.VERTICAL); scroll.addView(container);
        loadPendingSellers();
        return scroll;
    }

    private void loadPendingSellers() {
        SupabaseManager.getAllProfiles(new SupabaseManager.SupabaseCallbackWithProfiles() {
            @Override public void onResult(boolean success, List<SupabaseManager.Profile> list, String error) {
                if (success && list != null) {
                    container.removeAllViews();
                    for (SupabaseManager.Profile p : list) {
                        if ("pending_seller".equals(p.getAccount_type())) container.addView(createSellerCard(p));
                    }
                }
            }
        });
    }

    private View createSellerCard(SupabaseManager.Profile p) {
        LinearLayout card = new LinearLayout(requireContext()); card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(30,30,30,30); card.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
        TextView info = new TextView(requireContext()); info.setText(p.getEmail() + "\nShop: " + p.getShop_name());
        Button approve = new Button(requireContext()); approve.setText("Approve");
        approve.setOnClickListener(v -> updateStatus(p.getId(), "seller"));
        card.addView(info); card.addView(approve); return card;
    }

    private void updateStatus(String id, String type) {
        Map<String, Object> u = new HashMap<>(); u.put("account_type", type);
        SupabaseManager.updateProfile(id, u, new SupabaseManager.SupabaseCallback() {
            @Override public void onResult(boolean s, String e) { if (s) loadPendingSellers(); }
        });
    }
}
