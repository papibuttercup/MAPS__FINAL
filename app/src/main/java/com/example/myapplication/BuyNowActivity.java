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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;

public class BuyNowActivity extends AppCompatActivity {
    private TextView tvProductName, tvProductPrice, tvProductStock;
    private EditText etAddress, etPhone, etName;
    private Button btnCheckout;
    private String productId, sellerId, productName;
    private double productPrice;
    private long productStock;
    private FirebaseFirestore db;
    private String userId;
    private NumberPicker npQuantity;
    private int quantity = 1;
    private Spinner spinnerBarangay;
    private String selectedBarangay = "";
    private EditText etAdditionalDetails;
    private TextView tvCity;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy_now);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        tvProductName = findViewById(R.id.tvProductName);
        tvProductPrice = findViewById(R.id.tvProductPrice);
        tvProductStock = findViewById(R.id.tvProductStock);
        etAddress = findViewById(R.id.etAddress);
        etPhone = findViewById(R.id.etPhone);
        etName = findViewById(R.id.etName);
        btnCheckout = findViewById(R.id.btnCheckout);
        npQuantity = findViewById(R.id.npQuantity);
        npQuantity.setMinValue(1);
        npQuantity.setMaxValue((int) (productStock > 0 ? productStock : 1));
        npQuantity.setValue(getIntent().getIntExtra("quantity", 1));
        npQuantity.setWrapSelectorWheel(false);
        npQuantity.setOnValueChangedListener((picker, oldVal, newVal) -> quantity = newVal);

        spinnerBarangay = findViewById(R.id.spinnerBarangay);
        etAdditionalDetails = findViewById(R.id.etAdditionalDetails);
        tvCity = findViewById(R.id.tvCity);

        // Make name field read-only
        etName.setEnabled(false);
        etName.setFocusable(false);
        etName.setClickable(false);

        // Load user's name from Firestore
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String firstName = documentSnapshot.getString("firstName");
                    String lastName = documentSnapshot.getString("lastName");
                    String fullName = "";
                    if (firstName != null) fullName += firstName;
                    if (lastName != null) fullName += (fullName.isEmpty() ? "" : " ") + lastName;
                    if (!fullName.isEmpty()) {
                        etName.setText(fullName);
                    }
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error loading user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });

        // Set up barangay spinner
        ArrayAdapter<CharSequence> barangayAdapter = ArrayAdapter.createFromResource(
            this, R.array.baguio_barangays, android.R.layout.simple_spinner_item);
        barangayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBarangay.setAdapter(barangayAdapter);
        spinnerBarangay.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedBarangay = parent.getItemAtPosition(position).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedBarangay = "";
            }
        });
        tvCity.setText("Baguio City");

        // Get product info from intent
        productId = getIntent().getStringExtra("productId");
        sellerId = getIntent().getStringExtra("sellerId");
        productName = getIntent().getStringExtra("productName");
        productPrice = getIntent().getDoubleExtra("productPrice", 0);
        productStock = getIntent().getLongExtra("productStock", 0);

        tvProductName.setText(productName);
        tvProductPrice.setText("₱" + productPrice);
        tvProductStock.setText("Stock: " + productStock);

        btnCheckout.setOnClickListener(v -> {
            String address = etAddress.getText().toString().trim();
            String additionalDetails = etAdditionalDetails.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String name = etName.getText().toString().trim();
            if (TextUtils.isEmpty(selectedBarangay) || TextUtils.isEmpty(address) || TextUtils.isEmpty(phone)) {
                Toast.makeText(this, "Please fill all required fields (except additional details)", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this, "Error: Could not load your name. Please try again later.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (productStock < quantity) {
                Toast.makeText(this, "Not enough stock", Toast.LENGTH_SHORT).show();
                return;
            }
            String fullAddress = selectedBarangay + ", Baguio City, " + address;
            if (!TextUtils.isEmpty(additionalDetails)) {
                fullAddress += ", " + additionalDetails;
            }
            placeOrder(fullAddress, phone, name, quantity);
        });
    }

    private void placeOrder(String address, String phone, String name, int quantity) {
        // Reduce stock
        DocumentReference productRef = db.collection("products").document(productId);
        db.runTransaction(transaction -> {
            DocumentReference prodRef = productRef;
            Long currentStock = transaction.get(prodRef).getLong("stock");
            if (currentStock == null || currentStock < quantity) {
                throw new RuntimeException("Not enough stock");
            }
            transaction.update(prodRef, "stock", currentStock - quantity);
            return null;
        }).addOnSuccessListener(aVoid -> {
            // Add order
            List<Map<String, Object>> products = new ArrayList<>();
            Map<String, Object> productMap = new HashMap<>();
            productMap.put("productId", productId);
            productMap.put("productName", productName);
            productMap.put("price", productPrice);
            productMap.put("quantity", quantity);
            products.add(productMap);

            Order order = new Order(userId, sellerId, products, productPrice * quantity, address, name, phone);
            db.collection("orders").add(order)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Order placed!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to place order: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to update stock: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
} 