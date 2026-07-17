package com.example.myapplication;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import java.util.HashMap;
import java.util.Map;

public class AddAddressActivity extends AppCompatActivity {
    private EditText editFullName, editPhoneNumber, editStreet, editPostalCode;
    private TextView btnLabelWork, btnLabelHome;
    private SwitchCompat switchDefault;
    private Button btnSubmit;
    private String selectedLabel = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_address);

        initializeViews();
        setupClickListeners();
        setupTextWatchers();
    }

    private void initializeViews() {
        editFullName = findViewById(R.id.editFullName);
        editPhoneNumber = findViewById(R.id.editPhoneNumber);
        editStreet = findViewById(R.id.editStreet);
        editPostalCode = findViewById(R.id.editPostalCode);
        btnLabelWork = findViewById(R.id.btnLabelWork);
        btnLabelHome = findViewById(R.id.btnLabelHome);
        switchDefault = findViewById(R.id.switchDefault);
        btnSubmit = findViewById(R.id.btnSubmit);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        btnLabelWork.setOnClickListener(v -> selectLabel("Work"));
        btnLabelHome.setOnClickListener(v -> selectLabel("Home"));
        btnSubmit.setOnClickListener(v -> submitAddress());
    }

    private void selectLabel(String label) {
        selectedLabel = label;
        btnLabelWork.setSelected(label.equals("Work"));
        btnLabelHome.setSelected(label.equals("Home"));
        validateForm();
    }

    private void setupTextWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateForm();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };

        editFullName.addTextChangedListener(watcher);
        editPhoneNumber.addTextChangedListener(watcher);
        editStreet.addTextChangedListener(watcher);
        editPostalCode.addTextChangedListener(watcher);
    }

    private void validateForm() {
        boolean isValid = !editFullName.getText().toString().trim().isEmpty() &&
                !editPhoneNumber.getText().toString().trim().isEmpty() &&
                !editStreet.getText().toString().trim().isEmpty() &&
                !editPostalCode.getText().toString().trim().isEmpty();

        btnSubmit.setEnabled(isValid);
        if (isValid) {
            btnSubmit.setBackgroundTintList(getResources().getColorStateList(R.color.colorAccent));
            btnSubmit.setTextColor(getResources().getColor(R.color.white));
        } else {
            btnSubmit.setBackgroundTintList(getResources().getColorStateList(R.color.gray_200));
            btnSubmit.setTextColor(getResources().getColor(R.color.gray_500));
        }
    }

    private void submitAddress() {
        String userId = SupabaseManager.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "Please sign in to add address", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> address = new HashMap<>();
        address.put("user_id", userId);
        address.put("full_name", editFullName.getText().toString().trim());
        address.put("phone_number", editPhoneNumber.getText().toString().trim());
        address.put("street", editStreet.getText().toString().trim());
        address.put("postal_code", editPostalCode.getText().toString().trim());
        address.put("label", selectedLabel);
        address.put("is_default", switchDefault.isChecked());

        // Assuming a general updateProfile or similar for addresses, 
        // or a new method in SupabaseManager. For now, I'll use updateProfile logic or similar.
        SupabaseManager.updateProfile(userId, address, new SupabaseManager.SupabaseCallback() {
            @Override
            public void onResult(boolean success, String error) {
                if (success) {
                    Toast.makeText(AddAddressActivity.this, "Address added successfully", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(AddAddressActivity.this, "Error adding address: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
