package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import android.view.ViewGroup;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.widget.CheckBox;
import android.widget.ImageButton;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import android.widget.AutoCompleteTextView;
import androidx.annotation.NonNull;
import java.util.Arrays;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import android.util.Log;
import java.util.concurrent.atomic.AtomicInteger;
import android.view.Gravity;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.UploadTask;
import android.os.Handler;
import android.os.Looper;
import com.google.android.gms.tasks.Tasks;

public class ListNewItemActivity extends AppCompatActivity {
    private static final int PICK_COVER_PHOTO_REQUEST = 1;
    private static final int PICK_IMAGES_REQUEST = 2;
    private static final int MAX_IMAGE_DIMENSION = 1024; // Maximum width/height for compressed images
    private static final int JPEG_QUALITY = 80; // JPEG compression quality (0-100)

    // UI Components
    private TextInputEditText productNameInput;
    private TextInputEditText descriptionInput;
    private TextInputEditText priceInput;
    private AutoCompleteTextView categoryInput;
    private AutoCompleteTextView colorInput;
    private AutoCompleteTextView sizeInput;
    private TextInputEditText stockInput;
    private MaterialButton addStockButton;
    private RecyclerView stockEntriesRecyclerView;
    private TextView totalStockText;
    private MaterialButton listProductButton;
    private ImageView imgCoverPreview;
    private LinearLayout layoutImagePreviews;

    // Data
    private ArrayList<Uri> productImageUris = new ArrayList<>();
    private Uri coverPhotoUri;
    private ArrayList<StockEntry> stockEntries = new ArrayList<>();
    private StockEntryAdapter stockEntryAdapter;
    private int totalStock = 0;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Activity Results
    private ActivityResultLauncher<Intent> coverPhotoLauncher;
    private ActivityResultLauncher<Intent> productImagesLauncher;

    // Dialog
    private AlertDialog progressDialog;

