package com.example.myapplication;

import java.util.List;

public class CartRepository {
    private final String customerId = SupabaseManager.getCurrentUserId();

    public interface CartCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public interface CartItemsCallback {
        void onSuccess(List<SupabaseManager.CartItem> items);
        void onFailure(String error);
    }

    public void addToCart(SupabaseManager.CartItem item, CartCallback callback) {
        if (customerId == null) {
            callback.onFailure("User not logged in");
            return;
        }

        SupabaseManager.addToCart(item, new SupabaseManager.SupabaseCallback() {
            @Override
            public void onResult(boolean success, String error) {
                if (success) {
                    callback.onSuccess();
                } else {
                    callback.onFailure(error);
                }
            }
        });
    }

    public void getCart(CartItemsCallback callback) {
        if (customerId == null) {
            callback.onFailure("User not logged in");
            return;
        }

        SupabaseManager.getCart(customerId, new SupabaseManager.SupabaseCallbackWithCart() {
            @Override
            public void onResult(boolean success, List<SupabaseManager.CartItem> items, String error) {
                if (success) {
                    callback.onSuccess(items);
                } else {
                    callback.onFailure(error);
                }
            }
        });
    }

    public void clearCart(CartCallback callback) {
        if (customerId == null) {
            callback.onFailure("User not logged in");
            return;
        }

        // SupabaseManager doesn't have clearCart yet, we could implement it or delete items one by one
        // For now, I'll assume we can implement it in SupabaseManager later or use a workaround.
        callback.onFailure("clearCart not yet implemented in SupabaseManager");
    }
}
