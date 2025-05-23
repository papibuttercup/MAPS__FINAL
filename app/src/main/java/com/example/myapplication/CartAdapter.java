package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.core.content.ContextCompat;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
    private List<CartItem> cartItems;
    public interface CartListener {
        void onCartChanged(boolean isEmpty);
    }
    private CartListener cartListener;
    public void setCartListener(CartListener listener) {
        this.cartListener = listener;
    }

    public CartAdapter(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        holder.txtName.setText(item.name != null ? item.name : "");
        holder.txtPrice.setText("₱" + item.price);
        holder.txtQuantity.setText("Qty: " + item.quantity);
        holder.txtColor.setText("Color: " + (item.color != null ? item.color : "-"));
        holder.txtSize.setText("Size: " + (item.size != null ? item.size : "-"));
        if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext()).load(item.imageUrl).into(holder.imgProduct);
        } else {
            holder.imgProduct.setImageResource(R.drawable.placeholder_image);
        }
        holder.btnDecrease.setEnabled(item.quantity > 1);
        holder.btnDecrease.setBackground(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_remove));
        holder.btnIncrease.setBackground(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_add));
        holder.btnRemove.setBackground(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_delete));
        holder.btnDecrease.setOnClickListener(v -> {
            if (item.quantity > 1) {
                updateQuantity(item, item.quantity - 1);
            }
        });
        holder.btnIncrease.setOnClickListener(v -> {
            updateQuantity(item, item.quantity + 1);
        });
        holder.btnRemove.setOnClickListener(v -> {
            removeItem(item);
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    private void updateQuantity(CartItem item, int newQuantity) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("carts").document(userId)
            .collection("items")
            .whereEqualTo("productId", item.productId)
            .whereEqualTo("color", item.color)
            .whereEqualTo("size", item.size)
            .get()
            .addOnSuccessListener(query -> {
                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : query) {
                    doc.getReference().update("quantity", newQuantity);
                }
            });
        item.quantity = newQuantity;
        notifyDataSetChanged();
    }

    private void removeItem(CartItem item) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("carts").document(userId)
            .collection("items")
            .whereEqualTo("productId", item.productId)
            .whereEqualTo("color", item.color)
            .whereEqualTo("size", item.size)
            .get()
            .addOnSuccessListener(query -> {
                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : query) {
                    doc.getReference().delete();
                }
            });
        cartItems.remove(item);
        notifyDataSetChanged();
        if (cartListener != null) cartListener.onCartChanged(cartItems.isEmpty());
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView txtName, txtPrice, txtQuantity, txtColor, txtSize;
        Button btnDecrease, btnIncrease, btnRemove;
        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            txtName = itemView.findViewById(R.id.txtName);
            txtPrice = itemView.findViewById(R.id.txtPrice);
            txtQuantity = itemView.findViewById(R.id.txtQuantity);
            txtColor = itemView.findViewById(R.id.txtColor);
            txtSize = itemView.findViewById(R.id.txtSize);
            btnDecrease = itemView.findViewById(R.id.btnDecrease);
            btnIncrease = itemView.findViewById(R.id.btnIncrease);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
} 