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

public class ProductDetailsActivity extends AppCompatActivity {
    private ViewPager2 viewPagerImages;
    private ImageView imgProduct;
    private TextView txtProductName, txtProductPrice, txtDiscountBadge, txtOldPrice, txtGuarantee, txtRating, txtSold;
    private Button btnBuyNow;
    private Product product;
    private List<String> imageUrls = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);

        viewPagerImages = findViewById(R.id.viewPagerImages);
        imgProduct = findViewById(R.id.imgProduct);
        txtProductName = findViewById(R.id.txtProductName);
        txtProductPrice = findViewById(R.id.txtProductPrice);
        btnBuyNow = findViewById(R.id.btnBuyNow);
        txtDiscountBadge = findViewById(R.id.txtDiscountBadge);
        txtOldPrice = findViewById(R.id.txtOldPrice);
        txtGuarantee = findViewById(R.id.txtGuarantee);
        txtRating = findViewById(R.id.txtRating);
        txtSold = findViewById(R.id.txtSold);

        String productId = getIntent().getStringExtra("productId");
        FirebaseFirestore.getInstance().collection("products").document(productId)
            .get().addOnSuccessListener(doc -> {
                product = doc.toObject(Product.class);
                if (product != null) {
                    product.id = doc.getId();
                    txtProductName.setText(product.name != null ? product.name : "");
                    txtProductPrice.setText("₱" + product.price);
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
                    } else if (imageUrls.size() == 1) {
                        viewPagerImages.setVisibility(View.GONE);
                        imgProduct.setVisibility(View.VISIBLE);
                        com.bumptech.glide.Glide.with(this).load(imageUrls.get(0)).into(imgProduct);
                    } else {
                        viewPagerImages.setVisibility(View.GONE);
                        imgProduct.setVisibility(View.VISIBLE);
                        imgProduct.setImageResource(R.drawable.placeholder_image);
                    }
                    // Discount badge (placeholder logic)
                    double discountPercent = 50.0;
                    if (discountPercent > 0) {
                        txtDiscountBadge.setVisibility(View.VISIBLE);
                        txtDiscountBadge.setText((int)discountPercent + "% OFF");
                    } else {
                        txtDiscountBadge.setVisibility(View.GONE);
                    }
                    // Old price (placeholder logic)
                    double oldPrice = product.price + 100; // Example: old price is 100 more than current
                    if (oldPrice > product.price) {
                        txtOldPrice.setVisibility(View.VISIBLE);
                        txtOldPrice.setText("₱" + oldPrice);
                        txtOldPrice.setPaintFlags(txtOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    } else {
                        txtOldPrice.setVisibility(View.GONE);
                    }
                    // Guarantee/info
                    txtGuarantee.setText("Shopee Guarantees 100% Ori");
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
                intent.putExtra("quantity", 1);
                startActivity(intent);
            }
        });
    }
} 