package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private Button saveButton;
    private ImageButton backButton;
    private ImageView profileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupClickListeners();
        loadUserData();
    }

    private void initializeViews() {
        saveButton = findViewById(R.id.saveButton);
        backButton = findViewById(R.id.backButton);
        profileImage = findViewById(R.id.profileImage);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        setupItem(R.id.itemName, "Name", "", null);
        setupItem(R.id.itemBio, "Bio", "Set Now >", null);
        setupItem(R.id.itemGender, "Gender", "Male >", null);
        setupItem(R.id.itemBirthday, "Birthday", "**/ **/ 2006 >", null);
        setupItem(R.id.itemPhone, "Phone", "**********31 >", null);
        setupItem(R.id.itemEmail, "Email", "h***********2@gmail.com >", null);

        saveButton.setOnClickListener(v -> saveProfile());
    }

    private void setupItem(int viewId, String title, String value, View.OnClickListener listener) {
        View view = findViewById(viewId);
        if (view != null) {
            TextView titleView = view.findViewById(R.id.itemTitle);
            TextView valueView = view.findViewById(R.id.itemValue);
            if (titleView != null) titleView.setText(title);
            if (valueView != null) {
                valueView.setText(value);
                valueView.setTextColor(getResources().getColor(R.color.gray_500));
            }
            if (listener != null) view.setOnClickListener(listener);
        }
    }

    private void loadUserData() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String bio = doc.getString("bio");
                        String gender = doc.getString("gender");
                        String birthday = doc.getString("birthday");
                        String phone = doc.getString("phone");
                        String email = currentUser.getEmail();

                        if (name != null) updateItemValue(R.id.itemName, name);
                        if (bio != null) updateItemValue(R.id.itemBio, bio);
                        if (gender != null) updateItemValue(R.id.itemGender, gender);
                        if (birthday != null) updateItemValue(R.id.itemBirthday, birthday);
                        if (phone != null) updateItemValue(R.id.itemPhone, maskPhone(phone));
                        if (email != null) updateItemValue(R.id.itemEmail, maskEmail(email));
                    }
                });
        }
    }

    private void updateItemValue(int viewId, String value) {
        View view = findViewById(viewId);
        if (view != null) {
            TextView valueView = view.findViewById(R.id.itemValue);
            if (valueView != null) {
                valueView.setText(value);
                valueView.setTextColor(getResources().getColor(R.color.black));
            }
        }
    }

    private void saveProfile() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            // In a real app, we'd get values from input dialogs or new activities
            Map<String, Object> updates = new HashMap<>();
            // For now, just a placeholder for the save logic
            db.collection("users").document(currentUser.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                    finish();
                });
        }
    }

    private String maskPhone(String phone) {
        if (phone.length() > 2) {
            return "**********" + phone.substring(phone.length() - 2);
        }
        return phone;
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf("@");
        if (atIndex > 1) {
            return email.substring(0, 1) + "***********" + email.substring(atIndex - 1);
        }
        return email;
    }
}
