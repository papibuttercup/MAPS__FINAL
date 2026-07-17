package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import io.github.jan.supabase.auth.user.UserInfo;

public class AccountSecurityActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_security);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        setupItems();
        loadUserData();
    }

    private void setupItems() {
        setupDetailItem(R.id.itemMyProfile, "My Profile", "", v -> {
            startActivity(new Intent(this, EditProfileActivity.class));
        });
        setupDetailItem(R.id.itemUsername, "Username", "user", null);
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
        var session = SupabaseManager.getCurrentSession();
        if (session != null) {
            UserInfo user = session.getUser();
            String email = user.getEmail();
            String userId = user.getId();

            if (email != null) updateDetailValue(R.id.itemEmail, maskEmail(email));

            // Fetch profile data from Supabase using helper
            SupabaseManager.getUserProfile(userId, new SupabaseManager.SupabaseCallbackWithProfile() {
                @Override
                public void onResult(boolean success, SupabaseManager.Profile profile, String error) {
                    if (success && profile != null) {
                        String firstName = profile.getFirst_name();
                        String lastName = profile.getLast_name();
                        String fullName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
                        if (!fullName.trim().isEmpty()) updateDetailValue(R.id.itemUsername, fullName.trim());
                    } else if (error != null) {
                        Toast.makeText(AccountSecurityActivity.this, "Error loading profile: " + error, Toast.LENGTH_SHORT).show();
                    }
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

    private String maskEmail(String email) {
        int atIndex = email.indexOf("@");
        if (atIndex > 1) {
            return email.substring(0, 1) + "**********" + email.substring(atIndex - 1);
        }
        return email;
    }
}
