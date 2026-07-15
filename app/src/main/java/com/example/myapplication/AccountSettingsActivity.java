package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class AccountSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        View messageButton = findViewById(R.id.messageContainer);
        if (messageButton != null) {
            messageButton.setOnClickListener(v -> {
                startActivity(new Intent(this, CustomerChatListActivity.class));
            });
        }

        setupItems();
    }

    private void setupItems() {
        // My Account section
        setupItem(R.id.itemAccountSecurity, "Account & Security", v -> {
            startActivity(new Intent(this, AccountSecurityActivity.class));
        });

        setupItem(R.id.itemMyAddresses, "My Addresses", v -> {
            startActivity(new Intent(this, MyAddressesActivity.class));
        });

        setupItem(R.id.itemBankAccounts, "Bank Accounts / Cards", v -> {
            startActivity(new Intent(this, BankAccountsActivity.class));
        });

        // Settings section
        setupItem(R.id.itemChatSettings, "Chat Settings", null);
        setupItem(R.id.itemOrderSettings, "Order Settings", null);
        setupItem(R.id.itemNotificationSettings, "Notification Settings", null);
        setupItem(R.id.itemPrivacySettings, "Privacy Settings", null);
        setupItem(R.id.itemBlockedUsers, "Blocked Users", null);

        findViewById(R.id.itemLanguage).setOnClickListener(v -> {
            startActivity(new Intent(this, SelectLanguageActivity.class));
        });
        
        View languageItem = findViewById(R.id.itemLanguage);
        if (languageItem != null) {
            TextView subtitleView = languageItem.findViewById(R.id.languageSubtitle);
            if (subtitleView != null) {
                subtitleView.setText("English");
            }
        }
    }

    private void setupItem(int viewId, String title, View.OnClickListener listener) {
        View view = findViewById(viewId);
        if (view != null) {
            TextView titleView = view.findViewById(R.id.itemTitle);
            if (titleView != null) {
                titleView.setText(title);
            }
            if (listener != null) {
                view.setOnClickListener(listener);
            }
        }
    }
}
