package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import android.view.ViewGroup;
import android.widget.TextView;

public class ListNewItemActivity extends AppCompatActivity {
    private static final int PICK_COVER_PHOTO_REQUEST = 1;
    private static final int PICK_IMAGES_REQUEST = 2;
    private EditText etName, etDescription, etPrice, etWeight, etParcelSize, etCustomColor, etCustomSize;
    private Spinner spinnerMainCategory, spinnerCategory, spinnerStockColor, spinnerStockSize;
    private Button btnAddCoverPhoto, btnAddImages, btnAddProduct, btnAddColor, btnAddSize, buttonAddStockEntry;
    private ImageView imgCoverPreview;
    private LinearLayout layoutImagePreviews;
    private ArrayList<Uri> productImageUris = new ArrayList<>();
    private AlertDialog progressDialog;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Uri coverPhotoUri;
    private String selectedMainCategory = "", selectedCategory = "";
    private String editingProductId = null;
    private String editingCoverPhotoUri = null;
    private ArrayList<String> editingProductImageUris = new ArrayList<>();
    private EditText editTextStockValue;
    private RecyclerView recyclerViewStockEntries;
    private ArrayList<StockEntry> stockEntries = new ArrayList<>();
    private StockEntryAdapter stockEntryAdapter;

    private ActivityResultLauncher<Intent> coverPhotoLauncher;
    private ActivityResultLauncher<Intent> productImagesLauncher;

