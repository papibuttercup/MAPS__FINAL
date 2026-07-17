package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class CartActivity extends AppCompatActivity {
    private static final String TAG = "CartActivityDebug";
    private RecyclerView recyclerView;
    private CartAdapter cartAdapter;
    private List<CartItem> cartItems = new ArrayList<>();
    private TextView txtEmptyCart;
    private Button btnCheckout;
    private String userId;
    private ImageButton btnBack;
    private TextView txtTotalPrice;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: started");
        try {
            setContentView(R.layout.activity_cart);
            Log.d(TAG, "onCreate: layout set");

            recyclerView = findViewById(R.id.recyclerCartItems);
            txtEmptyCart = findViewById(R.id.txtEmptyCart);
            btnCheckout = findViewById(R.id.btnCheckout);
            btnBack = findViewById(R.id.btnBack);
            txtTotalPrice = findViewById(R.id.txtTotalPrice);

            Log.d(TAG, "onCreate: views initialized. recyclerView is " + (recyclerView == null ? "null" : "not null"));

            if (recyclerView != null) {
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                cartAdapter = new CartAdapter(cartItems);
                cartAdapter.setCartListener(isEmpty -> {
                    Log.d(TAG, "onCartChanged: is empty = " + isEmpty);
                    if (txtEmptyCart != null) txtEmptyCart.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                    updateTotalPrice();
                });
                recyclerView.setAdapter(cartAdapter);
                Log.d(TAG, "onCreate: adapter set");
            }

            userId = SupabaseManager.getCurrentUserId();
            Log.d(TAG, "onCreate: userId = " + userId);
            
            if (userId == null) { 
                Log.w(TAG, "onCreate: userId is null, finishing");
                finish(); 
                return; 
            }

            loadCartItems();

            if (btnCheckout != null) btnCheckout.setOnClickListener(v -> processCheckout());
            if (btnBack != null) btnBack.setOnClickListener(v -> finish());
            
        } catch (Exception e) {
            Log.e(TAG, "onCreate: crash detected!", e);
            Toast.makeText(this, "Crash in onCreate: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadCartItems() {
        Log.d(TAG, "loadCartItems: starting fetch");
        SupabaseManager.getCart(userId, new SupabaseManager.SupabaseCallbackWithCart() {
            @Override
            public void onResult(boolean success, List<SupabaseManager.CartItem> items, String error) {
                Log.d(TAG, "loadCartItems onResult: success=" + success + ", items=" + (items == null ? "null" : items.size()));
                if (success && items != null) {
                    cartItems.clear();
                    for (SupabaseManager.CartItem i : items) {
                        cartItems.add(new CartItem(i.getProduct_id(), i.getSeller_id(), i.getProduct_name(), i.getProduct_image_url(), i.getProduct_price(), i.getQuantity(), i.getColor(), i.getSize()));
                    }
                    runOnUiThread(() -> {
                        if (cartAdapter != null) cartAdapter.notifyDataSetChanged();
                        if (txtEmptyCart != null) txtEmptyCart.setVisibility(cartItems.isEmpty() ? View.VISIBLE : View.GONE);
                        updateTotalPrice();
                        Log.d(TAG, "loadCartItems: UI updated");
                    });
                } else {
                    Log.e(TAG, "loadCartItems Error: " + error);
                    runOnUiThread(() -> Toast.makeText(CartActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void processCheckout() {
        if (cartItems.isEmpty()) { Toast.makeText(this, "Empty cart", Toast.LENGTH_SHORT).show(); return; }
        
        SupabaseManager.getUserProfile(userId, new SupabaseManager.SupabaseCallbackWithProfile() {
            @Override
            public void onResult(boolean success, SupabaseManager.Profile profile, String error) {
                if (success && profile != null) {
                    double total = calculateTotal();
                    String name = profile.getFirst_name() + " " + profile.getLast_name();
                    SupabaseManager.Order order = new SupabaseManager.Order(null, userId, cartItems.get(0).sellerId, total, "pending", profile.getShop_location() != null ? profile.getShop_location() : "N/A", name, "N/A", null);
                    SupabaseManager.placeOrder(order, new SupabaseManager.SupabaseCallback() {
                        @Override
                        public void onResult(boolean success, String error) {
                            if (success) {
                                startActivity(new Intent(CartActivity.this, OrderSuccessActivity.class));
                                finish();
                            } else {
                                Toast.makeText(CartActivity.this, "Order failed: " + error, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });
    }

    private double calculateTotal() {
        double total = 0;
        for (CartItem item : cartItems) total += item.price * item.quantity;
        return total;
    }

    private void updateTotalPrice() {
        txtTotalPrice.setText(String.format("Total: ₱%.2f", calculateTotal()));
    }
}