    // Add database reference for categories
    private DatabaseReference categoriesRef;
    private List<String> categoryList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_new_item);

        // Initialize Firebase Database reference for categories
        categoriesRef = FirebaseDatabase.getInstance().getReference("categories");

        initializeViews();
        initializeData();
        setupListeners();
        setupStockManagement();
        setupImagePickers();
        setupListProductButton();
        loadCategories();
    }

    private void initializeViews() {
        // Product details
        productNameInput = findViewById(R.id.productNameInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        priceInput = findViewById(R.id.priceInput);
        categoryInput = findViewById(R.id.categoryInput);
        
        // Stock management
        colorInput = findViewById(R.id.colorInput);
        sizeInput = findViewById(R.id.sizeInput);
        stockInput = findViewById(R.id.stockInput);
        addStockButton = findViewById(R.id.addStockButton);
        stockEntriesRecyclerView = findViewById(R.id.stockEntriesRecyclerView);
        totalStockText = findViewById(R.id.totalStockText);
        
        // Image management
        imgCoverPreview = findViewById(R.id.imgCoverPreview);
        layoutImagePreviews = findViewById(R.id.layoutImagePreviews);
        
        // Action buttons
        listProductButton = findViewById(R.id.listProductButton);
        
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        
        // Initialize progress dialog
        progressDialog = new AlertDialog.Builder(this)
            .setView(R.layout.progress_dialog)
            .setCancelable(false)
            .create();
            
        // Setup RecyclerView
        stockEntryAdapter = new StockEntryAdapter(stockEntries);
        stockEntriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        stockEntriesRecyclerView.setAdapter(stockEntryAdapter);

        Intent intent = getIntent();
        if (intent.hasExtra("productId")) {
            // Handle existing product
            // ... existing code ...
        } else {
            // Handle new product
            // ... existing code ...
        }

        // Initialize category input with proper styling
        categoryInput.setHint("Select a category");
        categoryInput.setFocusable(true);
        categoryInput.setFocusableInTouchMode(true);
        categoryInput.setThreshold(0); // Show all suggestions immediately
        categoryInput.setOnClickListener(v -> categoryInput.showDropDown());
        categoryInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                categoryInput.showDropDown();
            }
        });
        
        // Load categories immediately
        loadCategories();
    }

    private void initializeData() {
        // Set up predefined colors
        String[] colors = {
            "Black", "White", "Red", "Blue", "Green", "Yellow", 
            "Purple", "Pink", "Orange", "Brown", "Gray", "Navy",
            "Beige", "Burgundy", "Teal", "Maroon", "Olive", "Cyan"
        };
        
        // Create adapter for color input
        ArrayAdapter<String> colorAdapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            colors
        );
        colorInput.setAdapter(colorAdapter);
        
        // Set up predefined sizes
        String[] sizes = {"XXL", "XL", "L", "M", "S", "XS"};
        ArrayAdapter<String> sizeAdapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            sizes
        );
        sizeInput.setAdapter(sizeAdapter);
    }

    private void setupListeners() {
        // ... existing code ...
    }

    private void updateSelectedCount() {
        // ... existing code ...
    }

    private void setupCategorySpinners() {
        // ... existing code ...
    }

    private void setupActivityResultLaunchers() {
        // ... existing code ...
    }

    private void setupImagePickers() {
        // Create intents for image picking
        Intent coverPhotoIntent = new Intent(Intent.ACTION_GET_CONTENT);
        coverPhotoIntent.setType("image/*");
        coverPhotoIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);

        Intent additionalImagesIntent = new Intent(Intent.ACTION_GET_CONTENT);
        additionalImagesIntent.setType("image/*");
        additionalImagesIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        // Setup launcher for cover photo
        coverPhotoLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImage = result.getData().getData();
                    if (selectedImage != null) {
                        try {
                            // Compress the cover photo before displaying
                            Uri compressedUri = getCompressedImageUri(selectedImage);
                            coverPhotoUri = selectedImage; // Keep original URI for upload
                            imgCoverPreview.setImageURI(compressedUri);
                            imgCoverPreview.setVisibility(View.VISIBLE);
                            productImageUris.clear(); // Clear previous images
                            productImageUris.add(selectedImage); // Add cover photo as first image
                        } catch (IOException e) {
                            Log.e("ListNewItemActivity", "Error compressing cover photo: " + e.getMessage());
                            Toast.makeText(this, "Error processing image. Please try another one.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        );

        // Setup launcher for additional images
        productImagesLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    if (result.getData().getClipData() != null) {
                        // Multiple images selected
                        int count = result.getData().getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            Uri imageUri = result.getData().getClipData().getItemAt(i).getUri();
                            addImagePreview(imageUri);
                            productImageUris.add(imageUri);
                        }
                    } else if (result.getData().getData() != null) {
                        // Single image selected
                        Uri imageUri = result.getData().getData();
                        addImagePreview(imageUri);
                        productImageUris.add(imageUri);
                    }
                }
            }
        );

        // Set click listeners for the buttons
        findViewById(R.id.addCoverPhotoButton).setOnClickListener(v -> {
            coverPhotoLauncher.launch(coverPhotoIntent);
        });

        findViewById(R.id.addAdditionalImagesButton).setOnClickListener(v -> {
            productImagesLauncher.launch(additionalImagesIntent);
        });
    }

    private Bitmap compressImage(Uri imageUri) throws IOException {
        // Get image dimensions
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri), null, options);
        
        // Calculate sample size for downsampling
        int sampleSize = 1;
        if (options.outHeight > MAX_IMAGE_DIMENSION || options.outWidth > MAX_IMAGE_DIMENSION) {
            final int heightRatio = Math.round((float) options.outHeight / (float) MAX_IMAGE_DIMENSION);
            final int widthRatio = Math.round((float) options.outWidth / (float) MAX_IMAGE_DIMENSION);
            sampleSize = Math.max(heightRatio, widthRatio);
        }
        
        // Decode image with calculated sample size
        options.inJustDecodeBounds = false;
        options.inSampleSize = sampleSize;
        return BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri), null, options);
    }

    private Uri getCompressedImageUri(Uri originalUri) throws IOException {
        Bitmap compressedBitmap = compressImage(originalUri);
        
        // Create a temporary file to store the compressed image
        File tempFile = File.createTempFile("compressed_", ".jpg", getCacheDir());
        FileOutputStream out = new FileOutputStream(tempFile);
        
        // Compress the bitmap to JPEG
        compressedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
        out.close();
        compressedBitmap.recycle();
        
        return Uri.fromFile(tempFile);
    }

    private void addImagePreview(final Uri uri) {
        try {
            // Compress the image before displaying
            Uri compressedUri = getCompressedImageUri(uri);
            
            // Create image preview container
            FrameLayout imageContainer = new FrameLayout(this);
            LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                dpToPx(120), // width
                dpToPx(120)  // height
            );
            containerParams.setMargins(dpToPx(8), 0, 0, 0);
            imageContainer.setLayoutParams(containerParams);

            // Create image view
            ImageView imageView = new ImageView(this);
            FrameLayout.LayoutParams imageParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            );
            imageView.setLayoutParams(imageParams);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setImageURI(compressedUri);
            imageView.setBackgroundResource(R.drawable.image_placeholder_background);

            // Create delete button
            ImageButton deleteButton = new ImageButton(this);
            FrameLayout.LayoutParams deleteParams = new FrameLayout.LayoutParams(
                dpToPx(24),
                dpToPx(24)
            );
            deleteParams.gravity = Gravity.TOP | Gravity.END;
            deleteParams.setMargins(0, dpToPx(4), dpToPx(4), 0);
            deleteButton.setLayoutParams(deleteParams);
            deleteButton.setImageResource(R.drawable.ic_delete);
            deleteButton.setBackgroundResource(android.R.color.transparent);
            deleteButton.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));

            // Add click listener to delete button
            deleteButton.setOnClickListener(v -> {
                layoutImagePreviews.removeView(imageContainer);
                productImageUris.remove(uri);
                if (uri.equals(coverPhotoUri)) {
                    coverPhotoUri = null;
                    imgCoverPreview.setVisibility(View.GONE);
                }
            });

            // Add views to container
            imageContainer.addView(imageView);
            imageContainer.addView(deleteButton);

            // Add container to preview layout
            layoutImagePreviews.addView(imageContainer);
        } catch (IOException e) {
            Log.e("ListNewItemActivity", "Error compressing image: " + e.getMessage());
            Toast.makeText(this, "Error processing image. Please try another one.", Toast.LENGTH_SHORT).show();
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void refreshImagePreviews() {
        layoutImagePreviews.removeAllViews();
        for (Uri uri : productImageUris) {
            if (!uri.equals(coverPhotoUri)) { // Don't add cover photo to additional images
                addImagePreview(uri);
            }
        }
    }

    private boolean validateInputs() {
        String name = productNameInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String priceStr = priceInput.getText().toString().trim();
        String category = categoryInput.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Please enter product name", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(description)) {
            Toast.makeText(this, "Please enter product description", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(priceStr)) {
            Toast.makeText(this, "Please enter product price", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(category)) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (coverPhotoUri == null) {
            Toast.makeText(this, "Please add a cover photo", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (stockEntries.isEmpty()) {
            Toast.makeText(this, "Please add at least one stock entry", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void setupListProductButton() {
        listProductButton.setOnClickListener(v -> {
            if (validateInputs()) {
                saveProduct();
            }
        });
    }

    private void saveProduct() {
        if (!validateInputs()) {
            return;
        }

        // Show progress dialog with initial message
        TextView progressText = progressDialog.findViewById(R.id.progressText);
        if (progressText != null) {
            progressText.setText("Preparing to upload...");
        }
        progressDialog.show();
        
        // Set a timeout for the entire save process
        Handler saveTimeoutHandler = new Handler(Looper.getMainLooper());
        Runnable saveTimeoutRunnable = () -> {
            if (progressDialog.isShowing()) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Operation timed out. Please try again.", Toast.LENGTH_LONG).show();
                });
            }
        };
        saveTimeoutHandler.postDelayed(saveTimeoutRunnable, 60000); // 60 second timeout for entire save process
        
        // Upload images first
        uploadImages(productImageUris, imageUrls -> {
            saveTimeoutHandler.removeCallbacks(saveTimeoutRunnable);
            if (imageUrls != null && !imageUrls.isEmpty()) {
                saveProductData(imageUrls);
            } else {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "No images were uploaded successfully", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void uploadImages(List<Uri> images, OnImagesUploadedListener listener) {
        if (images == null || images.isEmpty()) {
            listener.onImagesUploaded(new ArrayList<>());
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        ArrayList<String> uploadedUrls = new ArrayList<>();
        int totalImages = images.size();
        AtomicInteger uploadedCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        AtomicInteger retryCount = new AtomicInteger(0);
        final int MAX_RETRIES = 3;

        // Update progress dialog message
        TextView progressText = progressDialog.findViewById(R.id.progressText);
        if (progressText != null) {
            progressText.setText("Uploading images (0/" + totalImages + ")");
        }

        // Set a timeout for the entire upload process
        Handler timeoutHandler = new Handler(Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            if (uploadedCount.get() + failedCount.get() < totalImages) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Upload timed out. Please try again.", Toast.LENGTH_LONG).show();
                });
                listener.onImagesUploaded(new ArrayList<>());
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, 30000); // 30 second timeout

        for (Uri imageUri : images) {
            try {
                // Compress image before upload
                Uri compressedUri = getCompressedImageUri(imageUri);
                
                String imageName = "products/" + userId + "/" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString();
                StorageReference imageRef = storageRef.child(imageName);

                // Create upload task with metadata and cache control
                StorageMetadata metadata = new StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .setCacheControl("public,max-age=31536000") // Cache for 1 year
                    .build();

                UploadTask uploadTask = imageRef.putFile(compressedUri, metadata);

                // Add progress listener
                uploadTask.addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    if (progressText != null) {
                        runOnUiThread(() -> {
                            int current = uploadedCount.get() + failedCount.get();
                            progressText.setText(String.format("Uploading images (%d/%d) - %.0f%%", 
                                current, totalImages, progress));
                        });
                    }
                });

                // Handle success
                uploadTask.continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Exception e = task.getException();
                        if (e != null && e.getMessage() != null && 
                            (e.getMessage().contains("App Check") || e.getMessage().contains("Too many attempts")) &&
                            retryCount.get() < MAX_RETRIES) {
                            // Retry on App Check errors
                            retryCount.incrementAndGet();
                            Log.d("ListNewItemActivity", "Retrying upload due to App Check error. Attempt: " + retryCount.get());
                            return Tasks.forResult(null);
                        }
                        throw e;
                    }
                    return imageRef.getDownloadUrl();
                }).addOnSuccessListener(uri -> {
                    if (uri != null) {
                        uploadedUrls.add(uri.toString());
                    }
                    int current = uploadedCount.incrementAndGet() + failedCount.get();
                    
                    if (current == totalImages) {
                        timeoutHandler.removeCallbacks(timeoutRunnable);
                        if (!uploadedUrls.isEmpty()) {
                            listener.onImagesUploaded(uploadedUrls);
                        } else {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(this, "Failed to upload any images. Please try again.", Toast.LENGTH_LONG).show();
                            });
                            listener.onImagesUploaded(new ArrayList<>());
                        }
                    }
                }).addOnFailureListener(e -> {
                    Log.e("ListNewItemActivity", "Upload failed: " + e.getMessage());
                    
                    // Handle App Check errors
                    if (e.getMessage() != null && 
                        (e.getMessage().contains("App Check") || e.getMessage().contains("Too many attempts")) &&
                        retryCount.get() < MAX_RETRIES) {
                        retryCount.incrementAndGet();
                        Log.d("ListNewItemActivity", "Retrying upload due to App Check error. Attempt: " + retryCount.get());
                        // Retry the upload
                        uploadImages(Collections.singletonList(imageUri), urls -> {
                            if (!urls.isEmpty()) {
                                uploadedUrls.addAll(urls);
                            }
                            int current = uploadedCount.incrementAndGet() + failedCount.get();
                            if (current == totalImages) {
                                timeoutHandler.removeCallbacks(timeoutRunnable);
                                listener.onImagesUploaded(uploadedUrls);
                            }
                        });
                        return;
                    }
                    
                    int current = failedCount.incrementAndGet() + uploadedCount.get();
                    
                    if (current == totalImages) {
                        timeoutHandler.removeCallbacks(timeoutRunnable);
                        if (!uploadedUrls.isEmpty()) {
                            // If we have at least some successful uploads, continue with those
                            listener.onImagesUploaded(uploadedUrls);
                        } else {
                            runOnUiThread(() -> {
                                progressDialog.dismiss();
                                String errorMessage = "Failed to upload images: ";
                                if (e.getMessage().contains("App Check")) {
                                    errorMessage += "Authentication error. Please restart the app and try again.";
                                } else {
                                    errorMessage += e.getMessage();
                                }
                                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                            });
                            listener.onImagesUploaded(new ArrayList<>());
                        }
                    }
                });

            } catch (Exception e) {
                Log.e("ListNewItemActivity", "Error preparing upload: " + e.getMessage());
                failedCount.incrementAndGet();
            }
        }
    }

    private void saveProductData(List<String> imageUrls) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String productId = FirebaseDatabase.getInstance().getReference("products").push().getKey();
        
        // Update progress message
        TextView progressText = progressDialog.findViewById(R.id.progressText);
        if (progressText != null) {
            progressText.setText("Saving product data...");
        }
        
        // Get category and split into main category and subcategory
        String fullCategory = categoryInput.getText().toString().trim();
        String[] categoryParts = fullCategory.split(" - ", 2);
        String mainCategory = categoryParts[0]; // "Women", "Men", or "Kids"
        String category = categoryParts.length > 1 ? categoryParts[1] : fullCategory; // "Tops", "Dresses", etc.
        
        // Extract unique colors and sizes from stock entries
        final Set<String> uniqueColors = new HashSet<>();
        final Set<String> uniqueSizes = new HashSet<>();
        for (StockEntry entry : stockEntries) {
            uniqueColors.add(entry.color);
            uniqueSizes.add(entry.size);
        }
        
        // Get seller name from Firestore
        db.collection("sellers").document(userId)
            .get()
            .addOnSuccessListener(document -> {
                String sellerName = document.getString("shopName");
                
                // Calculate total stock inside the lambda
                int totalStock = 0;
                for (StockEntry entry : stockEntries) {
                    totalStock += entry.stock;
                }
                
                Map<String, Object> productData = new HashMap<>();
                productData.put("productId", productId);
                productData.put("sellerId", userId);
                productData.put("sellerName", sellerName != null ? sellerName : "");
                productData.put("name", productNameInput.getText().toString().trim());
                productData.put("description", descriptionInput.getText().toString().trim());
                productData.put("price", Double.parseDouble(priceInput.getText().toString().trim()));
                productData.put("mainCategory", mainCategory);
                productData.put("category", category);
                productData.put("coverPhotoUri", imageUrls.get(0));
                productData.put("productImageUris", imageUrls.subList(1, imageUrls.size()));
                productData.put("colors", new ArrayList<>(uniqueColors));
                productData.put("sizes", new ArrayList<>(uniqueSizes));
                productData.put("stock", totalStock);
                productData.put("stockEntries", convertStockEntriesToList());
                productData.put("createdAt", System.currentTimeMillis());
                productData.put("isAvailable", true);
                productData.put("parcelSize", "1"); // Default parcel size
                productData.put("weight", 1); // Default weight
                
                // Save to Firestore only
                db.collection("products")
                    .document(productId)
                    .set(productData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("ListNewItemActivity", "Product saved successfully to Firestore");
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(this, "Product listed successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ListNewItemActivity", "Error saving product: " + e.getMessage());
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            String errorMessage = "Failed to save product: ";
                            if (e.getMessage().contains("App Check")) {
                                errorMessage += "Authentication error. Please restart the app and try again.";
                            } else {
                                errorMessage += e.getMessage();
                            }
                            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                        });
                    });
            })
            .addOnFailureListener(e -> {
                Log.e("ListNewItemActivity", "Error getting seller name: " + e.getMessage());
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to get seller information: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            });
    }
    
    private List<Map<String, Object>> convertStockEntriesToList() {
        List<Map<String, Object>> stockEntriesList = new ArrayList<>();
        for (StockEntry entry : stockEntries) {
            Map<String, Object> entryMap = new HashMap<>();
            entryMap.put("color", entry.color);
            entryMap.put("size", entry.size);
            entryMap.put("stock", entry.stock);
            stockEntriesList.add(entryMap);
        }
        return stockEntriesList;
    }
    
    private interface OnImagesUploadedListener {
        void onImagesUploaded(List<String> imageUrls);
    }

    private void loadCategories() {
        categoryList.clear();
        
        // Load categories from string resources
        String[] womenCategories = getResources().getStringArray(R.array.categories_women);
        String[] menCategories = getResources().getStringArray(R.array.categories_men);
        String[] kidsCategories = getResources().getStringArray(R.array.categories_kids);
        
        // Add categories with gender prefix
        for (String category : womenCategories) {
            categoryList.add("Women - " + category);
        }
        for (String category : menCategories) {
            categoryList.add("Men - " + category);
        }
        for (String category : kidsCategories) {
            categoryList.add("Kids - " + category);
        }
        
        // Sort categories alphabetically
        Collections.sort(categoryList);
        
        // Update UI on the main thread
        runOnUiThread(() -> {
            // Create and set adapter for category input with custom layout
            ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this,
                R.layout.item_dropdown,  // Custom layout for dropdown items
                categoryList
            );
            
            // Set the adapter
            categoryInput.setAdapter(categoryAdapter);
            
            // Configure the AutoCompleteTextView
            categoryInput.setThreshold(0); // Show all suggestions immediately
            categoryInput.setOnClickListener(v -> categoryInput.showDropDown());
            categoryInput.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    categoryInput.showDropDown();
                }
            });
            
            // Force the dropdown to show
            categoryInput.post(() -> {
                if (categoryInput.hasFocus()) {
                    categoryInput.showDropDown();
                }
            });
            
            // Log for debugging
            Log.d("ListNewItemActivity", "Categories loaded: " + categoryList.size());
            for (String category : categoryList) {
                Log.d("ListNewItemActivity", "Category: " + category);
            }
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
        private Set<Integer> selectedPositions = new HashSet<>();

        public StockEntryAdapter(ArrayList<StockEntry> entries) { 
            this.entries = entries; 
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stock_entry, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            StockEntry entry = entries.get(position);
            holder.txtColor.setText(entry.color);
            holder.txtSize.setText(entry.size);
            holder.txtStock.setText("Stock: " + entry.stock);
            holder.checkbox.setChecked(selectedPositions.contains(position));
            
            holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedPositions.add(position);
                } else {
                    selectedPositions.remove(position);
                }
                updateSelectedCount();
            });

            holder.btnDelete.setOnClickListener(v -> {
                entries.remove(position);
                selectedPositions.remove(position);
                // Update positions of remaining selected items
                Set<Integer> newSelectedPositions = new HashSet<>();
                for (int pos : selectedPositions) {
                    if (pos > position) {
                        newSelectedPositions.add(pos - 1);
                    } else if (pos < position) {
                        newSelectedPositions.add(pos);
                    }
                }
                selectedPositions.clear();
                selectedPositions.addAll(newSelectedPositions);
                notifyDataSetChanged();
            });
        }

        @Override
        public int getItemCount() { 
            return entries.size(); 
        }

        public Set<Integer> getSelectedPositions() {
            return selectedPositions;
        }

        public void deleteSelected() {
            // Sort positions in descending order to avoid index shifting
            List<Integer> positions = new ArrayList<>(selectedPositions);
            Collections.sort(positions, Collections.reverseOrder());
            
            for (int position : positions) {
                entries.remove(position);
            }
            selectedPositions.clear();
            notifyDataSetChanged();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtColor;
            TextView txtSize;
            TextView txtStock;
            CheckBox checkbox;
            ImageButton btnDelete;

            public ViewHolder(View itemView) {
                super(itemView);
                txtColor = itemView.findViewById(R.id.stockEntryColor);
                txtSize = itemView.findViewById(R.id.stockEntrySize);
                txtStock = itemView.findViewById(R.id.stockEntryQuantity);
                checkbox = itemView.findViewById(R.id.stockEntryCheckbox);
                btnDelete = itemView.findViewById(R.id.btnDeleteStockEntry);
            }
        }
    }

    private void setupStockManagement() {
        // Set up stock entry button click listener
        addStockButton.setOnClickListener(v -> {
            String color = colorInput.getText().toString().trim();
            String size = sizeInput.getText().toString().trim();
            String stockStr = stockInput.getText().toString().trim();
            
            if (TextUtils.isEmpty(color)) {
                Toast.makeText(this, "Please enter a color", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(size)) {
                Toast.makeText(this, "Please select a size", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(stockStr)) {
                Toast.makeText(this, "Please enter stock quantity", Toast.LENGTH_SHORT).show();
                return;
            }
            
            try {
                int stock = Integer.parseInt(stockStr);
                if (stock <= 0) {
                    Toast.makeText(this, "Stock quantity must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Add new stock entry
                StockEntry entry = new StockEntry(color, size, stock);
                stockEntries.add(entry);
                stockEntryAdapter.notifyItemInserted(stockEntries.size() - 1);
                
                // Update total stock
                totalStock += stock;
                totalStockText.setText("Total Stock: " + totalStock);
                
                // Clear inputs
                colorInput.setText("");
                sizeInput.setText("");
                stockInput.setText("");
                
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid stock number", Toast.LENGTH_SHORT).show();
            }
        });
    }
} 