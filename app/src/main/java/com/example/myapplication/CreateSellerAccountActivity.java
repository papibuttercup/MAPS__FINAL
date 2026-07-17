package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import android.widget.ArrayAdapter;
import io.github.jan.supabase.auth.AuthKt;
import io.github.jan.supabase.auth.providers.builtin.Email;
import kotlinx.serialization.json.JsonObjectBuilder;
import kotlinx.serialization.json.JsonElementKt;
import kotlinx.serialization.json.JsonObject;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class CreateSellerAccountActivity extends AppCompatActivity {
    private EditText firstNameEditText, lastNameEditText, shopNameEditText, /*shopLocationEditText,*/
            emailEditText, phoneEditText, passwordEditText, confirmPasswordEditText;
    private CheckBox termsCheckbox;
    private Button createAccountButton;
    private Spinner spinnerShopBarangay;
    private String selectedBarangay = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_seller_account);

        // Initialize views
        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        firstNameEditText = findViewById(R.id.editTextFirstName);
        lastNameEditText = findViewById(R.id.editTextLastName);
        shopNameEditText = findViewById(R.id.editTextShopName);
        emailEditText = findViewById(R.id.editTextEmail);
        phoneEditText = findViewById(R.id.editTextPhone);
        passwordEditText = findViewById(R.id.editTextPassword);
        confirmPasswordEditText = findViewById(R.id.editTextConfirmPassword);
        termsCheckbox = findViewById(R.id.checkBoxTerms);
        createAccountButton = findViewById(R.id.buttonCreateAccount);
        spinnerShopBarangay = findViewById(R.id.spinnerShopBarangay);

        // Set up barangay spinner
        ArrayAdapter<CharSequence> barangayAdapter = ArrayAdapter.createFromResource(
            this, R.array.baguio_barangays, android.R.layout.simple_spinner_item);
        barangayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerShopBarangay.setAdapter(barangayAdapter);
        spinnerShopBarangay.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedBarangay = parent.getItemAtPosition(position).toString();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                selectedBarangay = "";
            }
        });
    }

    private void setupClickListeners() {
        createAccountButton.setOnClickListener(v -> {
            if (validateForm()) {
                createSellerAccount();
            }
        });
    }

    private boolean validateForm() {
        boolean isValid = true;

        if (TextUtils.isEmpty(firstNameEditText.getText())) {
            firstNameEditText.setError("First name is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(lastNameEditText.getText())) {
            lastNameEditText.setError("Last name is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(shopNameEditText.getText())) {
            shopNameEditText.setError("Shop name is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(selectedBarangay)) {
            Toast.makeText(this, "Please select a shop barangay", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (TextUtils.isEmpty(emailEditText.getText())) {
            emailEditText.setError("Email is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(phoneEditText.getText())) {
            phoneEditText.setError("Phone number is required");
            isValid = false;
        }

        if (TextUtils.isEmpty(passwordEditText.getText())) {
            passwordEditText.setError("Password is required");
            isValid = false;
        } else if (passwordEditText.getText().length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            isValid = false;
        }

        if (!passwordEditText.getText().toString().equals(confirmPasswordEditText.getText().toString())) {
            confirmPasswordEditText.setError("Passwords don't match");
            isValid = false;
        }

        if (!termsCheckbox.isChecked()) {
            Toast.makeText(this, "Please agree to the terms", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private void createSellerAccount() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String shopName = shopNameEditText.getText().toString().trim();
        String shopLocation = selectedBarangay;

        java.util.Map<String, String> metadata = new java.util.HashMap<>();
        metadata.put("first_name", firstName);
        metadata.put("last_name", lastName);
        metadata.put("shop_name", shopName);
        metadata.put("shop_location", shopLocation);
        metadata.put("account_type", "seller");

        SupabaseManager.signUp(email, password, metadata, new SupabaseManager.SupabaseCallback() {
            @Override
            public void onResult(boolean success, String error) {
                if (success) {
                    Toast.makeText(CreateSellerAccountActivity.this,
                            "Seller account created successfully! Please check your email for verification.",
                            Toast.LENGTH_LONG).show();
                    
                    // Return to login screen
                    startActivity(new Intent(CreateSellerAccountActivity.this, LoginActivity.class));
                    finish();
                } else {
                    Toast.makeText(CreateSellerAccountActivity.this,
                            "Account creation failed: " + error,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}