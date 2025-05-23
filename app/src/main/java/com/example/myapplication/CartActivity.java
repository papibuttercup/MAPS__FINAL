package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private CartAdapter cartAdapter;
    private List<CartItem> cartItems = new ArrayList<>();
    private TextView txtEmptyCart;
    private Button btnCheckout;
    private FirebaseFirestore db;
    private String userId;
    private ImageButton btnBack;
    private TextView txtTotalPrice;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        recyclerView = findViewById(R.id.recyclerCartItems);
        txtEmptyCart = findViewById(R.id.txtEmptyCart);
        btnCheckout = findViewById(R.id.btnCheckout);
        btnBack = findViewById(R.id.btnBack);
        txtTotalPrice = findViewById(R.id.txtTotalPrice);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartAdapter = new CartAdapter(cartItems);
        cartAdapter.setCartListener(isEmpty -> {
            txtEmptyCart.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            updateTotalPrice();
        });
        recyclerView.setAdapter(cartAdapter);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadCartItems();

        btnCheckout.setOnClickListener(v -> processCheckout());

        btnBack.setOnClickListener(v -> finish());
    }

    private void loadCartItems() {
        db.collection("carts").document(userId).collection("items")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                cartItems.clear();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    CartItem item = doc.toObject(CartItem.class);
                    cartItems.add(item);
                }
                cartAdapter.notifyDataSetChanged();
                txtEmptyCart.setVisibility(cartItems.isEmpty() ? View.VISIBLE : View.GONE);
                updateTotalPrice();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to load cart items", Toast.LENGTH_SHORT).show();
            });
    }

    private void processCheckout() {
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Extract sellerId from the first cart item (assuming all items are from the same seller)
        String sellerId = cartItems.get(0).sellerId;

        // Create order document
        Map<String, Object> order = new HashMap<>();
        order.put("customerId", userId);
        order.put("sellerId", sellerId);
        order.put("products", cartItems);
        order.put("totalPrice", calculateTotal());
        order.put("status", "pending");
        order.put("timestamp", System.currentTimeMillis());
        order.put("deliveryAddress", "");
        order.put("customerName", "");
        order.put("customerPhone", "");
        order.put("paymentMethod", "");
        order.put("orderDate", new java.util.Date());
        order.put("additionalDetails", "");

        // Start transaction to create order and clear cart
        db.runTransaction(transaction -> {
            // Create order
            DocumentReference orderRef = db.collection("orders").document();
            transaction.set(orderRef, order);

            // Clear cart
            DocumentReference cartRef = db.collection("carts").document(userId);
            transaction.delete(cartRef);

            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            android.util.Log.e("CartActivity", "Order creation failed", e);
            Toast.makeText(this, "Failed to process order: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private double calculateTotal() {
        double total = 0;
        for (CartItem item : cartItems) {
            total += item.price * item.quantity;
        }
        return total;
    }

    private void updateTotalPrice() {
        double total = calculateTotal();
        txtTotalPrice.setText(String.format("Total: ₱%.2f", total));
    }
} 