package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class OrderSuccessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_success);

        MaterialButton btnMyOrders = findViewById(R.id.btnMyOrders);
        MaterialButton btnContinueShopping = findViewById(R.id.btnContinueShopping);

        btnMyOrders.setOnClickListener(v -> {
            Intent intent = new Intent(this, LandingActivity.class);
            intent.putExtra("navigateTo", "orders");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        btnContinueShopping.setOnClickListener(v -> {
            Intent intent = new Intent(this, LandingActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}