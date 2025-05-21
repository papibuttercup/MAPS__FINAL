package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.app.AlertDialog;
import android.util.Log;
import android.content.res.ColorStateList;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ImageView;
import java.util.HashSet;
import java.util.Set;
import com.google.android.material.card.MaterialCardView;
import android.widget.LinearLayout;
import com.google.firebase.firestore.FieldValue;

public class BuyNowActivity extends AppCompatActivity {
    private static final String TAG = "BuyNowActivity";
    private TextView tvProductName, tvProductPrice, tvStockDisplay;
    private EditText etAddress, etPhone, etName;
    private MaterialButton btnCheckout;
    private String productId, sellerId, productName;
    private double productPrice;
    private long productStock;
    private FirebaseFirestore db;
    private String userId;
    private Spinner spinnerBarangay;
    private String selectedBarangay = "";
    private EditText etAdditionalDetails;
    private List<String> productColors;
    private List<Map<String, Object>> stockEntries;
    private String selectedColor;
    private String selectedSize;
    private int selectedQuantity = 0;
    private Spinner sizeSpinner;
    private Spinner quantitySpinner;
    private AlertDialog dialog;
    private int quantity;
    private LinearLayout colorOptionsContainer;
    private TextView tvSelectedColorName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy_now);

        try {
            initializeViews();
            initializeFirebase();
            loadProductData();
            setupListeners();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage());
            Toast.makeText(this, "Error initializing the app. Please try again.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        try {
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

            if (tvProductName == null || tvProductPrice == null || tvStockDisplay == null ||
                etAddress == null || etPhone == null || etName == null || btnCheckout == null ||
                sizeSpinner == null || quantitySpinner == null || spinnerBarangay == null ||
                colorOptionsContainer == null || tvSelectedColorName == null) {
                throw new IllegalStateException("Required views not found");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage());
            throw e;
        }
    }

    private void initializeFirebase() {
        try {
            db = FirebaseFirestore.getInstance();
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                throw new IllegalStateException("User not logged in");
            }
            userId = currentUser.getUid();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase: " + e.getMessage());
            throw e;
        }
    }

    private void loadProductData() {
        try {
            Intent intent = getIntent();
            if (intent == null) {
                throw new IllegalStateException("No intent data");
            }

            productId = intent.getStringExtra("productId");
            sellerId = intent.getStringExtra("sellerId");
            productName = intent.getStringExtra("productName");
            productPrice = intent.getDoubleExtra("productPrice", 0);
            productStock = intent.getLongExtra("productStock", 0);

            if (productId == null || sellerId == null || productName == null) {
                throw new IllegalStateException("Missing required product data");
            }

            // Load product colors and stock entries
            loadProductDetails();

        } catch (Exception e) {
            Log.e(TAG, "Error loading product data: " + e.getMessage());
            throw e;
        }
    }

    private void loadProductDetails() {
        if (productId == null) return;

        Log.d(TAG, "Loading product details for productId: " + productId);
        
        db.collection("products").document(productId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                try {
                    if (!documentSnapshot.exists()) {
                        Log.e(TAG, "Product document does not exist");
                        Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    List<Map<String, Object>> entries = (List<Map<String, Object>>) documentSnapshot.get("stockEntries");
                    stockEntries = entries != null ? entries : new ArrayList<>();
                    Log.d(TAG, "Loaded stockEntries: " + stockEntries.toString());

                    // Extract colors from stock entries
                    Set<String> uniqueColors = new HashSet<>();
                    for (Map<String, Object> entry : stockEntries) {
                        String color = (String) entry.get("color");
                        if (color != null) {
                            uniqueColors.add(color);
                            Log.d(TAG, "Found color in stock entry: " + color);
                        }
                    }
                    productColors = new ArrayList<>(uniqueColors);

                    Log.d(TAG, "Final productColors list: " + productColors.toString());

                    // Update UI
                    updateProductUI();

                    // Display available colors directly
                    displayAvailableColors();

                } catch (Exception e) {
                    Log.e(TAG, "Error processing product details: " + e.getMessage(), e);
                    Toast.makeText(this, "Error loading product details", Toast.LENGTH_SHORT).show();
                    finish();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading product details: " + e.getMessage(), e);
                Toast.makeText(this, "Error loading product details", Toast.LENGTH_SHORT).show();
                finish();
            });
    }

    private void updateProductUI() {
        try {
            tvProductName.setText(productName);
            tvProductPrice.setText(String.format("₱%.2f", productPrice));
            
            // Load user's name
            loadUserName();
            
            // Setup barangay spinner
            setupBarangaySpinner();
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI: " + e.getMessage());
            Toast.makeText(this, "Error updating display", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUserName() {
        if (userId == null) return;

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                try {
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
                } catch (Exception e) {
                    Log.e(TAG, "Error loading user name: " + e.getMessage());
                }
            })
            .addOnFailureListener(e -> 
                Log.e(TAG, "Error loading user data: " + e.getMessage()));
    }

    private void setupBarangaySpinner() {
        try {
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
        } catch (Exception e) {
            Log.e(TAG, "Error setting up barangay spinner: " + e.getMessage());
        }
    }

    private void setupListeners() {
        try {
            // Size spinner listener
            sizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        selectedSize = (String) parent.getItemAtPosition(position);
                        updateStockForSelectedColor();
                    } catch (Exception e) {
                        Log.e(TAG, "Error in size selection: " + e.getMessage());
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            // Quantity spinner listener
            quantitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        selectedQuantity = Integer.parseInt((String) parent.getItemAtPosition(position));
                        updateTotalPrice();
                    } catch (Exception e) {
                        Log.e(TAG, "Error in quantity selection: " + e.getMessage());
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            // Checkout button listener
            btnCheckout.setOnClickListener(v -> {
                try {
                    if (validateOrder()) {
                        processOrder();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in checkout: " + e.getMessage());
                    Toast.makeText(this, "Error processing order", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error setting up listeners: " + e.getMessage());
            throw e;
        }
    }

    private int calculateColorStock(String color) {
        int totalStock = 0;
        if (stockEntries != null) {
            for (Map<String, Object> entry : stockEntries) {
                String entryColor = (String) entry.get("color");
                if (color.equals(entryColor)) {
                    Long stock = (Long) entry.get("stock");
                    if (stock != null) {
                        totalStock += stock.intValue();
                    }
                }
            }
        }
        Log.d(TAG, "Calculated stock for color " + color + ": " + totalStock);
        return totalStock;
    }

    private void displayAvailableColors() {
        if (productColors == null || productColors.isEmpty()) {
            tvSelectedColorName.setText("-");
            return;
        }

        colorOptionsContainer.removeAllViews(); // Clear any existing views

        List<String> availableColorsWithStock = new ArrayList<>();
        for (String color : productColors) {
             if (calculateColorStock(color) > 0) {
                 availableColorsWithStock.add(color);
             }
        }

        if (availableColorsWithStock.isEmpty()) {
            tvSelectedColorName.setText("-");
            // Optionally disable Buy Now/Add to Cart buttons
            return;
        }

        for (String color : availableColorsWithStock) {
            // Create a new View for the color dot/chip
            View colorDot = new View(this);
            int size = (int) (32 * getResources().getDisplayMetrics().density); // Increased size to 32dp for better tapping
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMarginEnd((int) (12 * getResources().getDisplayMetrics().density)); // Increased margin
            colorDot.setLayoutParams(params);
            colorDot.setBackgroundResource(R.drawable.bg_color_circle);

            // Set the actual color
            int colorRes = getColorResource(color);
            if (colorRes != 0) {
                colorDot.setBackgroundTintList(getColorStateList(colorRes));
            } else {
                // If color is not mapped, show a default gray with a border
                 colorDot.setBackgroundTintList(getColorStateList(R.color.default_color));
                 // You might want to add a border here as well
            }

            // Make it clickable and set background selector for visual feedback
            colorDot.setClickable(true);
            colorDot.setFocusable(true);
            colorDot.setBackgroundResource(R.drawable.bg_color_dot_selector);
            colorDot.setTag(color); // Store the color name in the tag

            // Set click listener
            colorDot.setOnClickListener(v -> {
                String selectedColorName = (String) v.getTag();
                if (selectedColorName != null) {
                    // Update selected color and UI
                    selectedColor = selectedColorName;
                    updateColorSelection();
                    updateSizeSpinner(); // Update size spinner based on selected color
                    // Update visual state of all dots to show selection
                    updateColorDotSelectionVisuals();
                }
            });

            colorOptionsContainer.addView(colorDot);
        }

        // After adding all available colors, set the default selected color to the first one with stock
        if (!availableColorsWithStock.isEmpty()) {
            selectedColor = availableColorsWithStock.get(0);
            updateColorSelection();
            updateSizeSpinner();
            // Update visual state for initial selection
            updateColorDotSelectionVisuals();
        }
    }

    private void updateColorDotSelectionVisuals() {
        if (colorOptionsContainer == null || selectedColor == null) return;

        for (int i = 0; i < colorOptionsContainer.getChildCount(); i++) {
            View colorDot = colorOptionsContainer.getChildAt(i);
            String colorName = (String) colorDot.getTag();
            if (colorName != null) {
                colorDot.setSelected(colorName.equals(selectedColor));
            }
        }
    }

    private void updateColorSelection() {
        TextView colorTextView = findViewById(R.id.tvSelectedColorName);
        if (colorTextView != null) {
             colorTextView.setText(selectedColor != null ? selectedColor : "-");
        }
        // Visual update of color dots is now handled in updateColorDotSelectionVisuals()
    }

    private void updateSizeSpinner() {
        if (selectedColor == null) return;

        // Get available sizes for selected color
        List<String> availableSizes = new ArrayList<>();
        for (Map<String, Object> entry : stockEntries) {
            String entryColor = (String) entry.get("color");
            String entrySize = (String) entry.get("size");
            Long stock = (Long) entry.get("stock");
            
            if (selectedColor.equals(entryColor) && stock != null && stock > 0) {
                availableSizes.add(entrySize);
            }
        }

        if (availableSizes.isEmpty()) {
            Toast.makeText(this, "No sizes available for selected color", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Update size spinner
        ArrayAdapter<String> sizeAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, availableSizes);
        sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sizeSpinner.setAdapter(sizeAdapter);

        // Set listener for size selection
        sizeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSize = availableSizes.get(position);
                updateStockForSelectedColor();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedSize = null;
            }
        });

        // Select first size by default
        if (!availableSizes.isEmpty()) {
            sizeSpinner.setSelection(0);
            selectedSize = availableSizes.get(0);
            updateStockForSelectedColor();
        }
    }

    private void updateStockForSelectedColor() {
        if (selectedColor == null || selectedSize == null) return;
        
        int stock = 0;
        for (Map<String, Object> entry : stockEntries) {
            String entryColor = (String) entry.get("color");
            String entrySize = (String) entry.get("size");
            if (selectedColor.equals(entryColor) && selectedSize.equals(entrySize)) {
                Long entryStock = (Long) entry.get("stock");
                if (entryStock != null) {
                    stock = entryStock.intValue();
                    break;
                }
            }
        }
        
        updateStockDisplay(stock);
        updateTotalPrice(); // Update total price when stock changes
    }

    private void updateStockDisplay(int stock) {
        if (tvStockDisplay != null) {
            tvStockDisplay.setText("Available Stock: " + stock);
        }

        // Update quantity spinner based on available stock
        List<String> quantities = new ArrayList<>();
        int maxQuantity = Math.min(stock, 10); // Limit to 10 items or available stock
        for (int i = 1; i <= maxQuantity; i++) {
            quantities.add(String.valueOf(i));
        }

        ArrayAdapter<String> quantityAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, quantities);
        quantityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        quantitySpinner.setAdapter(quantityAdapter);

        // Set listener for quantity selection
        quantitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedQuantity = Integer.parseInt(quantities.get(position));
                updateTotalPrice();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedQuantity = 0;
            }
        });

        // Select first quantity by default
        if (!quantities.isEmpty()) {
            quantitySpinner.setSelection(0);
            selectedQuantity = 1;
            updateTotalPrice();
        }
    }

    private boolean validateOrder() {
        if (selectedColor == null) {
            Toast.makeText(this, "Please select a color", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (selectedSize == null) {
            Toast.makeText(this, "Please select a size", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (selectedQuantity <= 0) {
            Toast.makeText(this, "Please select a valid quantity", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validate stock
        int availableStock = 0;
        for (Map<String, Object> entry : stockEntries) {
            String entryColor = (String) entry.get("color");
            String entrySize = (String) entry.get("size");
            if (selectedColor.equals(entryColor) && selectedSize.equals(entrySize)) {
                Long stock = (Long) entry.get("stock");
                if (stock != null) {
                    availableStock = stock.intValue();
                    break;
                }
            }
        }

        if (selectedQuantity > availableStock) {
            Toast.makeText(this, "Selected quantity exceeds available stock", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void processOrder() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String currentUserId = currentUser.getUid();

        // Calculate total amount
        double totalAmount = productPrice * selectedQuantity;

        // Start transaction to update stock and create order
        FirebaseFirestore.getInstance().runTransaction(transaction -> {
            // Get current product data
            DocumentReference productRef = FirebaseFirestore.getInstance()
                .collection("products").document(productId);
            DocumentSnapshot productDoc = transaction.get(productRef);

            if (!productDoc.exists()) {
                throw new FirebaseFirestoreException("Product not found", 
                    FirebaseFirestoreException.Code.NOT_FOUND);
            }

            // Get current stock entries
            List<Map<String, Object>> stockEntries = 
                (List<Map<String, Object>>) productDoc.get("stockEntries");
            if (stockEntries == null) {
                throw new FirebaseFirestoreException("No stock entries found", 
                    FirebaseFirestoreException.Code.NOT_FOUND);
            }

            // Find and update the matching stock entry and calculate new total stock
            boolean stockEntryUpdated = false;
            long newTotalStock = 0;
            for (Map<String, Object> entry : stockEntries) {
                String color = (String) entry.get("color");
                String size = (String) entry.get("size");
                Long currentStock = (Long) entry.get("stock");

                if (color != null && size != null && currentStock != null) {
                     if (color.equals(selectedColor) && size.equals(selectedSize)) {
                        if (currentStock < selectedQuantity) {
                            throw new FirebaseFirestoreException("Insufficient stock for selected variation", 
                                FirebaseFirestoreException.Code.FAILED_PRECONDITION);
                        }
                        entry.put("stock", currentStock - selectedQuantity);
                        stockEntryUpdated = true;
                    }
                    // Sum up stock from all entries (after updating the relevant one)
                    Long updatedStock = (Long) entry.get("stock");
                    if (updatedStock != null) {
                         newTotalStock += updatedStock;
                    }
                }
            }

            if (!stockEntryUpdated) {
                throw new FirebaseFirestoreException("Selected color/size combination not found in stock entries", 
                    FirebaseFirestoreException.Code.NOT_FOUND);
            }

            // Update product document: stockEntries and the overall stock field
            transaction.update(productRef, "stockEntries", stockEntries);
            transaction.update(productRef, "stock", newTotalStock); // Update the total stock field

            // Create order in orders collection
            DocumentReference orderRef = FirebaseFirestore.getInstance()
                .collection("orders").document();
            String orderId = orderRef.getId();
            Map<String, Object> orderData = new HashMap<>();
            orderData.put("productId", productId);
            orderData.put("sellerId", sellerId);
            orderData.put("productName", productName);
            orderData.put("productPrice", productPrice);
            orderData.put("quantity", selectedQuantity);
            orderData.put("selectedColor", selectedColor);
            orderData.put("selectedSize", selectedSize);
            orderData.put("totalAmount", totalAmount);
            orderData.put("customerId", currentUserId);
            orderData.put("customerName", etName.getText().toString().trim());
            orderData.put("customerPhone", etPhone.getText().toString().trim());
            orderData.put("deliveryAddress", etAddress.getText().toString().trim() + ", " + selectedBarangay);
            orderData.put("additionalDetails", etAdditionalDetails.getText().toString().trim());
            orderData.put("timestamp", FieldValue.serverTimestamp());
            orderData.put("status", "pending");
            orderData.put("orderDate", new java.util.Date());
            orderData.put("orderId", orderId);
            transaction.set(orderRef, orderData);

            // Add order reference to customer's orders subcollection
            DocumentReference customerOrderRef = FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUserId)
                .collection("orders")
                .document(orderId);
            transaction.set(customerOrderRef, orderData);

            // Add order reference to seller's orders subcollection
            DocumentReference sellerOrderRef = FirebaseFirestore.getInstance()
                .collection("sellers")
                .document(sellerId)
                .collection("orders")
                .document(orderId);
            transaction.set(sellerOrderRef, orderData);

            return null;
        })
        .addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Order placed successfully! Total: ₱" + String.format("%.2f", totalAmount), Toast.LENGTH_LONG).show();
            finish();
        })
        .addOnFailureListener(e -> {
            String errorMessage = "Failed to place order: ";
            if (e instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException fe = (FirebaseFirestoreException) e;
                switch (fe.getCode()) {
                    case NOT_FOUND:
                        errorMessage += "Product or stock entry not found";
                        break;
                    case FAILED_PRECONDITION:
                        errorMessage += "Insufficient stock";
                        break;
                    default:
                        errorMessage += e.getMessage();
                }
            } else {
                errorMessage += e.getMessage();
            }
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateTotalPrice() {
        if (selectedQuantity > 0) {
            double total = productPrice * selectedQuantity;
            TextView tvTotalPrice = findViewById(R.id.tvTotalPrice);
            if (tvTotalPrice != null) {
                tvTotalPrice.setText(String.format("Total: ₱%.2f", total));
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: " + e.getMessage());
        }
    }

    private int getColorResource(String colorName) {
        switch (colorName.toLowerCase()) {
            case "red": return R.color.product_red;
            case "blue": return R.color.product_blue;
            case "green": return R.color.product_green;
            case "black": return R.color.product_black;
            case "white": return R.color.product_white;
            case "yellow": return R.color.product_yellow;
            case "purple": return R.color.product_purple;
            case "pink": return R.color.product_pink;
            case "orange": return R.color.product_orange;
            case "gray": return R.color.product_gray;
            default: return R.color.default_color;
        }
    }
} 