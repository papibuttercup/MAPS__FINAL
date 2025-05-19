package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import android.widget.EditText;
import android.app.ProgressDialog;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class EditProductActivity extends AppCompatActivity {
    private ImageView coverPhotoImageView;
    private ImageView additionalImage1ImageView;
    private ImageView additionalImage2ImageView;
    private Button changeCoverPhotoButton;
    private Button changeImagesButton;
    private ActivityResultLauncher<Intent> coverPhotoLauncher;
    private ActivityResultLauncher<Intent> imagesLauncher;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private String productId;
    private String coverPhotoUri;
    private List<String> productImageUris = new ArrayList<>();
    private Uri newCoverPhotoUri = null;
    private Uri newImage1Uri = null;
    private Uri newImage2Uri = null;
    private EditText etProductName;
    private EditText etProductStock;
    private ProgressDialog progressDialog;
    private Button btnSaveProduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_product);

        coverPhotoImageView = findViewById(R.id.coverPhotoImageView);
        additionalImage1ImageView = findViewById(R.id.additionalImage1ImageView);
        additionalImage2ImageView = findViewById(R.id.additionalImage2ImageView);
        changeCoverPhotoButton = findViewById(R.id.changeCoverPhotoButton);
        changeImagesButton = findViewById(R.id.changeImagesButton);
        etProductName = findViewById(R.id.etProductName);
        etProductStock = findViewById(R.id.etProductStock);
        btnSaveProduct = findViewById(R.id.btnSaveProduct);

        changeCoverPhotoButton.setText("Edit Cover Photo");
        changeImagesButton.setText("Edit Images");

        productId = getIntent().getStringExtra("productId");
        if (productId != null) {
            fetchProductData(productId);
        }

        coverPhotoLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                newCoverPhotoUri = result.getData().getData();
                coverPhotoImageView.setImageURI(newCoverPhotoUri);
            }
        });

        imagesLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                if (result.getData().getClipData() != null) {
                    int count = result.getData().getClipData().getItemCount();
                    if (count > 0) {
                        newImage1Uri = result.getData().getClipData().getItemAt(0).getUri();
                        additionalImage1ImageView.setImageURI(newImage1Uri);
                    }
                    if (count > 1) {
                        newImage2Uri = result.getData().getClipData().getItemAt(1).getUri();
                        additionalImage2ImageView.setImageURI(newImage2Uri);
                    }
                } else if (result.getData().getData() != null) {
                    newImage1Uri = result.getData().getData();
                    additionalImage1ImageView.setImageURI(newImage1Uri);
                    newImage2Uri = null;
                    additionalImage2ImageView.setImageDrawable(null);
                }
            }
        });

        changeCoverPhotoButton.setOnClickListener(v -> {
            if (checkPermission()) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                coverPhotoLauncher.launch(intent);
            } else {
                requestPermission();
            }
        });

        changeImagesButton.setOnClickListener(v -> {
            if (checkPermission()) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                imagesLauncher.launch(intent);
            } else {
                requestPermission();
            }
        });

        btnSaveProduct.setOnClickListener(v -> saveProductChanges());

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving...");
    }

    private void fetchProductData(String productId) {
        FirebaseFirestore.getInstance().collection("products").document(productId)
            .get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    coverPhotoUri = documentSnapshot.getString("coverPhotoUri");
                    List<String> images = (List<String>) documentSnapshot.get("productImageUris");
                    if (images != null) productImageUris = images;
                    // Load cover photo
                    if (coverPhotoUri != null && !coverPhotoUri.isEmpty()) {
                        Glide.with(this).load(coverPhotoUri).into(coverPhotoImageView);
                    }
                    // Load additional images
                    if (productImageUris.size() > 0 && productImageUris.get(0) != null && !productImageUris.get(0).isEmpty()) {
                        Glide.with(this).load(productImageUris.get(0)).into(additionalImage1ImageView);
                    }
                    if (productImageUris.size() > 1 && productImageUris.get(1) != null && !productImageUris.get(1).isEmpty()) {
                        Glide.with(this).load(productImageUris.get(1)).into(additionalImage2ImageView);
                    }
                    // Load name and stock
                    String name = documentSnapshot.getString("name");
                    Long stock = documentSnapshot.getLong("stock");
                    if (name != null) etProductName.setText(name);
                    if (stock != null) etProductStock.setText(String.valueOf(stock));
                }
            });
    }

    private boolean checkPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveProductChanges() {
        progressDialog.show();
        List<java.util.concurrent.CompletableFuture<String>> uploadFutures = new ArrayList<>();
        // Cover photo
        if (newCoverPhotoUri != null) {
            uploadFutures.add(uploadImageToStorage(newCoverPhotoUri, "coverPhoto"));
        } else {
            uploadFutures.add(java.util.concurrent.CompletableFuture.completedFuture(coverPhotoUri));
        }
        // Image 1
        if (newImage1Uri != null) {
            uploadFutures.add(uploadImageToStorage(newImage1Uri, "image1"));
        } else {
            String img1 = (productImageUris.size() > 0) ? productImageUris.get(0) : null;
            uploadFutures.add(java.util.concurrent.CompletableFuture.completedFuture(img1));
        }
        // Image 2
        if (newImage2Uri != null) {
            uploadFutures.add(uploadImageToStorage(newImage2Uri, "image2"));
        } else {
            String img2 = (productImageUris.size() > 1) ? productImageUris.get(1) : null;
            uploadFutures.add(java.util.concurrent.CompletableFuture.completedFuture(img2));
        }
        java.util.concurrent.CompletableFuture.allOf(uploadFutures.toArray(new java.util.concurrent.CompletableFuture[0]))
            .thenAccept(v -> {
                try {
                    String newCoverUrl = uploadFutures.get(0).get();
                    String newImg1Url = uploadFutures.get(1).get();
                    String newImg2Url = uploadFutures.get(2).get();
                    List<String> newImages = new ArrayList<>();
                    if (newImg1Url != null) newImages.add(newImg1Url);
                    if (newImg2Url != null) newImages.add(newImg2Url);
                    String newName = etProductName.getText().toString().trim();
                    int newStock = 0;
                    try { newStock = Integer.parseInt(etProductStock.getText().toString().trim()); } catch (Exception ignored) {}
                    FirebaseFirestore.getInstance().collection("products").document(productId)
                        .update("coverPhotoUri", newCoverUrl,
                                "productImageUris", newImages,
                                "name", newName,
                                "stock", newStock)
                        .addOnSuccessListener(unused -> runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(this, "Product updated!", Toast.LENGTH_SHORT).show();
                            finish();
                        }))
                        .addOnFailureListener(e -> runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(this, "Failed to update product: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }));
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
    }

    private java.util.concurrent.CompletableFuture<String> uploadImageToStorage(Uri uri, String type) {
        java.util.concurrent.CompletableFuture<String> future = new java.util.concurrent.CompletableFuture<>();
        if (uri == null) {
            future.complete(null);
            return future;
        }
        String fileName = productId + "_" + type + ".jpg";
        StorageReference ref = FirebaseStorage.getInstance().getReference().child("product_images/" + fileName);
        ref.putFile(uri)
            .continueWithTask(task -> {
                if (!task.isSuccessful()) throw task.getException();
                return ref.getDownloadUrl();
            })
            .addOnSuccessListener(downloadUri -> future.complete(downloadUri.toString()))
            .addOnFailureListener(future::completeExceptionally);
        return future;
    }
} 