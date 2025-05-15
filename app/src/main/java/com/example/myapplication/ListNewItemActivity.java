package com.example.myapplication;

import android.app.ProgressDialog;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ListNewItemActivity extends AppCompatActivity {
    private static final int PICK_COVER_PHOTO_REQUEST = 1;
    private static final int PICK_IMAGES_REQUEST = 2;
    private EditText etName, etDescription, etPrice, etStock, etWeight, etParcelSize;
    private Spinner spinnerMainCategory, spinnerCategory;
    private Button btnAddCoverPhoto, btnAddImages, btnAddProduct;
    private ImageView imgCoverPreview;
    private LinearLayout layoutImagePreviews;
    private ArrayList<Uri> productImageUris = new ArrayList<>();
    private ProgressDialog progressDialog;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Uri coverPhotoUri;
    private String selectedMainCategory = "", selectedCategory = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_new_item);

        etName = findViewById(R.id.etName);
        etDescription = findViewById(R.id.etDescription);
        etPrice = findViewById(R.id.etPrice);
        etStock = findViewById(R.id.etStock);
        etWeight = findViewById(R.id.etWeight);
        etParcelSize = findViewById(R.id.etParcelSize);
        spinnerMainCategory = findViewById(R.id.spinnerCategory);
        spinnerCategory = findViewById(R.id.spinnerSubCategory);
        btnAddCoverPhoto = findViewById(R.id.btnAddCoverPhoto);
        btnAddImages = findViewById(R.id.btnAddImages);
        btnAddProduct = findViewById(R.id.btnAddProduct);
        imgCoverPreview = findViewById(R.id.imgCoverPreview);
        layoutImagePreviews = findViewById(R.id.layoutImagePreviews);
        progressDialog = new ProgressDialog(this);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupCategorySpinners();

        btnAddCoverPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Cover Photo"), PICK_COVER_PHOTO_REQUEST);
            }
        });
        btnAddImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Product Images (up to 8)"), PICK_IMAGES_REQUEST);
            }
        });

        btnAddProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addProductToFirestore();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_COVER_PHOTO_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            coverPhotoUri = data.getData();
            imgCoverPreview.setImageURI(coverPhotoUri);
            imgCoverPreview.setVisibility(View.VISIBLE);
        } else if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK && data != null) {
            productImageUris.clear();
            if (data.getClipData() != null) {
                int count = Math.min(data.getClipData().getItemCount(), 8);
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    productImageUris.add(imageUri);
                }
            } else if (data.getData() != null) {
                productImageUris.add(data.getData());
            }
            refreshImagePreviews();
        }
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
        String stockStr = etStock.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();
        String parcelSize = etParcelSize.getText().toString().trim();

        if (TextUtils.isEmpty(selectedMainCategory) || TextUtils.isEmpty(selectedCategory) ||
                TextUtils.isEmpty(name) || TextUtils.isEmpty(description) || TextUtils.isEmpty(priceStr) ||
                TextUtils.isEmpty(stockStr) || TextUtils.isEmpty(weightStr) || TextUtils.isEmpty(parcelSize) || coverPhotoUri == null) {
            Toast.makeText(this, "Please fill all fields and add a cover photo", Toast.LENGTH_SHORT).show();
            return;
        }
        if (productImageUris.size() == 0) {
            Toast.makeText(this, "Please add at least one product image", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        int stock;
        double weight;
        try {
            price = Double.parseDouble(priceStr);
            stock = Integer.parseInt(stockStr);
            weight = Double.parseDouble(weightStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid price, stock, or weight", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog.setMessage("Adding product...");
        progressDialog.show();

        // For demo: just store imageUri as string. In production, upload to Firebase Storage and get URL.
        String sellerId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        String sellerName = ""; // Optionally fetch seller name from Firestore if needed

        Map<String, Object> product = new HashMap<>();
        product.put("mainCategory", selectedMainCategory);
        product.put("category", selectedCategory);
        product.put("name", name);
        product.put("description", description);
        product.put("price", price);
        product.put("stock", stock);
        product.put("weight", weight);
        product.put("parcelSize", parcelSize);
        product.put("coverPhotoUri", coverPhotoUri.toString());
        ArrayList<String> imageUris = new ArrayList<>();
        for (Uri uri : productImageUris) imageUris.add(uri.toString());
        product.put("productImageUris", imageUris);
        product.put("isAvailable", true);
        product.put("sellerId", sellerId);
        product.put("sellerName", sellerName);
        product.put("createdAt", System.currentTimeMillis());

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
} 