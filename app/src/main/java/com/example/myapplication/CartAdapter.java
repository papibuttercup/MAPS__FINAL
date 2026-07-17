package com.example.myapplication;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;
import androidx.core.content.ContextCompat;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
    private static final String TAG = "CartAdapterDebug";
    private List<CartItem> cartItems;
    public interface CartListener { void onCartChanged(boolean isEmpty); }
    private CartListener cartListener;
    public void setCartListener(CartListener listener) { this.cartListener = listener; }

    public CartAdapter(List<CartItem> cartItems) { this.cartItems = cartItems; }

    @NonNull @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        try {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
            return new CartViewHolder(view);
        } catch (Exception e) {
            Log.e(TAG, "onCreateViewHolder: Crash while inflating or creating ViewHolder", e);
            throw e;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        try {
            CartItem item = cartItems.get(position);
            holder.txtName.setText(item.name != null ? item.name : "");
            holder.txtPrice.setText("₱" + item.price);
            holder.txtQuantity.setText(String.valueOf(item.quantity));
            holder.txtSize.setText(item.size != null ? item.size : "-");
            if (item.imageUrl != null && !item.imageUrl.isEmpty()) Glide.with(holder.itemView.getContext()).load(item.imageUrl).into(holder.imgProduct);
            else holder.imgProduct.setImageResource(R.drawable.placeholder_image);
            
            holder.btnDecrease.setOnClickListener(v -> { if (item.quantity > 1) updateQuantity(item, item.quantity - 1); });
            holder.btnIncrease.setOnClickListener(v -> updateQuantity(item, item.quantity + 1));
            holder.btnRemove.setOnClickListener(v -> removeItem(item));
        } catch (Exception e) {
            Log.e(TAG, "onBindViewHolder: Crash at position " + position, e);
        }
    }

    @Override public int getItemCount() { return cartItems.size(); }

    private void updateQuantity(CartItem item, int newQuantity) {
        String userId = SupabaseManager.getCurrentUserId();
        if (userId == null) return;
        SupabaseManager.updateCartQuantity(userId, item.productId, newQuantity, new SupabaseManager.SupabaseCallback() {
            @Override public void onResult(boolean success, String error) {
                if (success) { item.quantity = newQuantity; notifyDataSetChanged(); if (cartListener != null) cartListener.onCartChanged(false); }
            }
        });
    }

    private void removeItem(CartItem item) {
        String userId = SupabaseManager.getCurrentUserId();
        if (userId == null) return;
        SupabaseManager.removeFromCart(userId, item.productId, new SupabaseManager.SupabaseCallback() {
            @Override public void onResult(boolean success, String error) {
                if (success) { cartItems.remove(item); notifyDataSetChanged(); if (cartListener != null) cartListener.onCartChanged(cartItems.isEmpty()); }
            }
        });
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct; TextView txtName, txtPrice, txtQuantity, txtSize; ImageButton btnDecrease, btnIncrease, btnRemove;
        public CartViewHolder(@NonNull View v) {
            super(v);
            imgProduct = v.findViewById(R.id.imgProduct); txtName = v.findViewById(R.id.txtName); txtPrice = v.findViewById(R.id.txtPrice);
            txtQuantity = v.findViewById(R.id.txtQuantity); txtSize = v.findViewById(R.id.txtSize);
            btnDecrease = v.findViewById(R.id.btnDecrease); btnIncrease = v.findViewById(R.id.btnIncrease); btnRemove = v.findViewById(R.id.btnRemove);
        }
    }
}
