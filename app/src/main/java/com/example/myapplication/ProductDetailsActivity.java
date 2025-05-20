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

public class ProductDetailsActivity extends AppCompatActivity {
    private ViewPager2 viewPagerImages;
    private ImageView imgProduct;
    private TextView txtProductName, txtProductPrice, txtGuarantee, txtRating, txtSold, txtSellerName, txtStock;
    private Button btnBuyNow;
    private Button btnAddToCart;
    private ImageButton btnMessageSeller;
    private Product product;
    private List<String> imageUrls = new ArrayList<>();
    private TextView txtImageCount;
    private FirebaseFirestore db;

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
        txtStock = findViewById(R.id.txtStock);

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
                    txtStock.setText("Stock: " + (product.stock != null ? product.stock : 0));
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
}