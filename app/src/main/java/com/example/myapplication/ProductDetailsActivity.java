package com.example.myapplication;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import android.widget.ImageButton;
import android.util.Log;
import android.widget.LinearLayout;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Color;
import com.google.android.material.chip.Chip;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import androidx.core.content.ContextCompat;

public class ProductDetailsActivity extends AppCompatActivity {
    private ViewPager2 viewPagerImages;
    private ImageView imgProduct;
    private TextView txtProductName, txtProductPrice, txtGuarantee, txtRating, txtSold, txtSellerName;
    private Button btnBuyNow;
    private Button btnAddToCart;
    private ImageButton btnMessageSeller;
    private ImageButton btnViewCart;
    private Product product;
    private List<String> imageUrls = new ArrayList<>();
    private TextView txtImageCount;
    private FirebaseFirestore db;
    private LinearLayout layoutDetailColors;
    private TextView txtSelectedColor;
    private int selectedColorIndex = -1;
    private LinearLayout layoutDetailSizes;
    private TextView txtSelectedSize;
    private int selectedSizeIndex = -1;
    private List<ProductDetailsActivity.StockEntry> stockEntries = new ArrayList<>();
    private List<String> availableSizes = new ArrayList<>();
    private List<String> availableColors = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);

        viewPagerImages = findViewById(R.id.viewPagerImages);
        imgProduct = findViewById(R.id.imgProduct);
        txtProductName = findViewById(R.id.txtProductName);
        txtProductPrice = findViewById(R.id.txtProductPrice);
        btnBuyNow = findViewById(R.id.btnBuyNow);
        btnAddToCart = findViewById(R.id.btnAddToCart);
        btnMessageSeller = findViewById(R.id.btnMessageSeller);
        txtImageCount = findViewById(R.id.txtImageCount);
        txtGuarantee = findViewById(R.id.txtGuarantee);
        txtRating = findViewById(R.id.txtRating);
        txtSold = findViewById(R.id.txtSold);
        txtSellerName = findViewById(R.id.txtSellerName);
        layoutDetailColors = findViewById(R.id.layoutDetailColors);
        txtSelectedColor = findViewById(R.id.txtSelectedColor);
        layoutDetailSizes = findViewById(R.id.layoutDetailSizes);
        txtSelectedSize = findViewById(R.id.txtSelectedSize);
        btnViewCart = findViewById(R.id.btnViewCart);

        db = FirebaseFirestore.getInstance();

        String productId = getIntent().getStringExtra("productId");
        db.collection("products").document(productId)
            .get().addOnSuccessListener(doc -> {
                product = doc.toObject(Product.class);
                if (product != null) {
                    product.id = doc.getId();
                    txtProductName.setText(product.name != null ? product.name : "");
                    txtProductPrice.setText("₱" + product.price);
                    txtSellerName.setText(product.sellerName != null ? "by " + product.sellerName : "");
                    imageUrls.clear();
                    if (product.coverPhotoUri != null && !product.coverPhotoUri.isEmpty()) imageUrls.add(product.coverPhotoUri);
                    if (product.productImageUris != null) {
                        for (String url : product.productImageUris) {
                            if (product.coverPhotoUri == null || !url.equals(product.coverPhotoUri)) imageUrls.add(url);
                        }
                    }
                    if (imageUrls.size() > 1) {
                        viewPagerImages.setVisibility(View.VISIBLE);
                        imgProduct.setVisibility(View.GONE);
                        viewPagerImages.setAdapter(new ImagePagerAdapter(imageUrls));
                        txtImageCount.setVisibility(View.VISIBLE);
                        txtImageCount.setText("1/" + imageUrls.size());
                        viewPagerImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                            @Override
                            public void onPageSelected(int position) {
                                super.onPageSelected(position);
                                txtImageCount.setText((position + 1) + "/" + imageUrls.size());
                            }
                        });
                    } else if (imageUrls.size() == 1) {
                        viewPagerImages.setVisibility(View.GONE);
                        imgProduct.setVisibility(View.VISIBLE);
                        com.bumptech.glide.Glide.with(this).load(imageUrls.get(0)).into(imgProduct);
                        txtImageCount.setVisibility(View.GONE);
                    } else {
                        viewPagerImages.setVisibility(View.GONE);
                        imgProduct.setVisibility(View.VISIBLE);
                        imgProduct.setImageResource(R.drawable.placeholder_image);
                        txtImageCount.setVisibility(View.GONE);
                    }
                    // Guarantee/info
                    txtGuarantee.setText("Thrifty Guarantees 100% Orig");
                    // Rating and sold count (placeholder logic)
                    double rating = 5.0;
                    int soldCount = 300;
                    txtRating.setText("★ " + rating);
                    txtSold.setText(soldCount + " Sold");
                    // Load stockEntries if available and populate colors from there
                    if (doc.contains("stockEntries")) {
                        List<?> stockEntryList = (List<?>) doc.get("stockEntries");
                        stockEntries.clear();
                        Set<String> uniqueColors = new HashSet<>();
                        for (Object obj : stockEntryList) {
                            if (obj instanceof java.util.Map) {
                                java.util.Map map = (java.util.Map) obj;
                                String color = map.get("color") != null ? map.get("color").toString() : "";
                                String size = map.get("size") != null ? map.get("size").toString() : "";
                                int entryStock = map.get("stock") != null ? Integer.parseInt(map.get("stock").toString()) : 0;
                                stockEntries.add(new StockEntry(color, size, entryStock));
                                if (!color.isEmpty()) {
                                    uniqueColors.add(color.trim());
                                }
                            }
                        }
                        availableColors.clear();
                        if (product.colors != null && !product.colors.isEmpty()) {
                            availableColors.addAll(product.colors);
                        } else {
                            availableColors.addAll(uniqueColors);
                        }
                        showProductColors(availableColors); // Show all colors
                    } else {
                        // If no stock entries, show no colors available
                        stockEntries.clear();
                        availableColors.clear();
                        showProductColors(availableColors);
                    }
                    // Show sizes for the first color by default
                    if (product.sizes != null && !product.sizes.isEmpty()) {
                        showProductSizes(product.sizes);
                    }
                    txtSellerName.setOnClickListener(v -> {
                        if (product != null && product.sellerId != null && product.sellerName != null) {
                            Intent intent = new Intent(ProductDetailsActivity.this, LandingActivity.class);
                            intent.putExtra("openShopProducts", true);
                            intent.putExtra("sellerId", product.sellerId);
                            intent.putExtra("shopName", product.sellerName);
                            startActivity(intent);
                        }
                    });
                    txtSellerName.setTextColor(Color.parseColor("#8B2CF5"));
                    txtSellerName.setPaintFlags(txtSellerName.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                    txtSellerName.setClickable(true);

                    if (product.sellerId != null) {
                        FirebaseFirestore.getInstance().collection("sellers")
                            .document(product.sellerId)
                            .get()
                            .addOnSuccessListener(sellerDoc -> {
                                String shopName = sellerDoc.getString("shopName");
                                if (shopName != null && !shopName.isEmpty()) {
                                    txtSellerName.setText("by " + shopName);
                                } else {
                                    txtSellerName.setText("by Shop");
                                }
                            })
                            .addOnFailureListener(e -> txtSellerName.setText("by Shop"));
                    } else {
                        txtSellerName.setText("by Shop");
                    }
                } else {
                    Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to load product", Toast.LENGTH_SHORT).show();
                finish();
            });

        btnBuyNow.setOnClickListener(v -> {
            if (selectedColorIndex == -1 || selectedSizeIndex == -1) {
                Toast.makeText(this, "Please select both color and size", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Get selected color and size
            String selectedColor = null;
            String selectedSize = null;

            if (selectedColorIndex >= 0 && selectedColorIndex < availableColors.size()) {
                selectedColor = availableColors.get(selectedColorIndex);
            } else {
                Toast.makeText(this, "Error getting selected color. Please re-select.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedSizeIndex >= 0 && selectedSizeIndex < availableSizes.size()) {
                selectedSize = availableSizes.get(selectedSizeIndex);
            } else {
                Toast.makeText(this, "Error getting selected size. Please re-select.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Find matching stock entry
            StockEntry selectedEntry = null;
            for (StockEntry entry : stockEntries) {
                if (entry.color.equals(selectedColor) && entry.size.equals(selectedSize)) {
                    selectedEntry = entry;
                    break;
                }
            }
            
            if (selectedEntry != null && selectedEntry.stock > 0) {
                // Start BuyNowActivity with selected options
                Intent intent = new Intent(this, BuyNowActivity.class);
                intent.putExtra("productId", product.id);
                intent.putExtra("sellerId", product.sellerId);
                intent.putExtra("productName", product.name);
                intent.putExtra("productPrice", product.price);
                intent.putExtra("selectedColor", selectedColor);
                intent.putExtra("selectedSize", selectedSize);
                intent.putExtra("availableStock", selectedEntry.stock);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Selected combination is out of stock", Toast.LENGTH_SHORT).show();
            }
        });

        btnAddToCart.setOnClickListener(v -> {
            if (selectedColorIndex == -1 || selectedSizeIndex == -1) {
                Toast.makeText(this, "Please select both color and size", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Get selected color and size
            String selectedColor = null;
            String selectedSize = null;

            if (selectedColorIndex >= 0 && selectedColorIndex < availableColors.size()) {
                selectedColor = availableColors.get(selectedColorIndex);
            } else {
                Toast.makeText(this, "Error getting selected color. Please re-select.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedSizeIndex >= 0 && selectedSizeIndex < availableSizes.size()) {
                selectedSize = availableSizes.get(selectedSizeIndex);
            } else {
                Toast.makeText(this, "Error getting selected size. Please re-select.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Find matching stock entry
            StockEntry selectedEntry = null;
            for (StockEntry entry : stockEntries) {
                if (entry.color.equals(selectedColor) && entry.size.equals(selectedSize)) {
                    selectedEntry = entry;
                    break;
                }
            }
            
            if (selectedEntry != null && selectedEntry.stock > 0) {
                // Add to cart logic here
                addToCart(product.id, selectedColor, selectedSize);
            } else {
                Toast.makeText(this, "Selected combination is out of stock", Toast.LENGTH_SHORT).show();
            }
        });

        btnMessageSeller.setOnClickListener(v -> {
            if (product != null && product.sellerId != null) {
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                // Verify the seller exists before starting chat
                db.collection("sellers").document(product.sellerId)
                    .get()
                    .addOnSuccessListener(sellerDoc -> {
                        if (sellerDoc.exists()) {
                            Log.d("CHAT_DEBUG", "Starting chat - Customer: " + currentUserId + ", Seller: " + product.sellerId);
                            Intent intent = new Intent(this, ChatActivity.class);
                            intent.putExtra("otherUserId", product.sellerId);
                            intent.putExtra("productId", product.id);
                            intent.putExtra("isCustomerInitiator", true);
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "Seller account not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("CHAT_DEBUG", "Error finding seller", e);
                        Toast.makeText(this, "Failed to start chat", Toast.LENGTH_SHORT).show();
                    });
            } else {
                Toast.makeText(this, "Seller information not available", Toast.LENGTH_SHORT).show();
            }
        });

        btnViewCart.setOnClickListener(v -> {
            Intent intent = new Intent(ProductDetailsActivity.this, CartActivity.class);
            startActivity(intent);
        });

        // Setup color and size selection
        setupColorSelection();
        setupSizeSelection();
        setupBuyButtons();
    }

    private void showProductColors(List<String> colors) {
        layoutDetailColors.removeAllViews();
        txtSelectedColor.setVisibility(View.VISIBLE);
        
        if (colors == null || colors.isEmpty()) {
            txtSelectedColor.setText("Color: -");
            View dot = new View(this);
            int size = (int) (28 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(16, 0, 16, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.bg_color_dot);
            GradientDrawable bg = (GradientDrawable) dot.getBackground();
            bg.setColor(Color.LTGRAY);
            bg.setStroke(1, Color.parseColor("#888888"));
            layoutDetailColors.addView(dot);
            return;
        }

        if (availableColors.isEmpty()) {
            txtSelectedColor.setText("Color: Out of Stock");
            View dot = new View(this);
            int size = (int) (28 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(16, 0, 16, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.bg_color_dot);
            GradientDrawable bg = (GradientDrawable) dot.getBackground();
            bg.setColor(Color.LTGRAY);
            bg.setStroke(1, Color.parseColor("#888888"));
            layoutDetailColors.addView(dot);
            return;
        }

        // Set initial selected color if none selected
        if (selectedColorIndex == -1 || selectedColorIndex >= availableColors.size()) {
            selectedColorIndex = 0;
        }
        updateSelectedColorLabel(availableColors.get(selectedColorIndex));

        // Create color dots
        for (int i = 0; i < availableColors.size(); i++) {
            String colorName = availableColors.get(i);
            View dot = new View(this);
            int size = (int) (28 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(16, 0, 16, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.bg_color_dot);
            GradientDrawable bg = (GradientDrawable) dot.getBackground();
            bg.setColor(getColorFromName(colorName));
            
            // Set selection state
            if (i == selectedColorIndex) {
                bg.setStroke(4, Color.parseColor("#8B2CF5")); // Selected state
            } else {
                bg.setStroke(1, Color.parseColor("#888888")); // Unselected state
            }

            final int index = i;
            dot.setOnClickListener(v -> {
                selectedColorIndex = index;
                String selectedColor = colors.get(selectedColorIndex);
                Log.d("ColorSelectionDebug", "Color dot clicked. Selected color: " + selectedColor);
                updateSelectedColorLabel(selectedColor);
                updateAvailableSizesForColor(selectedColor);
                showProductColors(colors); // Refresh to update selection with the same list
            });

            layoutDetailColors.addView(dot);
        }

        // Update sizes for selected color
        if (product != null && product.sizes != null && !colors.isEmpty()) {
            updateAvailableSizesForColor(colors.get(selectedColorIndex));
        }
    }

    private void updateAvailableSizesForColor(String color) {
        availableSizes.clear();
        for (StockEntry entry : stockEntries) {
            if (entry.color.equals(color) && entry.stock > 0 && !availableSizes.contains(entry.size)) {
                availableSizes.add(entry.size);
            }
        }
        txtSelectedSize.setVisibility(View.VISIBLE); // Always show label
        if (!availableSizes.isEmpty()) {
            selectedSizeIndex = 0;
            showProductSizes(availableSizes);
        } else {
            layoutDetailSizes.removeAllViews();
            txtSelectedSize.setText("Size: -");
            TextView sizeView = new TextView(this);
            sizeView.setText("-");
            sizeView.setTextSize(15);
            sizeView.setPadding(32, 8, 32, 8);
            sizeView.setBackgroundResource(R.drawable.bg_size_selector);
            sizeView.setTextColor(Color.parseColor("#888888"));
            layoutDetailSizes.addView(sizeView);
        }
    }

    private void showProductSizes(List<String> sizes) {
        layoutDetailSizes.removeAllViews();
        if (sizes == null || sizes.isEmpty()) {
            txtSelectedSize.setVisibility(View.GONE);
            return;
        }
        txtSelectedSize.setVisibility(View.VISIBLE);
        if (selectedSizeIndex == -1) selectedSizeIndex = 0;
        updateSelectedSizeLabel(sizes.get(selectedSizeIndex));
        for (int i = 0; i < sizes.size(); i++) {
            String sizeName = sizes.get(i);
            TextView sizeView = new TextView(this);
            sizeView.setText(sizeName);
            sizeView.setTextSize(15);
            sizeView.setPadding(32, 8, 32, 8);
            sizeView.setBackgroundResource(R.drawable.bg_size_selector);
            if (i == selectedSizeIndex) {
                sizeView.setTextColor(Color.parseColor("#8B2CF5"));
            } else {
                sizeView.setTextColor(Color.parseColor("#444444"));
            }
            final int index = i;
            sizeView.setOnClickListener(v -> {
                selectedSizeIndex = index;
                updateSelectedSizeLabel(sizes.get(index));
                showProductSizes(sizes); // Refresh to update selection
            });
            layoutDetailSizes.addView(sizeView);
        }
    }

    private void updateSelectedColorLabel(String colorName) {
        txtSelectedColor.setText("Color: " + colorName);
    }

    private void updateSelectedSizeLabel(String sizeName) {
        txtSelectedSize.setText("Size: " + sizeName);
    }

    // StockEntry class for details
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

    private int getColorFromName(String colorName) {
        Log.d("ColorSelectionDebug", "getColorFromName called for color: " + colorName);
        switch (colorName.toLowerCase()) {
            case "red": return ContextCompat.getColor(this, R.color.product_red);
            case "blue": return ContextCompat.getColor(this, R.color.product_blue);
            case "green": return ContextCompat.getColor(this, R.color.product_green);
            case "black": return ContextCompat.getColor(this, R.color.product_black);
            case "white": return ContextCompat.getColor(this, R.color.product_white);
            case "yellow": return ContextCompat.getColor(this, R.color.product_yellow);
            case "purple":
            case "violet": return ContextCompat.getColor(this, R.color.product_purple);
            case "pink": return ContextCompat.getColor(this, R.color.product_pink);
            case "orange": return ContextCompat.getColor(this, R.color.product_orange);
            case "gray": return ContextCompat.getColor(this, R.color.product_gray);
            default: return ContextCompat.getColor(this, R.color.default_color);
        }
    }

    private void setupColorSelection() {
        layoutDetailColors = findViewById(R.id.layoutDetailColors);
        txtSelectedColor = findViewById(R.id.txtSelectedColor);
        
        // Get unique colors from stock entries
        Set<String> uniqueColors = new HashSet<>();
        for (StockEntry entry : stockEntries) {
            uniqueColors.add(entry.color);
        }
        
        // Create color chips
        for (String color : uniqueColors) {
            Chip colorChip = new Chip(this);
            colorChip.setText(color);
            colorChip.setCheckable(true);
            colorChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    // Uncheck other chips
                    for (int i = 0; i < layoutDetailColors.getChildCount(); i++) {
                        View child = layoutDetailColors.getChildAt(i);
                        if (child instanceof Chip && child != buttonView) {
                            ((Chip) child).setChecked(false);
                        }
                    }
                    txtSelectedColor.setText("Selected Color: " + color);
                    selectedColorIndex = layoutDetailColors.indexOfChild(buttonView);
                    updateAvailableSizes(color);
                } else {
                    txtSelectedColor.setText("Select a color");
                    selectedColorIndex = -1;
                    layoutDetailSizes.removeAllViews();
                }
            });
            layoutDetailColors.addView(colorChip);
        }
    }

    private void setupSizeSelection() {
        layoutDetailSizes = findViewById(R.id.layoutDetailSizes);
        txtSelectedSize = findViewById(R.id.txtSelectedSize);
    }

    private void updateAvailableSizes(String selectedColor) {
        layoutDetailSizes.removeAllViews();
        Set<String> availableSizes = new HashSet<>();
        
        // Get available sizes for selected color
        for (StockEntry entry : stockEntries) {
            if (entry.color.equals(selectedColor) && entry.stock > 0) {
                availableSizes.add(entry.size);
            }
        }
        
        // Create size chips
        for (String size : availableSizes) {
            Chip sizeChip = new Chip(this);
            sizeChip.setText(size);
            sizeChip.setCheckable(true);
            sizeChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    // Uncheck other chips
                    for (int i = 0; i < layoutDetailSizes.getChildCount(); i++) {
                        View child = layoutDetailSizes.getChildAt(i);
                        if (child instanceof Chip && child != buttonView) {
                            ((Chip) child).setChecked(false);
                        }
                    }
                    txtSelectedSize.setText("Selected Size: " + size);
                    selectedSizeIndex = layoutDetailSizes.indexOfChild(buttonView);
                    updateBuyButtonState();
                } else {
                    txtSelectedSize.setText("Select a size");
                    selectedSizeIndex = -1;
                    updateBuyButtonState();
                }
            });
            layoutDetailSizes.addView(sizeChip);
        }
    }

    private void updateBuyButtonState() {
        boolean canBuy = selectedColorIndex != -1 && selectedSizeIndex != -1;
        btnBuyNow.setEnabled(canBuy);
        btnAddToCart.setEnabled(canBuy);
    }

    private void setupBuyButtons() {
        btnBuyNow = findViewById(R.id.btnBuyNow);
        btnAddToCart = findViewById(R.id.btnAddToCart);
        
        btnBuyNow.setOnClickListener(v -> {
            if (selectedColorIndex == -1 || selectedSizeIndex == -1) {
                Toast.makeText(this, "Please select both color and size", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Get selected color and size
            String selectedColor = null;
            String selectedSize = null;

            if (selectedColorIndex >= 0 && selectedColorIndex < availableColors.size()) {
                selectedColor = availableColors.get(selectedColorIndex);
            } else {
                Toast.makeText(this, "Error getting selected color. Please re-select.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedSizeIndex >= 0 && selectedSizeIndex < availableSizes.size()) {
                selectedSize = availableSizes.get(selectedSizeIndex);
            } else {
                Toast.makeText(this, "Error getting selected size. Please re-select.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Find matching stock entry
            StockEntry selectedEntry = null;
            for (StockEntry entry : stockEntries) {
                if (entry.color.equals(selectedColor) && entry.size.equals(selectedSize)) {
                    selectedEntry = entry;
                    break;
                }
            }
            
            if (selectedEntry != null && selectedEntry.stock > 0) {
                // Start BuyNowActivity with selected options
                Intent intent = new Intent(this, BuyNowActivity.class);
                intent.putExtra("productId", product.id);
                intent.putExtra("sellerId", product.sellerId);
                intent.putExtra("productName", product.name);
                intent.putExtra("productPrice", product.price);
                intent.putExtra("selectedColor", selectedColor);
                intent.putExtra("selectedSize", selectedSize);
                intent.putExtra("availableStock", selectedEntry.stock);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Selected combination is out of stock", Toast.LENGTH_SHORT).show();
            }
        });
        
        btnAddToCart.setOnClickListener(v -> {
            if (selectedColorIndex == -1 || selectedSizeIndex == -1) {
                Toast.makeText(this, "Please select both color and size", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Get selected color and size
            String selectedColor = null;
            String selectedSize = null;

            if (selectedColorIndex >= 0 && selectedColorIndex < availableColors.size()) {
                selectedColor = availableColors.get(selectedColorIndex);
            } else {
                Toast.makeText(this, "Error getting selected color. Please re-select.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedSizeIndex >= 0 && selectedSizeIndex < availableSizes.size()) {
                selectedSize = availableSizes.get(selectedSizeIndex);
            } else {
                Toast.makeText(this, "Error getting selected size. Please re-select.", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Find matching stock entry
            StockEntry selectedEntry = null;
            for (StockEntry entry : stockEntries) {
                if (entry.color.equals(selectedColor) && entry.size.equals(selectedSize)) {
                    selectedEntry = entry;
                    break;
                }
            }
            
            if (selectedEntry != null && selectedEntry.stock > 0) {
                // Add to cart logic here
                addToCart(product.id, selectedColor, selectedSize);
            } else {
                Toast.makeText(this, "Selected combination is out of stock", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addToCart(String productId, String color, String size) {
        if (product == null || product.sellerId == null || product.sellerId.isEmpty()) {
            Toast.makeText(this, "Seller information missing. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("carts").document(userId)
            .collection("items")
            .whereEqualTo("productId", productId)
            .whereEqualTo("color", color)
            .whereEqualTo("size", size)
            .get()
            .addOnSuccessListener(query -> {
                if (!query.isEmpty()) {
                    // Item exists, update quantity
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : query) {
                        Long currentQty = doc.getLong("quantity");
                        int newQty = (currentQty != null ? currentQty.intValue() : 1) + 1;
                        doc.getReference().update("quantity", newQty);
                    }
                    Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show();
                } else {
                    // Add new item
                    Map<String, Object> cartItem = new HashMap<>();
                    cartItem.put("productId", productId);
                    cartItem.put("sellerId", product.sellerId);
                    cartItem.put("name", product.name != null ? product.name : "");
                    cartItem.put("imageUrl", product.coverPhotoUri != null ? product.coverPhotoUri : "");
                    cartItem.put("color", color);
                    cartItem.put("size", size);
                    cartItem.put("price", product.price);
                    cartItem.put("quantity", 1);
                    cartItem.put("timestamp", System.currentTimeMillis());
                    db.collection("carts").document(userId)
                        .collection("items")
                        .add(cartItem)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(this, "Added to cart", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to add to cart: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to add to cart: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
}