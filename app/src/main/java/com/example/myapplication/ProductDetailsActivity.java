package com.example.myapplication;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import java.util.ArrayList;
import java.util.List;
import android.widget.ImageButton;
import android.util.Log;
import android.widget.LinearLayout;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Color;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.example.myapplication.databinding.ItemVariationGridBinding;
import com.example.myapplication.databinding.LayoutVariationBottomSheetBinding;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import androidx.core.content.ContextCompat;

public class ProductDetailsActivity extends AppCompatActivity {
    private ViewPager2 viewPagerImages;
    private ImageView imgProduct;
    private TextView txtProductName, txtProductPrice, txtSellerName;
    private Button btnAddToCart, btnBuyNow;
    private ImageButton btnMessageSeller, btnMessageSellerBottom;
    private ImageButton btnViewCart;
    private Product product;
    private List<String> imageUrls = new ArrayList<>();
    private TextView txtImageCount;
    private LinearLayout layoutDetailColors;
    private TextView txtSelectedColor;
    private int selectedColorIndex = -1;
    private LinearLayout layoutDetailSizes;
    private TextView txtSelectedSize;
    private int selectedSizeIndex = -1;
    private Map<String, String> variationImages = new HashMap<>();
    private List<ProductDetailsActivity.StockEntry> stockEntries = new ArrayList<>();
    private List<String> availableSizes = new ArrayList<>();
    private List<String> availableColors = new ArrayList<>();
    private int currentQuantity = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);

        viewPagerImages = findViewById(R.id.viewPagerImages);
        imgProduct = findViewById(R.id.imgProduct);
        txtProductName = findViewById(R.id.txtProductName);
        txtProductPrice = findViewById(R.id.txtProductPrice);
        btnMessageSeller = findViewById(R.id.btnMessageSeller);
        btnMessageSellerBottom = findViewById(R.id.btnMessageSellerBottom);
        txtImageCount = findViewById(R.id.txtImageCount);
        txtSellerName = findViewById(R.id.txtSellerName);
        layoutDetailColors = findViewById(R.id.layoutDetailColors);
        txtSelectedColor = findViewById(R.id.txtSelectedColor);
        layoutDetailSizes = findViewById(R.id.layoutDetailSizes);
        txtSelectedSize = findViewById(R.id.txtSelectedSize);
        btnViewCart = findViewById(R.id.btnViewCart);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSearch).setOnClickListener(v -> Toast.makeText(this, "Search clicked", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnShare).setOnClickListener(v -> Toast.makeText(this, "Share clicked", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnFavorite).setOnClickListener(v -> Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show());

        String productId = getIntent().getStringExtra("productId");
        if (productId == null) {
            Toast.makeText(this, "Product ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        SupabaseManager.getProduct(productId, new SupabaseManager.SupabaseCallbackWithProduct() {
            @Override
            public void onResult(boolean success, SupabaseManager.ProductModel sp, String error) {
                if (success && sp != null) {
                    product = new Product();
                    product.id = sp.getId();
                    product.name = sp.getName();
                    product.price = sp.getPrice();
                    product.coverPhotoUri = sp.getCover_photo_url();
                    product.productImageUris = sp.get_product_images();
                    product.sellerId = sp.get_seller_id();
                    product.description = sp.get_description();
                    product.stock = (long) sp.get_stock();
                    product.colors = sp.get_colors();
                    product.sizes = sp.get_sizes();
                    product.variationImages = sp.get_variation_images();
                    product.mainCategory = sp.get_main_category();
                    product.category = sp.get_category();
                    product.isAvailable = sp.is_available_val();

                    // Parse createdAt
                    if (sp.get_created_at() != null) {
                        try {
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
                            java.util.Date date = sdf.parse(sp.get_created_at());
                            if (date != null) product.createdAt = date.getTime();
                        } catch (Exception e) {
                            android.util.Log.e("ProductDetails", "Error parsing date: " + sp.get_created_at());
                        }
                    }
                    
                    txtProductName.setText(product.name != null ? product.name : "");
                    txtProductPrice.setText("₱" + product.price);
                    
                    // Handle image URLs with proper prefixing
                    imageUrls.clear();
                    
                    // Add cover photo first
                    if (product.coverPhotoUri != null && !product.coverPhotoUri.isEmpty()) {
                        imageUrls.add(getAbsoluteUrl(product.coverPhotoUri));
                    }
                    
                    // Add additional photos
                    if (product.productImageUris != null) {
                        for (String uri : product.productImageUris) {
                            if (uri != null && !uri.isEmpty()) {
                                String absUrl = getAbsoluteUrl(uri);
                                if (!imageUrls.contains(absUrl)) {
                                    imageUrls.add(absUrl);
                                }
                            }
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
                        com.bumptech.glide.Glide.with(ProductDetailsActivity.this).load(imageUrls.get(0)).into(imgProduct);
                        txtImageCount.setVisibility(View.GONE);
                    } else {
                        viewPagerImages.setVisibility(View.GONE);
                        imgProduct.setVisibility(View.VISIBLE);
                        imgProduct.setImageResource(R.drawable.placeholder_image);
                        txtImageCount.setVisibility(View.GONE);
                    }

                    txtSellerName.setOnClickListener(v -> {
                        if (product != null && product.sellerId != null) {
                            Intent intent = new Intent(ProductDetailsActivity.this, LandingActivity.class);
                            intent.putExtra("openShopProducts", true);
                            intent.putExtra("sellerId", product.sellerId);
                            startActivity(intent);
                        }
                    });
                    txtSellerName.setTextColor(ContextCompat.getColor(ProductDetailsActivity.this, R.color.colorAccent));
                    txtSellerName.setPaintFlags(txtSellerName.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                    txtSellerName.setClickable(true);

                    if (product.sellerId != null) {
                        SupabaseManager.getUserProfile(product.sellerId, new SupabaseManager.SupabaseCallbackWithProfile() {
                            @Override
                            public void onResult(boolean success, SupabaseManager.Profile profile, String error) {
                                if (success && profile != null) {
                                    txtSellerName.setText("by " + (profile.getShop_name() != null ? profile.getShop_name() : "Shop"));
                                } else {
                                    txtSellerName.setText("by Shop");
                                }
                            }
                        });
                    } else {
                        txtSellerName.setText("by Shop");
                    }

                    stockEntries.clear();
                    
                    // Trigger variation setup after product data is loaded
                    setupColorSelection();
                    setupSizeSelection();

                    setupBuyButtons();
                } else {
                    Toast.makeText(ProductDetailsActivity.this, "Error loading product: " + error, Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });


        if (btnMessageSellerBottom != null) {
            btnMessageSellerBottom.setOnClickListener(v -> messageSeller());
        }

        btnMessageSeller.setOnClickListener(v -> messageSeller());

        btnViewCart.setOnClickListener(v -> {
            Intent intent = new Intent(ProductDetailsActivity.this, CartActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.layoutVariationPreview).setOnClickListener(v -> showVariationBottomSheet());

        // Setup initial button state
        updateBuyButtonState();

        // Update preview row
        updatePreviewImages();
    }

    private void updatePreviewImages() {
        LinearLayout previewLayout = findViewById(R.id.layoutPreviewImages);
        if (previewLayout == null || product == null || product.colors == null) return;
        previewLayout.removeAllViews();
        
        int count = Math.min(product.colors.size(), 4);
        for (int i = 0; i < count; i++) {
            String color = product.colors.get(i);
            ImageView iv = new ImageView(this);
            int size = (int) (24 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(0, 0, 4, 0);
            iv.setLayoutParams(lp);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            
            String imgUrl = (product.variationImages != null) ? product.variationImages.get(color) : null;
            if (imgUrl == null && !imageUrls.isEmpty()) imgUrl = imageUrls.get(0);
            
            Glide.with(this).load(imgUrl).placeholder(R.drawable.placeholder_image).into(iv);
            previewLayout.addView(iv);
        }
        
        TextView summary = findViewById(R.id.txtVariationSummary);
        if (summary != null) {
            summary.setText(product.colors.size() + " options available");
        }
    }

    private void showProductColors(List<String> colors) {
        layoutDetailColors.removeAllViews();
        txtSelectedColor.setVisibility(View.VISIBLE);
        
        if (colors == null || colors.isEmpty()) {
            txtSelectedColor.setText("Color: -");
            return;
        }

        // Set initial selected color if none selected
        if (selectedColorIndex == -1 || selectedColorIndex >= colors.size()) {
            selectedColorIndex = 0;
        }
        updateSelectedColorLabel(colors.get(selectedColorIndex));

        // Create color dots
        for (int i = 0; i < colors.size(); i++) {
            String colorName = colors.get(i);
            View dot = new View(this);
            int size = (int) (32 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(0, 0, 16, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.bg_color_dot);
            GradientDrawable bg = (GradientDrawable) dot.getBackground();
            bg.setColor(getColorFromName(colorName));
            
            // Set selection state (thicker stroke for selection)
            if (i == selectedColorIndex) {
                bg.setStroke(4, Color.BLACK); 
            } else {
                bg.setStroke(1, Color.LTGRAY);
            }

            final int index = i;
            dot.setOnClickListener(v -> {
                selectedColorIndex = index;
                String selectedColor = colors.get(selectedColorIndex);
                updateSelectedColorLabel(selectedColor);
                updateAvailableSizesForColor(selectedColor);
                showProductColors(colors); 
            });

            layoutDetailColors.addView(dot);
        }

        // Update sizes for selected color
        if (product != null && !colors.isEmpty()) {
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
            sizeView.setTextSize(13);
            sizeView.setPadding(48, 16, 48, 16);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 12, 0);
            sizeView.setLayoutParams(params);
            sizeView.setGravity(android.view.Gravity.CENTER);
            
            if (i == selectedSizeIndex) {
                sizeView.setBackgroundResource(R.drawable.bg_size_item_selected_v2);
                sizeView.setTextColor(Color.WHITE);
            } else {
                sizeView.setBackgroundResource(R.drawable.bg_size_item_v2);
                sizeView.setTextColor(Color.BLACK);
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

    private void showVariationBottomSheet() {
        if (product == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        LayoutVariationBottomSheetBinding sheetBinding = LayoutVariationBottomSheetBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheetBinding.getRoot());

        // Setup Summary
        sheetBinding.tvSheetPrice.setText("₱" + product.price);
        sheetBinding.tvSheetStock.setText("Stock: " + product.stock);
        updateSheetSelectedText(sheetBinding);
        
        String currentImg = imageUrls.isEmpty() ? null : imageUrls.get(0);
        if (selectedColorIndex != -1 && product.colors != null) {
            String color = product.colors.get(selectedColorIndex);
            if (product.variationImages != null && product.variationImages.containsKey(color)) {
                currentImg = product.variationImages.get(color);
            }
        }
        Glide.with(this).load(currentImg).placeholder(R.drawable.placeholder_image).into(sheetBinding.ivSheetProductImage);

        // Setup Colors
        if (product.colors != null && !product.colors.isEmpty()) {
            populateFlexbox(sheetBinding.flexSheetColors, product.colors, product.variationImages, selectedColorIndex, (index, name) -> {
                selectedColorIndex = index;
                updateSheetSelectedText(sheetBinding);
                if (product.variationImages != null && product.variationImages.containsKey(name)) {
                    Glide.with(this).load(product.variationImages.get(name)).into(sheetBinding.ivSheetProductImage);
                }
            });
        } else {
            sheetBinding.tvSheetColorLabel.setVisibility(View.GONE);
            sheetBinding.flexSheetColors.setVisibility(View.GONE);
        }

        // Setup Sizes
        if (product.sizes != null && !product.sizes.isEmpty()) {
            populateFlexbox(sheetBinding.flexSheetSizes, product.sizes, null, selectedSizeIndex, (index, name) -> {
                selectedSizeIndex = index;
                updateSheetSelectedText(sheetBinding);
            });
        } else {
            sheetBinding.tvSheetSizeLabel.setVisibility(View.GONE);
            sheetBinding.flexSheetSizes.setVisibility(View.GONE);
        }

        // Quantity
        sheetBinding.tvQuantity.setText(String.valueOf(currentQuantity));
        sheetBinding.btnQtyPlus.setOnClickListener(v -> {
            currentQuantity++;
            sheetBinding.tvQuantity.setText(String.valueOf(currentQuantity));
        });
        sheetBinding.btnQtyMinus.setOnClickListener(v -> {
            if (currentQuantity > 1) {
                currentQuantity--;
                sheetBinding.tvQuantity.setText(String.valueOf(currentQuantity));
            }
        });

        // Actions
        sheetBinding.btnSheetAddToCart.setOnClickListener(v -> {
            if (validateSelection()) {
                addToCart(product.id, getSelectedColor(), getSelectedSize());
                dialog.dismiss();
            }
        });

        sheetBinding.btnSheetBuyNow.setOnClickListener(v -> {
            if (validateSelection()) {
                startBuyNow();
                dialog.dismiss();
            }
        });

        sheetBinding.btnCloseSheet.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void populateFlexbox(ViewGroup flexbox, List<String> items, Map<String, String> images, int selectedIdx, OnSelectListener listener) {
        flexbox.removeAllViews();
        for (int i = 0; i < items.size(); i++) {
            final int index = i;
            String name = items.get(i);
            View bubbleView = getLayoutInflater().inflate(R.layout.item_variation_bubble, flexbox, false);
            com.google.android.material.card.MaterialCardView card = bubbleView.findViewById(R.id.cardVariationBubble);
            ImageView ivImage = bubbleView.findViewById(R.id.ivVariationBubbleImage);
            TextView tvName = bubbleView.findViewById(R.id.tvVariationBubbleName);

            tvName.setText(name);

            // Handle image if present (usually for colors)
            if (images != null && images.containsKey(name)) {
                ivImage.setVisibility(View.VISIBLE);
                Glide.with(this).load(images.get(name)).into(ivImage);
            } else {
                ivImage.setVisibility(View.GONE);
            }

            // Selection state
            if (i == selectedIdx) {
                card.setStrokeColor(ContextCompat.getColor(this, R.color.colorAccent));
                card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.wizard_accent_tint));
            } else {
                card.setStrokeColor(ContextCompat.getColor(this, R.color.gray_200));
                card.setCardBackgroundColor(Color.WHITE);
            }

            card.setOnClickListener(v -> {
                listener.onSelect(index, name);
                // Refresh all bubbles in this flexbox to update selection UI
                for (int j = 0; j < flexbox.getChildCount(); j++) {
                    View child = flexbox.getChildAt(j);
                    com.google.android.material.card.MaterialCardView c = child.findViewById(R.id.cardVariationBubble);
                    if (j == index) {
                        c.setStrokeColor(ContextCompat.getColor(this, R.color.colorAccent));
                        c.setCardBackgroundColor(ContextCompat.getColor(this, R.color.wizard_accent_tint));
                    } else {
                        c.setStrokeColor(ContextCompat.getColor(this, R.color.gray_200));
                        c.setCardBackgroundColor(Color.WHITE);
                    }
                }
            });

            flexbox.addView(bubbleView);
        }
    }

    private void updateSheetSelectedText(LayoutVariationBottomSheetBinding binding) {
        String color = selectedColorIndex != -1 ? product.colors.get(selectedColorIndex) : "None";
        String size = selectedSizeIndex != -1 ? product.sizes.get(selectedSizeIndex) : "None";
        binding.tvSheetSelectedVariation.setText("Selected: " + color + ", " + size);
        
        // Update main preview too
        TextView previewText = findViewById(R.id.txtVariationSummary);
        previewText.setText(color + ", " + size);
        updateBuyButtonState();
    }

    private boolean validateSelection() {
        boolean hasColors = product.colors != null && !product.colors.isEmpty();
        boolean hasSizes = product.sizes != null && !product.sizes.isEmpty();
        
        if (hasColors && selectedColorIndex == -1) {
            Toast.makeText(this, "Please select a color", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (hasSizes && selectedSizeIndex == -1) {
            Toast.makeText(this, "Please select a size", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private String getSelectedColor() {
        return (selectedColorIndex != -1 && product.colors != null) ? product.colors.get(selectedColorIndex) : null;
    }

    private String getSelectedSize() {
        return (selectedSizeIndex != -1 && product.sizes != null) ? product.sizes.get(selectedSizeIndex) : null;
    }

    private void startBuyNow() {
        Intent intent = new Intent(this, BuyNowActivity.class);
        intent.putExtra("productId", product.id);
        intent.putExtra("sellerId", product.sellerId);
        intent.putExtra("productName", product.name);
        intent.putExtra("productPrice", product.price);
        intent.putExtra("selectedColor", getSelectedColor());
        intent.putExtra("selectedSize", getSelectedSize());
        intent.putExtra("quantity", currentQuantity);
        startActivity(intent);
    }

    private interface OnSelectListener { void onSelect(int index, String name); }

    // Adapter for the grid in bottom sheet
    private class VariationGridAdapter extends RecyclerView.Adapter<VariationGridAdapter.VH> {
        private List<String> items;
        private Map<String, String> images;
        private int selectedIdx;
        private OnSelectListener listener;

        VariationGridAdapter(List<String> items, Map<String, String> images, int selectedIdx, OnSelectListener listener) {
            this.items = items;
            this.images = images;
            this.selectedIdx = selectedIdx;
            this.listener = listener;
        }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(ItemVariationGridBinding.inflate(getLayoutInflater(), p, false));
        }

        @Override public void onBindViewHolder(@NonNull VH h, int p) {
            String name = items.get(p);
            h.b.tvVariationName.setText(name);
            
            if (images != null && images.containsKey(name)) {
                Glide.with(ProductDetailsActivity.this).load(images.get(name)).into(h.b.ivVariationImage);
            } else {
                h.b.ivVariationImage.setVisibility(View.GONE);
            }

            if (p == selectedIdx) {
                h.b.cardVariation.setStrokeColor(ColorStateList.valueOf(getResources().getColor(R.color.colorAccent)));
                h.b.cardVariation.setCardBackgroundColor(getResources().getColor(R.color.wizard_accent_tint));
            } else {
                h.b.cardVariation.setStrokeColor(ColorStateList.valueOf(getResources().getColor(R.color.gray_200)));
                h.b.cardVariation.setCardBackgroundColor(Color.WHITE);
            }

            h.itemView.setOnClickListener(v -> {
                int oldIdx = selectedIdx;
                selectedIdx = p;
                notifyItemChanged(oldIdx);
                notifyItemChanged(selectedIdx);
                listener.onSelect(p, name);
            });
        }

        @Override public int getItemCount() { return items.size(); }
        class VH extends RecyclerView.ViewHolder {
            ItemVariationGridBinding b;
            VH(ItemVariationGridBinding b) { super(b.getRoot()); this.b = b; }
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
        
        if (product == null || product.colors == null || product.colors.isEmpty()) {
            txtSelectedColor.setVisibility(View.GONE);
            layoutDetailColors.setVisibility(View.GONE);
            return;
        }

        layoutDetailColors.removeAllViews();
        for (String color : product.colors) {
            View bubbleView = getLayoutInflater().inflate(R.layout.item_variation_bubble, layoutDetailColors, false);
            com.google.android.material.card.MaterialCardView card = bubbleView.findViewById(R.id.cardVariationBubble);
            ImageView ivImage = bubbleView.findViewById(R.id.ivVariationBubbleImage);
            TextView tvName = bubbleView.findViewById(R.id.tvVariationBubbleName);

            tvName.setText(color);

            // Check if this variation has an image
            if (product.variationImages != null && product.variationImages.containsKey(color)) {
                String imageUrl = product.variationImages.get(color);
                ivImage.setVisibility(View.VISIBLE);
                com.bumptech.glide.Glide.with(this).load(imageUrl).into(ivImage);
                tvName.setPadding(4, 0, 12, 0); // Less padding if image is present
            } else {
                ivImage.setVisibility(View.GONE);
                tvName.setPadding(12, 0, 12, 0);
            }

            // Selection logic
            card.setOnClickListener(v -> {
                // Clear previous selection
                for (int i = 0; i < layoutDetailColors.getChildCount(); i++) {
                    View child = layoutDetailColors.getChildAt(i);
                    com.google.android.material.card.MaterialCardView c = child.findViewById(R.id.cardVariationBubble);
                    c.setStrokeColor(ColorStateList.valueOf(getResources().getColor(R.color.gray_200)));
                    c.setCardBackgroundColor(Color.WHITE);
                }
                // Set current selection
                card.setStrokeColor(ColorStateList.valueOf(getResources().getColor(R.color.colorAccent)));
                card.setCardBackgroundColor(getResources().getColor(R.color.wizard_accent_tint));
                
                txtSelectedColor.setText("Color: " + color);
                selectedColorIndex = product.colors.indexOf(color);
                updateAvailableSizes(color);
            });

            layoutDetailColors.addView(bubbleView);
        }
    }

    private void setupSizeSelection() {
        layoutDetailSizes = findViewById(R.id.layoutDetailSizes);
        txtSelectedSize = findViewById(R.id.txtSelectedSize);
        
        if (product == null || product.sizes == null || product.sizes.isEmpty()) {
            if (txtSelectedSize != null) txtSelectedSize.setVisibility(View.GONE);
            if (layoutDetailSizes != null) layoutDetailSizes.setVisibility(View.GONE);
            return;
        }

        layoutDetailSizes.removeAllViews();
        for (String size : product.sizes) {
            View bubbleView = getLayoutInflater().inflate(R.layout.item_variation_bubble, layoutDetailSizes, false);
            com.google.android.material.card.MaterialCardView card = bubbleView.findViewById(R.id.cardVariationBubble);
            ImageView ivImage = bubbleView.findViewById(R.id.ivVariationBubbleImage);
            TextView tvName = bubbleView.findViewById(R.id.tvVariationBubbleName);

            tvName.setText(size);
            ivImage.setVisibility(View.GONE);

            card.setOnClickListener(v -> {
                // Clear previous selection
                for (int i = 0; i < layoutDetailSizes.getChildCount(); i++) {
                    View child = layoutDetailSizes.getChildAt(i);
                    com.google.android.material.card.MaterialCardView c = child.findViewById(R.id.cardVariationBubble);
                    c.setStrokeColor(ContextCompat.getColor(this, R.color.gray_200));
                    c.setCardBackgroundColor(Color.WHITE);
                }
                // Set current selection
                card.setStrokeColor(ContextCompat.getColor(this, R.color.colorAccent));
                card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.wizard_accent_tint));
                
                txtSelectedSize.setText("Size: " + size);
                selectedSizeIndex = product.sizes.indexOf(size);
                updateBuyButtonState();
            });

            layoutDetailSizes.addView(bubbleView);
        }
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
        boolean hasColors = product != null && product.colors != null && !product.colors.isEmpty();
        boolean hasSizes = product != null && product.sizes != null && !product.sizes.isEmpty();
        
        boolean colorSelected = !hasColors || selectedColorIndex != -1;
        boolean sizeSelected = !hasSizes || selectedSizeIndex != -1;
        
        boolean canBuy = colorSelected && sizeSelected;
        btnBuyNow.setEnabled(canBuy);
        btnAddToCart.setEnabled(canBuy);
    }

    private void setupBuyButtons() {
        btnBuyNow = findViewById(R.id.btnBuyNow);
        btnAddToCart = findViewById(R.id.btnAddToCart);
        
        btnBuyNow.setOnClickListener(v -> {
            if (product == null) return;
            if (selectedColorIndex == -1 || selectedSizeIndex == -1) {
                showVariationBottomSheet();
                return;
            }
            startBuyNow();
        });
        
        btnAddToCart.setOnClickListener(v -> {
            if (product == null) return;
            if (selectedColorIndex == -1 || selectedSizeIndex == -1) {
                showVariationBottomSheet();
                return;
            }
            addToCart(product.id, getSelectedColor(), getSelectedSize());
        });
    }

    private String getAbsoluteUrl(String uri) {
        if (uri == null || uri.isEmpty()) return "";
        if (uri.startsWith("http")) return uri;
        if (uri.startsWith("uploads/")) return Config.BASE_URL + uri;
        return Config.PRODUCT_IMAGES_URL + uri;
    }

    private void messageSeller() {
        if (product != null && product.sellerId != null) {
            String currentUserId = SupabaseManager.getCurrentUserId();
            if (currentUserId != null) {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("otherUserId", product.sellerId);
                intent.putExtra("productId", product.id);
                intent.putExtra("isCustomerInitiator", true);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Please log in to message the seller", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Seller information not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void addToCart(String productId, String color, String size) {
        if (product == null || product.sellerId == null || product.sellerId.isEmpty()) {
            Toast.makeText(this, "Seller information missing. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = SupabaseManager.getCurrentUserId();
        if (userId == null) {
            Toast.makeText(this, "Please log in to add to cart", Toast.LENGTH_SHORT).show();
            return;
        }

        SupabaseManager.CartItem item = new SupabaseManager.CartItem(
            productId,
            userId,
            currentQuantity,
            color,
            size,
            product.name,
            product.price,
            product.coverPhotoUri,
            product.sellerId
        );

        SupabaseManager.addToCart(item, new SupabaseManager.SupabaseCallback() {
            @Override
            public void onResult(boolean success, String error) {
                if (success) {
                    Toast.makeText(ProductDetailsActivity.this, "Added to cart", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ProductDetailsActivity.this, "Failed to add to cart: " + error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}