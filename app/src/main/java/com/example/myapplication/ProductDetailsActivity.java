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

public class ProductDetailsActivity extends AppCompatActivity {
    private ViewPager2 viewPagerImages;
    private ImageView imgProduct;
    private TextView txtProductName, txtProductPrice, txtGuarantee, txtRating, txtSold, txtSellerName;
    private Button btnBuyNow;
    private Button btnAddToCart;
    private ImageButton btnMessageSeller;
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
                    showProductColors(product.colors);
                    // Show sizes for the first color by default
                    if (product.sizes != null && !product.sizes.isEmpty()) {
                        showProductSizes(product.sizes);
                    }
                    // Load stockEntries if available
                    if (doc.contains("stockEntries")) {
                        List<?> stockEntryList = (List<?>) doc.get("stockEntries");
                        stockEntries.clear();
                        for (Object obj : stockEntryList) {
                            if (obj instanceof java.util.Map) {
                                java.util.Map map = (java.util.Map) obj;
                                String color = map.get("color") != null ? map.get("color").toString() : "";
                                String size = map.get("size") != null ? map.get("size").toString() : "";
                                int entryStock = map.get("stock") != null ? Integer.parseInt(map.get("stock").toString()) : 0;
                                stockEntries.add(new StockEntry(color, size, entryStock));
                            }
                        }
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
            if (product != null) {
                Intent intent = new Intent(ProductDetailsActivity.this, BuyNowActivity.class);
                intent.putExtra("productId", product.id);
                intent.putExtra("sellerId", product.sellerId);
                intent.putExtra("productName", product.name);
                intent.putExtra("productPrice", product.price);
                intent.putExtra("productStock", product.stock != null ? product.stock : 0);
                intent.putExtra("quantity", 1); // default quantity
                // Add selected color and size
                if (selectedColorIndex >= 0 && product.colors != null && selectedColorIndex < product.colors.size()) {
                    intent.putExtra("color", product.colors.get(selectedColorIndex));
                }
                if (selectedSizeIndex >= 0 && product.sizes != null && selectedSizeIndex < product.sizes.size()) {
                    intent.putExtra("size", product.sizes.get(selectedSizeIndex));
                }
                startActivity(intent);
            }
        });

        btnAddToCart.setOnClickListener(v -> {
            if (product != null) {
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                java.util.Map<String, Object> cartItem = new java.util.HashMap<>();
                cartItem.put("productId", product.id);
                cartItem.put("name", product.name);
                cartItem.put("price", product.price);
                cartItem.put("image", product.coverPhotoUri);
                cartItem.put("quantity", 1); // default quantity
                cartItem.put("sellerId", product.sellerId);
                cartItem.put("sellerName", product.sellerName);
                cartItem.put("stock", product.stock);
                // Add selected color and size
                if (selectedColorIndex >= 0 && product.colors != null && selectedColorIndex < product.colors.size()) {
                    cartItem.put("color", product.colors.get(selectedColorIndex));
                }
                if (selectedSizeIndex >= 0 && product.sizes != null && selectedSizeIndex < product.sizes.size()) {
                    cartItem.put("size", product.sizes.get(selectedSizeIndex));
                }
                db
                    .collection("users")
                    .document(userId)
                    .collection("cart")
                    .document(product.id)
                    .set(cartItem)
                    .addOnSuccessListener(unused -> Toast.makeText(this, "Added to cart!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to add to cart", Toast.LENGTH_SHORT).show());
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
    }

    private void showProductColors(List<String> colors) {
        layoutDetailColors.removeAllViews();
        txtSelectedColor.setVisibility(View.VISIBLE); // Always show label
        if (colors == null || colors.isEmpty()) {
            // Show disabled color UI or message
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
            // Show size label and disabled size UI
            txtSelectedSize.setVisibility(View.VISIBLE);
            txtSelectedSize.setText("Size: -");
            layoutDetailSizes.removeAllViews();
            TextView sizeView = new TextView(this);
            sizeView.setText("-");
            sizeView.setTextSize(15);
            sizeView.setPadding(32, 8, 32, 8);
            sizeView.setBackgroundResource(R.drawable.bg_size_selector);
            sizeView.setTextColor(Color.parseColor("#888888"));
            layoutDetailSizes.addView(sizeView);
            return;
        }
        // Only show colors that have at least one size with stock > 0
        List<String> filteredColors = new ArrayList<>();
        for (String color : colors) {
            boolean hasStock = false;
            for (StockEntry entry : stockEntries) {
                if (entry.color.equals(color) && entry.stock > 0) {
                    hasStock = true;
                    break;
                }
            }
            if (hasStock) filteredColors.add(color);
        }
        if (filteredColors.isEmpty()) {
            // Show disabled color UI or message
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
            // Show size label and disabled size UI
            txtSelectedSize.setVisibility(View.VISIBLE);
            txtSelectedSize.setText("Size: -");
            layoutDetailSizes.removeAllViews();
            TextView sizeView = new TextView(this);
            sizeView.setText("-");
            sizeView.setTextSize(15);
            sizeView.setPadding(32, 8, 32, 8);
            sizeView.setBackgroundResource(R.drawable.bg_size_selector);
            sizeView.setTextColor(Color.parseColor("#888888"));
            layoutDetailSizes.addView(sizeView);
            return;
        }
        if (selectedColorIndex == -1 || selectedColorIndex >= filteredColors.size()) selectedColorIndex = 0;
        updateSelectedColorLabel(filteredColors.get(selectedColorIndex));
        for (int i = 0; i < filteredColors.size(); i++) {
            String colorName = filteredColors.get(i);
            View dot = new View(this);
            int size = (int) (28 * getResources().getDisplayMetrics().density); // 28dp for detail page
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(16, 0, 16, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.bg_color_dot);
            GradientDrawable bg = (GradientDrawable) dot.getBackground();
            bg.setColor(getColorFromName(colorName));
            if (i == selectedColorIndex) {
                bg.setStroke(4, Color.parseColor("#8B2CF5")); // Highlight selected
            } else {
                bg.setStroke(1, Color.parseColor("#888888"));
            }
            final int index = i;
            dot.setOnClickListener(v -> {
                selectedColorIndex = index;
                updateSelectedColorLabel(filteredColors.get(index));
                updateAvailableSizesForColor(filteredColors.get(index));
                showProductColors(filteredColors); // Refresh to update selection
            });
            layoutDetailColors.addView(dot);
        }
        // Show sizes for selected color
        if (product != null && product.sizes != null) {
            updateAvailableSizesForColor(filteredColors.get(selectedColorIndex));
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
        if (colorName == null) return Color.LTGRAY;
        colorName = colorName.trim();
        if (colorName.isEmpty()) return Color.LTGRAY;
        // Try to parse as hex code
        if (colorName.startsWith("#") && (colorName.length() == 7 || colorName.length() == 9)) {
            try {
                // Only allow valid hex codes
                if (colorName.matches("#[A-Fa-f0-9]{6}") || colorName.matches("#[A-Fa-f0-9]{8}")) {
                    return Color.parseColor(colorName);
                }
            } catch (Exception e) {
                return Color.LTGRAY;
            }
        }
        // Fallback to name mapping
        switch (colorName.toLowerCase()) {
            case "red": return Color.RED;
            case "blue": return Color.BLUE;
            case "green": return Color.GREEN;
            case "black": return Color.BLACK;
            case "white": return Color.WHITE;
            case "yellow": return Color.YELLOW;
            case "orange": return 0xFFFFA500;
            case "purple": return 0xFF800080;
            case "pink": return 0xFFFFC0CB;
            case "brown": return 0xFFA52A2A;
            case "gray": return Color.GRAY;
            default: return Color.LTGRAY;
        }
    }
}