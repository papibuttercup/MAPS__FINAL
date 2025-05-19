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

        db.collection("orders")
            .whereEqualTo("sellerId", sellerId)
            .addSnapshotListener((snapshots, e) -> {
                if (e != null) {
                    listener.onOrdersReceived(null, e);
                    return;
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