    private Spinner spinnerAvailableColors, spinnerAvailableSizes;
    private Button btnRemoveColor, btnRemoveSize;
    private ArrayList<String> colorList = new ArrayList<>();
    private ArrayAdapter<String> colorAdapter;
    private ArrayList<String> sizeList = new ArrayList<>();
    private ArrayAdapter<String> sizeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_new_item);

        etName = findViewById(R.id.etName);
        etDescription = findViewById(R.id.etDescription);
        etPrice = findViewById(R.id.etPrice);
        etWeight = findViewById(R.id.etWeight);
        etParcelSize = findViewById(R.id.etParcelSize);
        spinnerMainCategory = findViewById(R.id.spinnerCategory);
        spinnerCategory = findViewById(R.id.spinnerSubCategory);
        btnAddCoverPhoto = findViewById(R.id.btnAddCoverPhoto);
        btnAddImages = findViewById(R.id.btnAddImages);
        btnAddProduct = findViewById(R.id.btnAddProduct);
        btnAddColor = findViewById(R.id.btnAddColor);
        btnAddSize = findViewById(R.id.btnAddSize);
        imgCoverPreview = findViewById(R.id.imgCoverPreview);
        layoutImagePreviews = findViewById(R.id.layoutImagePreviews);
        progressDialog = new AlertDialog.Builder(this).setView(R.layout.progress_dialog).setCancelable(false).create();
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        editTextStockValue = findViewById(R.id.editTextStockValue);
        buttonAddStockEntry = findViewById(R.id.buttonAddStockEntry);
        recyclerViewStockEntries = findViewById(R.id.recyclerViewStockEntries);
        spinnerAvailableColors = findViewById(R.id.spinnerAvailableColors);
        spinnerAvailableSizes = findViewById(R.id.spinnerAvailableSizes);
        btnRemoveColor = findViewById(R.id.btnRemoveColor);
        btnRemoveSize = findViewById(R.id.btnRemoveSize);
        spinnerStockColor = findViewById(R.id.spinnerStockColor);
        spinnerStockSize = findViewById(R.id.spinnerStockSize);
        etCustomColor = findViewById(R.id.etCustomColor);
        etCustomSize = findViewById(R.id.etCustomSize);

        setupCategorySpinners();

        Intent intent = getIntent();
        if (intent.hasExtra("productId")) {
            editingProductId = intent.getStringExtra("productId");
            etName.setText(intent.getStringExtra("name"));
            etPrice.setText(String.valueOf(intent.getDoubleExtra("price", 0)));
            etWeight.setText(String.valueOf(intent.getDoubleExtra("weight", 0)));
            etParcelSize.setText(intent.getStringExtra("parcelSize"));
            editingCoverPhotoUri = intent.getStringExtra("coverPhotoUri");
            if (editingCoverPhotoUri != null && !editingCoverPhotoUri.isEmpty()) {
                imgCoverPreview.setVisibility(View.VISIBLE);
                imgCoverPreview.setImageURI(Uri.parse(editingCoverPhotoUri));
            }
            ArrayList<String> imgUris = intent.getStringArrayListExtra("productImageUris");
            if (imgUris != null) {
                editingProductImageUris = imgUris;
                for (String uri : imgUris) {
                    addImagePreview(Uri.parse(uri));
                }
            }
            // Set category spinners (optional: you may want to select the right items)
            // ...
            btnAddProduct.setText("Update Product");
            btnAddProduct.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateProductInFirestore(editingProductId);
                }
            });
        } else {
            btnAddCoverPhoto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    coverPhotoLauncher.launch(Intent.createChooser(intent, "Select Cover Photo"));
                }
            });
            btnAddImages.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    productImagesLauncher.launch(Intent.createChooser(intent, "Select Product Images (up to 8)"));
                }
            });

            btnAddProduct.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addProductToFirestore();
                }
            });
        }

        // Default colors and sizes
        colorList.add("Red");
        colorList.add("Blue");
        colorList.add("Green");
        colorList.add("Black");
        colorList.add("White");
        colorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, colorList);
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAvailableColors.setAdapter(colorAdapter);

        sizeList.add("S");
        sizeList.add("M");
        sizeList.add("L");
        sizeList.add("XL");
        sizeList.add("XXL");
        sizeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sizeList);
        sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAvailableSizes.setAdapter(sizeAdapter);

        spinnerStockColor.setAdapter(colorAdapter);
        spinnerStockSize.setAdapter(sizeAdapter);

        btnRemoveColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = spinnerAvailableColors.getSelectedItemPosition();
                if (pos >= 0 && colorList.size() > 1) {
                    colorList.remove(pos);
                    colorAdapter.notifyDataSetChanged();
                    spinnerStockColor.setAdapter(colorAdapter);
                }
            }
        });
        btnRemoveSize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = spinnerAvailableSizes.getSelectedItemPosition();
                if (pos >= 0 && sizeList.size() > 1) {
                    sizeList.remove(pos);
                    sizeAdapter.notifyDataSetChanged();
                    spinnerStockSize.setAdapter(sizeAdapter);
                }
            }
        });
        btnAddColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String color = etCustomColor.getText().toString().trim();
                // List of common color names (add more as needed)
                String[] allowedColors = {"Red", "Blue", "Green", "Black", "White", "Yellow", "Pink", "Purple", "Orange", "Brown", "Gray", "Grey", "Beige", "Violet", "Indigo", "Gold", "Silver", "Maroon", "Navy", "Teal", "Olive", "Cyan", "Magenta", "Turquoise", "Peach", "Mint", "Coral", "Lavender", "Burgundy", "Cream", "Khaki", "Tan", "Charcoal", "Aqua", "Lime", "Mustard", "Salmon", "Ivory", "Bronze", "Copper", "Rose", "Plum", "Azure", "Amber", "Emerald", "Sapphire", "Ruby", "Chocolate", "Sand", "Lilac", "Mauve", "Pearl", "Slate", "Denim", "Fuchsia", "Wine", "Cherry", "Sky", "Forest", "Sea", "Stone", "Blush", "Eggplant", "Mocha", "Onyx", "Jade", "Mint", "Rust", "Snow", "Sunflower", "Berry", "Graphite", "Pine", "Clay", "Dusty Rose", "Powder Blue", "Light Blue", "Light Green", "Light Pink", "Light Gray", "Dark Blue", "Dark Green", "Dark Red", "Dark Gray", "Dark Brown", "Off White"};
                boolean isAllowedColor = false;
                // Check if it's a valid hex code
                if (color.matches("#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})")) {
                    isAllowedColor = true;
                } else {
                    for (String allowed : allowedColors) {
                        if (allowed.equalsIgnoreCase(color)) {
                            isAllowedColor = true;
                            break;
                        }
                    }
                }
                if (!isAllowedColor) {
                    Toast.makeText(ListNewItemActivity.this, "Enter a valid color name (e.g., Red, Blue, Black, etc.) or hex code (e.g., #FF5733)", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!color.isEmpty() && !colorList.contains(color)) {
                    colorList.add(color);
                    colorAdapter.notifyDataSetChanged();
                    spinnerStockColor.setAdapter(colorAdapter);
                    etCustomColor.setText("");
                }
            }
        });
        btnAddSize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String size = etCustomSize.getText().toString().trim();
                if (!size.isEmpty() && !sizeList.contains(size)) {
                    sizeList.add(size);
                    sizeAdapter.notifyDataSetChanged();
                    spinnerStockSize.setAdapter(sizeAdapter);
                    etCustomSize.setText("");
                }
            }
        });

        setupActivityResultLaunchers();

        stockEntryAdapter = new StockEntryAdapter(stockEntries);
        recyclerViewStockEntries.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewStockEntries.setAdapter(stockEntryAdapter);

        buttonAddStockEntry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String color = spinnerStockColor.getSelectedItem().toString();
                String size = spinnerStockSize.getSelectedItem().toString();
                int stock = 0;
                try {
                    stock = Integer.parseInt(editTextStockValue.getText().toString());
                } catch (Exception ignored) {}
                boolean updated = false;
                for (StockEntry entry : stockEntries) {
                    if (entry.color.equals(color) && entry.size.equals(size)) {
                        entry.stock = stock;
                        updated = true;
                        break;
                    }
                }
                if (!updated) {
                    stockEntries.add(new StockEntry(color, size, stock));
                }
                stockEntryAdapter.notifyDataSetChanged();
                editTextStockValue.setText("");
            }
        });
    }

    private void setupCategorySpinners() {
        ArrayAdapter<CharSequence> mainCategoryAdapter = ArrayAdapter.createFromResource(this, R.array.main_categories, android.R.layout.simple_spinner_item);
        mainCategoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMainCategory.setAdapter(mainCategoryAdapter);
        spinnerMainCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMainCategory = parent.getItemAtPosition(position).toString();
                int catArrayId = R.array.categories_women;
                if (selectedMainCategory.equals("Women")) catArrayId = R.array.categories_women;
                else if (selectedMainCategory.equals("Men")) catArrayId = R.array.categories_men;
                else if (selectedMainCategory.equals("Kids")) catArrayId = R.array.categories_kids;
                ArrayAdapter<CharSequence> catAdapter = ArrayAdapter.createFromResource(ListNewItemActivity.this, catArrayId, android.R.layout.simple_spinner_item);
                catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCategory.setAdapter(catAdapter);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = parent.getItemAtPosition(position).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupActivityResultLaunchers() {
        coverPhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    coverPhotoUri = result.getData().getData();
                    imgCoverPreview.setImageURI(coverPhotoUri);
                    imgCoverPreview.setVisibility(View.VISIBLE);
                }
            }
        );

        productImagesLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    if (result.getData().getClipData() != null) {
                        for (int i = 0; i < result.getData().getClipData().getItemCount(); i++) {
                            Uri imageUri = result.getData().getClipData().getItemAt(i).getUri();
                            if (productImageUris.size() < 8) {
                                productImageUris.add(imageUri);
                            }
                        }
                    }
                    Toast.makeText(this, "Selected " + productImageUris.size() + " images", Toast.LENGTH_SHORT).show();
                    refreshImagePreviews();
                }
            }
        );
    }

    private void addImagePreview(final Uri uri) {
        FrameLayout frame = new FrameLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(180, 180);
        params.setMargins(8, 8, 8, 8);
        frame.setLayoutParams(params);

        ImageView imageView = new ImageView(this);
        FrameLayout.LayoutParams imgParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        imageView.setLayoutParams(imgParams);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageURI(uri);

        // Remove button
        ImageView removeBtn = new ImageView(this);
        FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(60, 60);
        btnParams.topMargin = 0;
        btnParams.rightMargin = 0;
        btnParams.gravity = android.view.Gravity.END | android.view.Gravity.TOP;
        removeBtn.setLayoutParams(btnParams);
        removeBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        removeBtn.setBackgroundResource(android.R.color.white);
        removeBtn.setPadding(8, 8, 8, 8);
        removeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                productImageUris.remove(uri);
                refreshImagePreviews();
            }
        });

        frame.addView(imageView);
        frame.addView(removeBtn);
        layoutImagePreviews.addView(frame);
    }

    private void refreshImagePreviews() {
        layoutImagePreviews.removeAllViews();
        for (Uri uri : productImageUris) {
            addImagePreview(uri);
        }
    }

    private void addProductToFirestore() {
        String name = etName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();
        String parcelSize = etParcelSize.getText().toString().trim();

        if (TextUtils.isEmpty(selectedMainCategory) || TextUtils.isEmpty(selectedCategory) ||
                TextUtils.isEmpty(name) || TextUtils.isEmpty(description) || TextUtils.isEmpty(priceStr) ||
                TextUtils.isEmpty(weightStr) || TextUtils.isEmpty(parcelSize) || coverPhotoUri == null) {
            Toast.makeText(this, "Please fill all fields and add a cover photo", Toast.LENGTH_SHORT).show();
            return;
        }
        if (productImageUris.size() == 0) {
            Toast.makeText(this, "Please add at least one product image", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        double weight;
        try {
            price = Double.parseDouble(priceStr);
            weight = Double.parseDouble(weightStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid price or weight", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.show();
        progressDialog.setMessage("Uploading images...");

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        String sellerId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        long timestamp = System.currentTimeMillis();
        StorageReference coverRef = storageRef.child("products/" + sellerId + "/" + timestamp + "_cover.jpg");

        coverRef.putFile(coverPhotoUri)
            .continueWithTask(task -> {
                if (!task.isSuccessful()) throw task.getException();
                return coverRef.getDownloadUrl();
            })
            .addOnSuccessListener(coverUri -> {
                uploadProductImages(storageRef, sellerId, timestamp, coverUri.toString(), name, description, price, weight, parcelSize);
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Failed to upload cover photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void uploadProductImages(StorageReference storageRef, String sellerId, long timestamp, String coverUrl, String name, String description, double price, double weight, String parcelSize) {
        ArrayList<String> imageUrls = new ArrayList<>();
        ArrayList<Uri> uris = new ArrayList<>(productImageUris);
        if (uris.isEmpty()) {
            saveProductToFirestore(coverUrl, imageUrls, name, description, price, weight, parcelSize);
            return;
        }
        uploadNextImage(storageRef, sellerId, timestamp, uris, imageUrls, 0, coverUrl, name, description, price, weight, parcelSize);
    }

    private void uploadNextImage(StorageReference storageRef, String sellerId, long timestamp, ArrayList<Uri> uris, ArrayList<String> imageUrls, int index, String coverUrl, String name, String description, double price, double weight, String parcelSize) {
        if (index >= uris.size()) {
            saveProductToFirestore(coverUrl, imageUrls, name, description, price, weight, parcelSize);
            return;
        }
        Uri uri = uris.get(index);
        StorageReference imgRef = storageRef.child("products/" + sellerId + "/" + timestamp + "_img_" + index + ".jpg");
        imgRef.putFile(uri)
            .continueWithTask(task -> {
                if (!task.isSuccessful()) throw task.getException();
                return imgRef.getDownloadUrl();
            })
            .addOnSuccessListener(imgUri -> {
                imageUrls.add(imgUri.toString());
                uploadNextImage(storageRef, sellerId, timestamp, uris, imageUrls, index + 1, coverUrl, name, description, price, weight, parcelSize);
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Failed to upload product image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void saveProductToFirestore(String coverUrl, ArrayList<String> imageUrls, String name, String description, double price, double weight, String parcelSize) {
        String sellerId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        String sellerName = ""; // Optionally fetch seller name from Firestore if needed
        Map<String, Object> product = new HashMap<>();
        product.put("mainCategory", selectedMainCategory);
        product.put("category", selectedCategory);
        product.put("name", name);
        product.put("description", description);
        product.put("price", price);
        product.put("weight", weight);
        product.put("parcelSize", parcelSize);
        product.put("coverPhotoUri", coverUrl);
        product.put("productImageUris", imageUrls);
        product.put("isAvailable", true);
        product.put("sellerId", sellerId);
        product.put("sellerName", sellerName);
        product.put("createdAt", System.currentTimeMillis());

        List<Map<String, Object>> stockEntryList = new ArrayList<>();
        java.util.Set<String> colorSet = new java.util.HashSet<>();
        java.util.Set<String> sizeSet = new java.util.HashSet<>();
        for (StockEntry entry : stockEntries) {
            Map<String, Object> map = new HashMap<>();
            map.put("color", entry.color);
            map.put("size", entry.size);
            map.put("stock", entry.stock);
            stockEntryList.add(map);
            if (entry.color != null && !entry.color.trim().isEmpty()) colorSet.add(entry.color.trim());
            if (entry.size != null && !entry.size.trim().isEmpty()) sizeSet.add(entry.size.trim());
        }
        product.put("stockEntries", stockEntryList);
        product.put("colors", new ArrayList<>(colorSet));
        product.put("sizes", new ArrayList<>(sizeSet));

        db.collection("products")
            .add(product)
            .addOnSuccessListener(documentReference -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Product added successfully!", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(this, "Failed to add product: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void updateProductInFirestore(String productId) {
        String name = etName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();
        String parcelSize = etParcelSize.getText().toString().trim();
        double price;
        double weight;
        try {
            price = Double.parseDouble(priceStr);
            weight = Double.parseDouble(weightStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid price or weight", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("description", description);
        updates.put("price", price);
        updates.put("weight", weight);
        updates.put("parcelSize", parcelSize);
        // Add stockEntries update
        List<Map<String, Object>> stockEntryList = new ArrayList<>();
        java.util.Set<String> colorSet = new java.util.HashSet<>();
        java.util.Set<String> sizeSet = new java.util.HashSet<>();
        for (StockEntry entry : stockEntries) {
            Map<String, Object> map = new HashMap<>();
            map.put("color", entry.color);
            map.put("size", entry.size);
            map.put("stock", entry.stock);
            stockEntryList.add(map);
            if (entry.color != null && !entry.color.trim().isEmpty()) colorSet.add(entry.color.trim());
            if (entry.size != null && !entry.size.trim().isEmpty()) sizeSet.add(entry.size.trim());
        }
        updates.put("stockEntries", stockEntryList);
        updates.put("colors", new ArrayList<>(colorSet));
        updates.put("sizes", new ArrayList<>(sizeSet));
        db.collection("products").document(productId)
            .update(updates)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Product updated successfully!", Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to update product: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    public static class StockEntry {
        public String color;
        public String size;
        public int stock;
        public StockEntry(String color, String size, int stock) {
            this.color = color;
            this.size = size;
            this.stock = stock;
        }
    }

    public class StockEntryAdapter extends RecyclerView.Adapter<StockEntryAdapter.ViewHolder> {
        private ArrayList<StockEntry> entries;
        public StockEntryAdapter(ArrayList<StockEntry> entries) { this.entries = entries; }
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setPadding(16, 16, 16, 16);
            return new ViewHolder(tv);
        }
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            StockEntry entry = entries.get(position);
            ((TextView) holder.itemView).setText(entry.color + " - " + entry.size + ": " + entry.stock);
        }
        @Override
        public int getItemCount() { return entries.size(); }
        public class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(View itemView) { super(itemView); }
        }
    }
} 