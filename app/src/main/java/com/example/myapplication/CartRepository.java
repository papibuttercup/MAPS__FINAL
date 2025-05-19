package com.example.myapplication;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String customerId = FirebaseAuth.getInstance().getUid();

    public void addToCart(CartItem item, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection("carts").document(customerId)
            .update("cartItems", FieldValue.arrayUnion(item))
            .addOnSuccessListener(onSuccess)
            .addOnFailureListener(e -> {
                // If cart doesn't exist, create it
                Map<String, Object> cart = new HashMap<>();
                cart.put("cartItems", java.util.Arrays.asList(item));
                db.collection("carts").document(customerId)
                    .set(cart)
                    .addOnSuccessListener(onSuccess)
                    .addOnFailureListener(onFailure);
            });
    }

    public void getCart(OnSuccessListener<DocumentSnapshot> onSuccess, OnFailureListener onFailure) {
        db.collection("carts").document(customerId)
            .get()
            .addOnSuccessListener(onSuccess)
            .addOnFailureListener(onFailure);
    }

    public void clearCart(OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection("carts").document(customerId)
            .delete()
            .addOnSuccessListener(onSuccess)
            .addOnFailureListener(onFailure);
    }
} 