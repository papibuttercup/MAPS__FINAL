package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class ProductDetailsActivity extends AppCompatActivity {
    private ViewPager2 viewPagerImages;
    private TextView txtProductName, txtProductPrice, txtProductStock;
    private Button btnAddToCart, btnBuyNow;
    private Product product;
    private List<String> imageUrls = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);

        viewPagerImages = findViewById(R.id.viewPagerImages);
        txtProductName = findViewById(R.id.txtProductName);
        txtProductPrice = findViewById(R.id.txtProductPrice);
        btnAddToCart = findViewById(R.id.btnAddToCart);
        btnBuyNow = findViewById(R.id.btnBuyNow);
        txtProductStock = findViewById(R.id.txtProductStock);

        String productId = getIntent().getStringExtra("productId");
        FirebaseFirestore.getInstance().collection("products").document(productId)
            .get().addOnSuccessListener(doc -> {
                product = doc.toObject(Product.class);
                if (product != null) {
                    product.id = doc.getId();
                    txtProductName.setText(product.name != null ? product.name : "");
                    txtProductPrice.setText("₱" + product.price);
                    txtProductStock.setText("Stock: " + (product.stock != null ? product.stock : "N/A"));
                    imageUrls.clear();
                    if (product.coverPhotoUri != null && !product.coverPhotoUri.isEmpty()) imageUrls.add(product.coverPhotoUri);
                    if (product.productImageUris != null) {
                        for (String url : product.productImageUris) {
                            if (product.coverPhotoUri == null || !url.equals(product.coverPhotoUri)) imageUrls.add(url);
                        }
                    }
                    viewPagerImages.setAdapter(new ImagePagerAdapter(imageUrls));
                } else {
                    Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to load product", Toast.LENGTH_SHORT).show();
                finish();
            });

        btnAddToCart.setOnClickListener(v -> {
            if (product == null) return;
            CartRepository cartRepo = new CartRepository();
            CartItem item = new CartItem(productId, product.name, imageUrls.size() > 0 ? imageUrls.get(0) : "", product.price, 1);
            cartRepo.addToCart(item, aVoid -> {
                Toast.makeText(this, "Added to cart!", Toast.LENGTH_SHORT).show();
            }, e -> {
                Toast.makeText(this, "Failed to add to cart", Toast.LENGTH_SHORT).show();
            });
        });

        btnBuyNow.setOnClickListener(v -> {
            if (product != null) {
                Intent intent = new Intent(ProductDetailsActivity.this, BuyNowActivity.class);
                intent.putExtra("productId", product.id);
                intent.putExtra("sellerId", product.sellerId);
                intent.putExtra("productName", product.name);
                intent.putExtra("productPrice", product.price);
                intent.putExtra("productStock", product.stock != null ? product.stock : 0);
                intent.putExtra("quantity", 1);
                startActivity(intent);
            }
        });
    }
} 