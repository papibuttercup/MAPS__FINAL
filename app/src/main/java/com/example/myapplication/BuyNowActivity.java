package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.app.AlertDialog;
import android.util.Log;
import android.widget.LinearLayout;

public class BuyNowActivity extends AppCompatActivity {
    private static final String TAG = "BuyNowActivity";
    private TextView tvProductName, tvProductPrice, tvStockDisplay;
    private EditText etAddress, etPhone, etName;
    private MaterialButton btnCheckout;
    private String productId, sellerId, productName;
    private double productPrice;
    private String userId;
    private Spinner spinnerBarangay;
    private String selectedBarangay = "";
    private EditText etAdditionalDetails;
    private String selectedColor;
    private String selectedSize;
    private int selectedQuantity = 1;
    private Spinner sizeSpinner;
    private Spinner quantitySpinner;
    private LinearLayout colorOptionsContainer;
    private TextView tvSelectedColorName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy_now);

        userId = SupabaseManager.getCurrentUserId();
        if (userId == null) { finish(); return; }

        initializeViews();
        loadProductData();
        setupListeners();
    }

    private void initializeViews() {
        tvProductName = findViewById(R.id.tvProductName);
        tvProductPrice = findViewById(R.id.tvProductPrice);
        tvStockDisplay = findViewById(R.id.tvStockDisplay);
        etAddress = findViewById(R.id.etAddress);
        etPhone = findViewById(R.id.etPhone);
        etName = findViewById(R.id.etName);
        btnCheckout = findViewById(R.id.placeOrderButton);
        sizeSpinner = findViewById(R.id.sizeSpinner);
        quantitySpinner = findViewById(R.id.quantitySpinner);
        spinnerBarangay = findViewById(R.id.spinnerBarangay);
        etAdditionalDetails = findViewById(R.id.etAdditionalDetails);
        colorOptionsContainer = findViewById(R.id.colorOptionsContainer);
        tvSelectedColorName = findViewById(R.id.tvSelectedColorName);
    }

    private void loadProductData() {
        Intent intent = getIntent();
        productId = intent.getStringExtra("productId");
        sellerId = intent.getStringExtra("sellerId");
        productName = intent.getStringExtra("productName");
        productPrice = intent.getDoubleExtra("productPrice", 0);
        
        tvProductName.setText(productName);
        tvProductPrice.setText(String.format("₱%.2f", productPrice));

        SupabaseManager.getUserProfile(userId, new SupabaseManager.SupabaseCallbackWithProfile() {
            @Override public void onResult(boolean success, SupabaseManager.Profile profile, String error) {
                if (success && profile != null) {
                    etName.setText(profile.getFirst_name() + " " + profile.getLast_name());
                }
            }
        });

        setupBarangaySpinner();
    }

    private void setupBarangaySpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.baguio_barangays, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBarangay.setAdapter(adapter);
        spinnerBarangay.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { selectedBarangay = p.getItemAtPosition(pos).toString(); }
            @Override public void onNothingSelected(AdapterView<?> p) { selectedBarangay = ""; }
        });
    }

    private void setupListeners() {
        btnCheckout.setOnClickListener(v -> processOrder());
    }

    private void processOrder() {
        String address = etAddress.getText().toString().trim() + ", " + selectedBarangay;
        SupabaseManager.Order order = new SupabaseManager.Order(null, userId, sellerId, productPrice * selectedQuantity, "pending", address, etName.getText().toString(), etPhone.getText().toString(), null);
        SupabaseManager.placeOrder(order, new SupabaseManager.SupabaseCallback() {
            @Override public void onResult(boolean success, String error) {
                if (success) { Toast.makeText(BuyNowActivity.this, "Order placed!", Toast.LENGTH_SHORT).show(); finish(); }
                else Toast.makeText(BuyNowActivity.this, "Failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
