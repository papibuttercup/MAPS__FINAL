package com.example.myapplication;

import java.util.List;

public class OrderRepository {
    private final String userId = SupabaseManager.getCurrentUserId();

    public interface OrderListener {
        void onOrdersReceived(List<SupabaseManager.Order> orders, String error);
    }

    public void listenToCustomerOrders(OrderListener listener) {
        if (userId == null) { listener.onOrdersReceived(null, "Not logged in"); return; }
        SupabaseManager.getOrders(userId, false, new SupabaseManager.SupabaseCallbackWithOrders() {
            @Override public void onResult(boolean success, List<SupabaseManager.Order> orders, String error) {
                listener.onOrdersReceived(orders, error);
            }
        });
    }

    public void listenToSellerOrders(OrderListener listener) {
        if (userId == null) { listener.onOrdersReceived(null, "Not logged in"); return; }
        SupabaseManager.getOrders(userId, true, new SupabaseManager.SupabaseCallbackWithOrders() {
            @Override public void onResult(boolean success, List<SupabaseManager.Order> orders, String error) {
                listener.onOrdersReceived(orders, error);
            }
        });
    }
}
