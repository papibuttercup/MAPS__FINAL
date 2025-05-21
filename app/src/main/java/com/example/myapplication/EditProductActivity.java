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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import android.widget.EditText;
import android.widget.TextView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.widget.Spinner;
import android.widget.ArrayAdapter;

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
    private AlertDialog progressDialog;
    private Button btnSaveProduct;
    private RecyclerView rvProductImages;
    private ProductImagesAdapter productImagesAdapter;
    private Spinner spinnerStockColorEdit, spinnerStockSizeEdit, spinnerAvailableColorsEdit, spinnerAvailableSizesEdit;
    private EditText editTextStockValueEdit, etCustomColorEdit, etCustomSizeEdit;
    private Button buttonAddStockEntryEdit, btnAddColorEdit, btnRemoveColorEdit, btnAddSizeEdit, btnRemoveSizeEdit;
    private RecyclerView recyclerViewStockEntriesEdit;
    private ArrayList<StockEntry> stockEntriesEdit = new ArrayList<>();
    private StockEntryAdapterEdit stockEntryAdapterEdit;
    private ArrayList<String> colorListEdit = new ArrayList<>();
    private ArrayAdapter<String> colorAdapterEdit;
    private ArrayList<String> sizeListEdit = new ArrayList<>();
    private ArrayAdapter<String> sizeAdapterEdit;

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

        // Initialize progress dialog
        View progressView = getLayoutInflater().inflate(R.layout.progress_dialog, null);
        progressDialog = new AlertDialog.Builder(this)
            .setView(progressView)
            .setCancelable(false)
            .create();

        spinnerStockColorEdit = findViewById(R.id.spinnerStockColorEdit);
        spinnerStockSizeEdit = findViewById(R.id.spinnerStockSizeEdit);
        spinnerAvailableColorsEdit = findViewById(R.id.spinnerAvailableColorsEdit);
        spinnerAvailableSizesEdit = findViewById(R.id.spinnerAvailableSizesEdit);
        editTextStockValueEdit = findViewById(R.id.editTextStockValueEdit);
        etCustomColorEdit = findViewById(R.id.etCustomColorEdit);
        etCustomSizeEdit = findViewById(R.id.etCustomSizeEdit);
        buttonAddStockEntryEdit = findViewById(R.id.buttonAddStockEntryEdit);
        btnAddColorEdit = findViewById(R.id.btnAddColorEdit);
        btnRemoveColorEdit = findViewById(R.id.btnRemoveColorEdit);
        btnAddSizeEdit = findViewById(R.id.btnAddSizeEdit);
        btnRemoveSizeEdit = findViewById(R.id.btnRemoveSizeEdit);
        recyclerViewStockEntriesEdit = findViewById(R.id.recyclerViewStockEntriesEdit);
        colorAdapterEdit = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, colorListEdit);
        colorAdapterEdit.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sizeAdapterEdit = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sizeListEdit);
        sizeAdapterEdit.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStockColorEdit.setAdapter(colorAdapterEdit);
        spinnerStockSizeEdit.setAdapter(sizeAdapterEdit);
        spinnerAvailableColorsEdit.setAdapter(colorAdapterEdit);
        spinnerAvailableSizesEdit.setAdapter(sizeAdapterEdit);
        stockEntryAdapterEdit = new StockEntryAdapterEdit(stockEntriesEdit);
        recyclerViewStockEntriesEdit.setAdapter(stockEntryAdapterEdit);
        recyclerViewStockEntriesEdit.setLayoutManager(new LinearLayoutManager(this));
        buttonAddStockEntryEdit.setOnClickListener(v -> {
            String color = spinnerStockColorEdit.getSelectedItem() != null ? spinnerStockColorEdit.getSelectedItem().toString() : "";
            String size = spinnerStockSizeEdit.getSelectedItem() != null ? spinnerStockSizeEdit.getSelectedItem().toString() : "";
            String stockStr = editTextStockValueEdit.getText().toString().trim();
            if (color.isEmpty() || size.isEmpty() || stockStr.isEmpty()) {
                Toast.makeText(this, "Please select color, size, and enter stock", Toast.LENGTH_SHORT).show();
                return;
            }
            int stock = Integer.parseInt(stockStr);
            boolean found = false;
            for (StockEntry entry : stockEntriesEdit) {
                if (entry.color.equals(color) && entry.size.equals(size)) {
                    entry.stock = stock;
                    found = true;
                    break;
                }
            }
            if (!found) {
                stockEntriesEdit.add(new StockEntry(color, size, stock));
            }
            stockEntryAdapterEdit.notifyDataSetChanged();
        });
        btnAddColorEdit.setOnClickListener(v -> {
            String customColor = etCustomColorEdit.getText().toString().trim();
            // List of common color names (add more as needed)
            String[] allowedColors = {"Red", "Blue", "Green", "Black", "White", "Yellow", "Pink", "Purple", "Orange", "Brown", "Gray", "Grey", "Beige", "Violet", "Indigo", "Gold", "Silver", "Maroon", "Navy", "Teal", "Olive", "Cyan", "Magenta", "Turquoise", "Peach", "Mint", "Coral", "Lavender", "Burgundy", "Cream", "Khaki", "Tan", "Charcoal", "Aqua", "Lime", "Mustard", "Salmon", "Ivory", "Bronze", "Copper", "Rose", "Plum", "Azure", "Amber", "Emerald", "Sapphire", "Ruby", "Chocolate", "Sand", "Lilac", "Mauve", "Pearl", "Slate", "Denim", "Fuchsia", "Wine", "Cherry", "Sky", "Forest", "Sea", "Stone", "Blush", "Eggplant", "Mocha", "Onyx", "Jade", "Mint", "Rust", "Snow", "Sunflower", "Berry", "Graphite", "Pine", "Clay", "Dusty Rose", "Powder Blue", "Light Blue", "Light Green", "Light Pink", "Light Gray", "Dark Blue", "Dark Green", "Dark Red", "Dark Gray", "Dark Brown", "Off White"};
            boolean isAllowedColor = false;
            // Check if it's a valid hex code
            if (customColor.matches("#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})")) {
                isAllowedColor = true;
            } else {
                for (String color : allowedColors) {
                    if (color.equalsIgnoreCase(customColor)) {
                        isAllowedColor = true;
                        break;
                    }
                }
            }
            if (!isAllowedColor) {
                Toast.makeText(this, "Enter a valid color name (e.g., Red, Blue, Black, etc.) or hex code (e.g., #FF5733)", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!colorListEdit.contains(customColor)) {
                colorListEdit.add(customColor);
                colorAdapterEdit.notifyDataSetChanged();
                etCustomColorEdit.setText("");
            }
        });
        btnRemoveColorEdit.setOnClickListener(v -> {
            String color = spinnerAvailableColorsEdit.getSelectedItem() != null ? spinnerAvailableColorsEdit.getSelectedItem().toString() : "";
            if (!color.isEmpty() && colorListEdit.contains(color)) {
                colorListEdit.remove(color);
                colorAdapterEdit.notifyDataSetChanged();
            }
        });
        btnAddSizeEdit.setOnClickListener(v -> {
            String customSize = etCustomSizeEdit.getText().toString().trim();
            // Allow only typical sizes
            String[] allowedSizes = {"XS", "S", "M", "L", "XL", "XXL", "XXXL", "28", "30", "32", "34", "36", "38", "40", "42", "44", "46", "48", "50", "52"};
            boolean isAllowedSize = false;
            for (String size : allowedSizes) {
                if (size.equalsIgnoreCase(customSize)) {
                    isAllowedSize = true;
                    break;
                }
            }
            if (!isAllowedSize) {
                Toast.makeText(this, "Enter a valid size (XS, S, M, L, XL, XXL, 28-52, etc.)", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!sizeListEdit.contains(customSize)) {
                sizeListEdit.add(customSize);
                sizeAdapterEdit.notifyDataSetChanged();
                etCustomSizeEdit.setText("");
            }
        });
        btnRemoveSizeEdit.setOnClickListener(v -> {
            String size = spinnerAvailableSizesEdit.getSelectedItem() != null ? spinnerAvailableSizesEdit.getSelectedItem().toString() : "";
            if (!size.isEmpty() && sizeListEdit.contains(size)) {
                sizeListEdit.remove(size);
                sizeAdapterEdit.notifyDataSetChanged();
            }
        });
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
                        if (name != null) etProductName.setText(name);
                        // Load stockEntries
                        List<?> stockEntryList = (List<?>) documentSnapshot.get("stockEntries");
                        if (stockEntryList != null) {
                            stockEntriesEdit.clear();
                            for (Object obj : stockEntryList) {
                                if (obj instanceof java.util.Map) {
                                    java.util.Map map = (java.util.Map) obj;
                                    String color = map.get("color") != null ? map.get("color").toString() : "";
                                    String size = map.get("size") != null ? map.get("size").toString() : "";
                                    int entryStock = map.get("stock") != null ? Integer.parseInt(map.get("stock").toString()) : 0;
                                    stockEntriesEdit.add(new StockEntry(color, size, entryStock));
                                    if (!colorListEdit.contains(color)) colorListEdit.add(color);
                                    if (!sizeListEdit.contains(size)) sizeListEdit.add(size);
                                }
                            }
                            colorAdapterEdit.notifyDataSetChanged();
                            sizeAdapterEdit.notifyDataSetChanged();
                            stockEntryAdapterEdit.notifyDataSetChanged();
                        }
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
        showProgress("Saving...");
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
                    java.util.List<java.util.Map<String, Object>> stockEntryList = new java.util.ArrayList<>();
                    java.util.Set<String> colorSet = new java.util.HashSet<>();
                    java.util.Set<String> sizeSet = new java.util.HashSet<>();
                    for (StockEntry entry : stockEntriesEdit) {
                        java.util.Map<String, Object> map = new java.util.HashMap<>();
                        map.put("color", entry.color);
                        map.put("size", entry.size);
                        map.put("stock", entry.stock);
                        stockEntryList.add(map);
                        if (entry.color != null && !entry.color.trim().isEmpty()) colorSet.add(entry.color.trim());
                        if (entry.size != null && !entry.size.trim().isEmpty()) sizeSet.add(entry.size.trim());
                    }
                    FirebaseFirestore.getInstance().collection("products").document(productId)
                        .update("coverPhotoUri", newCoverUrl,
                                "productImageUris", newImages,
                                "name", newName,
                                "stockEntries", stockEntryList,
                                "colors", new ArrayList<>(colorSet),
                                "sizes", new ArrayList<>(sizeSet))
                        .addOnSuccessListener(unused -> runOnUiThread(() -> {
                            hideProgress();
                            Toast.makeText(this, "Product updated!", Toast.LENGTH_SHORT).show();
                            finish();
                        }))
                        .addOnFailureListener(e -> runOnUiThread(() -> {
                            hideProgress();
                            Toast.makeText(this, "Failed to update product: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }));
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        hideProgress();
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

    // 1. StockEntry class
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

    // 6. RecyclerView Adapter for stock entries
    public class StockEntryAdapterEdit extends RecyclerView.Adapter<StockEntryAdapterEdit.ViewHolder> {
        private ArrayList<StockEntry> stockEntries;
        public StockEntryAdapterEdit(ArrayList<StockEntry> stockEntries) {
            this.stockEntries = stockEntries;
        }
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            StockEntry entry = stockEntries.get(position);
            ((android.widget.TextView) holder.itemView.findViewById(android.R.id.text1)).setText(entry.color + " / " + entry.size);
            ((android.widget.TextView) holder.itemView.findViewById(android.R.id.text2)).setText("Stock: " + entry.stock);
        }
        @Override
        public int getItemCount() {
            return stockEntries.size();
        }
        public class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(View itemView) {
                super(itemView);
            }
        }
    }

    private void showProgress(String message) {
        if (progressDialog != null) {
            TextView messageView = progressDialog.findViewById(R.id.progressText);
            if (messageView != null) {
                messageView.setText(message);
            }
            progressDialog.show();
        }
    }

    private void hideProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
} 