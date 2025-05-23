package com.example.myapplication;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.FieldValue;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.firebase.firestore.Query;

public class OrderRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String userId = FirebaseAuth.getInstance().getUid();

    public interface OrderListener {
        void onOrdersReceived(QuerySnapshot snap, Exception e);
    }

    public void placeOrder(Order order, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        Map<String, Object> orderMap = new HashMap<>();
        orderMap.put("customerId", order.customerId);
        orderMap.put("sellerId", order.sellerId);
        orderMap.put("products", order.products);
        orderMap.put("totalPrice", order.totalPrice);
        orderMap.put("status", order.status);
        orderMap.put("timestamp", FieldValue.serverTimestamp());
        db.collection("orders").add(orderMap)
            .addOnSuccessListener(docRef -> onSuccess.onSuccess(null))
            .addOnFailureListener(onFailure);
    }

    public ListenerRegistration listenToCustomerOrders(EventListener<QuerySnapshot> listener) {
        return db.collection("orders")
            .whereEqualTo("customerId", userId)
            .addSnapshotListener(listener);
    }

    public void listenToSellerOrders(OrderListener listener) {
        String sellerId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        if (sellerId.isEmpty()) {
            listener.onOrdersReceived(null, new Exception("User not authenticated"));
            return;
        }

        android.util.Log.d("OrderRepository", "Listening to orders for seller: " + sellerId);

        db.collection("orders")
            .whereEqualTo("sellerId", sellerId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((snapshots, e) -> {
                if (e != null) {
                    android.util.Log.e("OrderRepository", "Error loading seller orders", e);
                    listener.onOrdersReceived(null, e);
                    return;
                }

                if (snapshots != null) {
                    android.util.Log.d("OrderRepository", "Received " + snapshots.size() + " orders for seller");
                    for (var doc : snapshots) {
                        android.util.Log.d("OrderRepository", "Order ID: " + doc.getId() + 
                            ", Status: " + doc.getString("status"));
                    }
                }

                listener.onOrdersReceived(snapshots, null);
            });
    }

    public void updateOrderStatus(String orderId, String status, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection("orders").document(orderId)
            .update("status", status)
            .addOnSuccessListener(onSuccess)
            .addOnFailureListener(onFailure);
    }
} 