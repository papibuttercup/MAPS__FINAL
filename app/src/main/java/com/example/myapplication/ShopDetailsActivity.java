package com.example.myapplication;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ShopDetailsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_details);

        String sellerId = getIntent().getStringExtra("sellerId");
        TextView textView = findViewById(R.id.txtShopDetails);
        textView.setText("Shop Details for sellerId: " + sellerId);
        // TODO: Replace with your actual shop details UI and logic
    }
} 