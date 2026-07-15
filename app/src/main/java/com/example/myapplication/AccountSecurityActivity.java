package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AccountSecurityActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_security);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        setupItems();
        loadUserData();
    }

    private void setupItems() {
        setupDetailItem(R.id.itemMyProfile, "My Profile", "", v -> {
            startActivity(new Intent(this, EditProfileActivity.class));
        });
        setupDetailItem(R.id.itemUsername, "Username", "akishiki", null);
        setupDetailItem(R.id.itemPhone, "Phone", "*****31", null);
        setupDetailItem(R.id.itemEmail, "Email", "h***********2@gmail.com", null);
        setupDetailItem(R.id.itemSocialMedia, "Social Media Accounts", "", null);
        setupDetailItem(R.id.itemChangePassword, "Change Password", "", null);

        setupSwitchItem(R.id.itemFingerprint, "Fingerprint Authentication", 
            "Your Fingerprint data is on your device and Thrifty does not store it");
        setupSwitchItem(R.id.itemQuickLogin, "Quick Login", 
            "Allow quick login on this device: samsung");

        setupDetailItem(R.id.itemCheckAccountActivity, "Check Account Activity", 
            "Check your login and account changes in the last 30 days", null);
        setupDetailItem(R.id.itemManageLoginDevice, "Manage Login Device", 
            "Review the devices that you have logged in Thrifty account.", null);
    }

    private void setupDetailItem(int viewId, String title, String value, View.OnClickListener listener) {
        View view = findViewById(viewId);
        if (view != null) {
            TextView titleView = view.findViewById(R.id.itemTitle);
            TextView valueView = view.findViewById(R.id.itemValue);
            if (titleView != null) titleView.setText(title);
            if (valueView != null) valueView.setText(value);
            if (listener != null) view.setOnClickListener(listener);
        }
    }

    private void setupSwitchItem(int viewId, String title, String subtitle) {
        View view = findViewById(viewId);
        if (view != null) {
            TextView titleView = view.findViewById(R.id.itemTitle);
            TextView subtitleView = view.findViewById(R.id.itemSubtitle);
            if (titleView != null) titleView.setText(title);
            if (subtitleView != null) subtitleView.setText(subtitle);
        }
    }

    private void loadUserData() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String username = doc.getString("username");
                        String phone = doc.getString("phone");
                        String email = currentUser.getEmail();

                        if (username != null) updateDetailValue(R.id.itemUsername, username);
                        if (phone != null) updateDetailValue(R.id.itemPhone, maskPhone(phone));
                        if (email != null) updateDetailValue(R.id.itemEmail, maskEmail(email));
                    }
                });
        }
    }

    private void updateDetailValue(int viewId, String value) {
        View view = findViewById(viewId);
        if (view != null) {
            TextView valueView = view.findViewById(R.id.itemValue);
            if (valueView != null) valueView.setText(value);
        }
    }

    private String maskPhone(String phone) {
        if (phone.length() > 2) {
            return "*******" + phone.substring(phone.length() - 2);
        }
        return phone;
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf("@");
        if (atIndex > 1) {
            return email.substring(0, 1) + "**********" + email.substring(atIndex - 1);
        }
        return email;
    }
}
