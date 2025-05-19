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
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageButton;
import androidx.recyclerview.widget.LinearLayoutManager;

public class EditProductActivity extends AppCompatActivity {
    private ImageView coverPhotoImageView;
    private Button changeCoverPhotoButton;
    private Button changeImagesButton;
    private ActivityResultLauncher<Intent> coverPhotoLauncher;
    private ActivityResultLauncher<Intent> imagesLauncher;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private String productId;
    private String coverPhotoUri;
    private List<String> productImageUris = new ArrayList<>();
    private Uri newCoverPhotoUri = null;
    private EditText etProductName;
    private EditText etProductStock;
    private ProgressDialog progressDialog;
    private Button btnSaveProduct;
    private RecyclerView rvProductImages;
    private ProductImagesAdapter productImagesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toast.makeText(this, "onCreate started", Toast.LENGTH_SHORT).show();
        setContentView(R.layout.activity_edit_product);
        coverPhotoImageView = findViewById(R.id.coverPhotoImageView);
        Toast.makeText(this, "coverPhotoImageView found", Toast.LENGTH_SHORT).show();
        changeCoverPhotoButton = findViewById(R.id.changeCoverPhotoButton);
        Toast.makeText(this, "changeCoverPhotoButton found", Toast.LENGTH_SHORT).show();
        changeImagesButton = findViewById(R.id.changeImagesButton);
        Toast.makeText(this, "changeImagesButton found", Toast.LENGTH_SHORT).show();
        etProductName = findViewById(R.id.etProductName);
        etProductStock = findViewById(R.id.etProductStock);
        btnSaveProduct = findViewById(R.id.btnSaveProduct);
        rvProductImages = findViewById(R.id.rvProductImages);
        rvProductImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        Toast.makeText(this, "rvProductImages found", Toast.LENGTH_SHORT).show();

        changeImagesButton.setVisibility(View.VISIBLE);

        productId = getIntent().getStringExtra("productId");
        if (productId != null) {
            Toast.makeText(this, "Fetching product data", Toast.LENGTH_SHORT).show();
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
                productImageUris.clear();
                if (result.getData().getClipData() != null) {
                    int count = result.getData().getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        Uri uri = result.getData().getClipData().getItemAt(i).getUri();
                        productImageUris.add(uri.toString());
                    }
                } else if (result.getData().getData() != null) {
                    Uri uri = result.getData().getData();
                    productImageUris.add(uri.toString());
                }
                productImagesAdapter.notifyDataSetChanged();
            }
        });
        Toast.makeText(this, "Adapters and launchers set up", Toast.LENGTH_SHORT).show();

        productImagesAdapter = new ProductImagesAdapter(productImageUris, uri -> {
            int idx = productImageUris.indexOf(uri);
            if (idx >= 0) {
                productImageUris.remove(idx);
                productImagesAdapter.notifyDataSetChanged();
            }
        });
        rvProductImages.setAdapter(productImagesAdapter);
        Toast.makeText(this, "Adapter set on RecyclerView", Toast.LENGTH_SHORT).show();

        changeCoverPhotoButton.setText("Edit Cover Photo");
        changeImagesButton.setText("Edit Images");

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
        Toast.makeText(this, "fetchProductData called", Toast.LENGTH_SHORT).show();
        FirebaseFirestore.getInstance().collection("products").document(productId)
            .get().addOnSuccessListener(documentSnapshot -> {
                try {
                    if (documentSnapshot.exists()) {
                        coverPhotoUri = documentSnapshot.getString("coverPhotoUri");
                        Object imagesObj = documentSnapshot.get("productImageUris");
                        productImageUris.clear();
                        if (imagesObj instanceof List<?>) {
                            List<?> imagesList = (List<?>) imagesObj;
                            for (Object o : imagesList) {
                                if (o instanceof String) productImageUris.add((String) o);
                            }
                        }
                        // Load cover photo
                        if (coverPhotoUri != null && !coverPhotoUri.isEmpty()) {
                            Glide.with(this).load(coverPhotoUri).into(coverPhotoImageView);
                        }
                        // Load name and stock
                        String name = documentSnapshot.getString("name");
                        Long stock = documentSnapshot.getLong("stock");
                        if (name != null) etProductName.setText(name);
                        if (stock != null) etProductStock.setText(String.valueOf(stock));
                        // Update carousel
                        if (productImagesAdapter != null) productImagesAdapter.notifyDataSetChanged();
                        Toast.makeText(this, "Loaded " + productImageUris.size() + " product images", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Error loading product images: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Firestore error: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
        // Product images
        for (int i = 0; i < productImageUris.size(); i++) {
            String uriStr = productImageUris.get(i);
            Uri uri = null;
            try { uri = Uri.parse(uriStr); } catch (Exception ignored) {}
            if (uri != null && uri.getScheme() != null && (uri.getScheme().equals("content") || uri.getScheme().equals("file"))) {
                uploadFutures.add(uploadImageToStorage(uri, "image" + i));
            } else {
                uploadFutures.add(java.util.concurrent.CompletableFuture.completedFuture(uriStr));
            }
        }
        java.util.concurrent.CompletableFuture.allOf(uploadFutures.toArray(new java.util.concurrent.CompletableFuture[0]))
            .thenAccept(v -> {
                try {
                    String newCoverUrl = uploadFutures.get(0).get();
                    List<String> newImages = new ArrayList<>();
                    for (int i = 1; i < uploadFutures.size(); i++) {
                        String imgUrl = uploadFutures.get(i).get();
                        if (imgUrl != null) newImages.add(imgUrl);
                    }
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

    // Adapter for product images carousel
    private static class ProductImagesAdapter extends RecyclerView.Adapter<ProductImagesAdapter.ImageViewHolder> {
        private List<String> imageUris;
        private OnRemoveImageListener removeListener;
        interface OnRemoveImageListener {
            void onRemove(String uri);
        }
        ProductImagesAdapter(List<String> imageUris, OnRemoveImageListener removeListener) {
            this.imageUris = imageUris;
            this.removeListener = removeListener;
        }
        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_edit_product_image, parent, false);
            return new ImageViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            String uri = imageUris.get(position);
            Glide.with(holder.imageView.getContext()).load(uri).into(holder.imageView);
            holder.btnRemove.setOnClickListener(v -> removeListener.onRemove(uri));
        }
        @Override
        public int getItemCount() {
            return imageUris.size();
        }
        static class ImageViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            ImageButton btnRemove;
            ImageViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.imageViewProduct);
                btnRemove = itemView.findViewById(R.id.btnRemoveImage);
            }
        }
    }
} 