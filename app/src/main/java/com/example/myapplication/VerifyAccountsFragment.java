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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class VerifyAccountsFragment extends Fragment {
    private LinearLayout container;
    private FirebaseFirestore db;
    private List<String> pendingSellerIds = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ScrollView scrollView = new ScrollView(getContext());
        this.container = new LinearLayout(getContext());
        this.container.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(this.container);
        db = FirebaseFirestore.getInstance();
        loadPendingSellers();
        return scrollView;
    }

    private void loadPendingSellers() {
        db.collection("sellers")
            .whereEqualTo("verificationStatus", "pending")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                container.removeAllViews();
                pendingSellerIds.clear();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    String sellerId = doc.getId();
                    String shopName = doc.getString("shopName");
                    String email = doc.getString("email");
                    String phone = doc.getString("phone");
                    String name = doc.getString("firstName") + " " + doc.getString("lastName");
                    View card = createSellerCard(sellerId, shopName, name, email, phone);
                    container.addView(card);
                    pendingSellerIds.add(sellerId);
                }
                if (pendingSellerIds.isEmpty()) {
                    TextView empty = new TextView(getContext());
                    empty.setText("No pending seller accounts.");
                    container.addView(empty);
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed to load sellers: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private View createSellerCard(String sellerId, String shopName, String name, String email, String phone) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(32, 32, 32, 32);
        card.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
        TextView info = new TextView(getContext());
        info.setText("Shop: " + shopName + "\nName: " + name + "\nEmail: " + email + "\nPhone: " + phone);
        Button approve = new Button(getContext());
        approve.setText("Approve");
        Button reject = new Button(getContext());
        reject.setText("Reject");
        approve.setOnClickListener(v -> updateSellerStatus(sellerId, "approved"));
        reject.setOnClickListener(v -> updateSellerStatus(sellerId, "rejected"));
        card.addView(info);
        card.addView(approve);
        card.addView(reject);
        return card;
    }

    private void updateSellerStatus(String sellerId, String status) {
        db.collection("sellers").document(sellerId)
            .update("verificationStatus", status)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Seller " + status, Toast.LENGTH_SHORT).show();
                loadPendingSellers();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
} 