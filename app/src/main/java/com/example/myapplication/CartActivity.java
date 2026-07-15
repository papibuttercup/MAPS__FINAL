package com.example.myapplication;

import android.content.Intent;
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
import com.google.firebase.firestore.FieldValue;
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

        // Fetch customer info first
        db.collection("users").document(userId).get()
            .addOnSuccessListener(customerDoc -> {
                String customerName = "Unknown Customer";
                String customerPhone = "N/A";
                String deliveryAddress = "N/A";

                if (customerDoc.exists()) {
                    String firstName = customerDoc.getString("firstName");
                    String lastName = customerDoc.getString("lastName");
                    if (firstName != null || lastName != null) {
                        customerName = (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
                        customerName = customerName.trim();
                    }
                    customerPhone = customerDoc.getString("phone");
                    deliveryAddress = customerDoc.getString("address");
                }

                // Extract sellerId from the first cart item
                String sellerId = cartItems.get(0).sellerId;
                double totalAmount = calculateTotal();

                // Generate order ID
                DocumentReference orderRef = db.collection("orders").document();
                String orderId = orderRef.getId();

                // Create order document
                Map<String, Object> order = new HashMap<>();
                order.put("orderId", orderId);
                order.put("customerId", userId);
                order.put("sellerId", sellerId);
                order.put("products", cartItems);
                order.put("totalAmount", totalAmount); // standardized field
                order.put("totalPrice", totalAmount);  // legacy support
                order.put("status", "pending");
                order.put("timestamp", FieldValue.serverTimestamp());
                order.put("deliveryAddress", deliveryAddress != null ? deliveryAddress : "N/A");
                order.put("customerName", customerName);
                order.put("customerPhone", customerPhone != null ? customerPhone : "N/A");
                order.put("paymentMethod", "COD"); // Default for now or from UI
                order.put("orderDate", new java.util.Date());
                order.put("additionalDetails", "");

                // Start transaction to create order and clear cart
                db.runTransaction(transaction -> {
                    // Create main order
                    transaction.set(orderRef, order);

                    // Add to customer's orders subcollection
                    DocumentReference customerOrderRef = db.collection("users")
                        .document(userId)
                        .collection("orders")
                        .document(orderId);
                    transaction.set(customerOrderRef, order);

                    // Add to seller's orders subcollection
                    DocumentReference sellerOrderRef = db.collection("sellers")
                        .document(sellerId)
                        .collection("orders")
                        .document(orderId);
                    transaction.set(sellerOrderRef, order);

                    // Clear cart
                    DocumentReference cartRef = db.collection("carts").document(userId);
                    transaction.delete(cartRef);
                    // Also clear items subcollection (optional but cleaner if your logic needs it)
                    // Note: delete on a doc doesn't delete its subcollections in Firestore usually, 
                    // but most cart logic just looks at the subcollection.

                    return null;
                }).addOnSuccessListener(aVoid -> {
                    Intent intent = new Intent(this, OrderSuccessActivity.class);
                    startActivity(intent);
                    finish();
                }).addOnFailureListener(e -> {
                    android.util.Log.e("CartActivity", "Order creation failed", e);
                    Toast.makeText(this, "Failed to process order: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Could not fetch profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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