package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class BankAccountsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bank_accounts);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        setupOptions();
    }

    private void setupOptions() {
        setupOption(R.id.itemAddCard, "Add New Card", v -> {
            Toast.makeText(this, "Add Card clicked", Toast.LENGTH_SHORT).show();
        });

        setupOption(R.id.itemAddBank, "Add Bank Account", v -> {
            Toast.makeText(this, "Add Bank clicked", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupOption(int viewId, String title, View.OnClickListener listener) {
        View view = findViewById(viewId);
        if (view != null) {
            TextView titleView = view.findViewById(R.id.txtTitle);
            if (titleView != null) titleView.setText(title);
            if (listener != null) view.setOnClickListener(listener);
        }
    }
}
