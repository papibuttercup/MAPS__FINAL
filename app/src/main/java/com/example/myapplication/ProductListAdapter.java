package com.example.myapplication;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import com.bumptech.glide.Glide;
import android.content.Intent;

public class ProductListAdapter extends RecyclerView.Adapter<ProductListAdapter.ViewHolder> {
    private List<Product> products;
    private Context context;
    private OnEditProductListener editListener;
    private OnDeleteProductListener deleteListener;
    private boolean isSeller = false;

    public interface OnEditProductListener {
        void onEditProduct(Product product);
    }

    public interface OnDeleteProductListener {
        void onDeleteProduct(Product product);
    }

    public void setOnEditProductListener(OnEditProductListener listener) {
        this.editListener = listener;
    }

    public void setOnDeleteProductListener(OnDeleteProductListener listener) {
        this.deleteListener = listener;
    }

    public ProductListAdapter(Context context, List<Product> products, boolean isSeller) {
        this.context = context;
        this.products = products;
        this.isSeller = isSeller;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);
        // Set product image
        String imageUri = null;
        if (product.coverPhotoUri != null && !product.coverPhotoUri.isEmpty()) {
            imageUri = product.coverPhotoUri;
        } else if (product.productImageUris != null && !product.productImageUris.isEmpty()) {
            imageUri = product.productImageUris.get(0);
        }
        if (imageUri != null && !imageUri.isEmpty()) {
            Glide.with(context)
                .load(imageUri)
                .placeholder(R.drawable.ic_image_placeholder)
                .into(holder.imgProduct);
        } else {
            holder.imgProduct.setImageResource(R.drawable.ic_image_placeholder);
        }
        holder.txtProductName.setText(product.name);
        holder.txtProductPrice.setText("₱" + product.price);
        TextView stockView = holder.itemView.findViewById(R.id.textProductStock);
        if (stockView != null) {
            stockView.setText("Stock: " + (product.stock != null ? product.stock : "N/A"));
        }
        if (holder.btnEditProduct != null) {
            if (isSeller) {
                holder.btnEditProduct.setVisibility(View.VISIBLE);
                holder.btnEditProduct.setOnClickListener(v -> {
                    Intent intent = new Intent(context, EditProductActivity.class);
                    intent.putExtra("productId", product.id);
                    context.startActivity(intent);
                });
            } else {
                holder.btnEditProduct.setVisibility(View.GONE);
            }
        }
        if (holder.btnDeleteProduct != null) {
            if (isSeller) {
                holder.btnDeleteProduct.setVisibility(View.VISIBLE);
                holder.btnDeleteProduct.setOnClickListener(v -> {
                    new android.app.AlertDialog.Builder(context)
                        .setTitle("Delete Product")
                        .setMessage("Are you sure you want to delete this product?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            if (deleteListener != null) deleteListener.onDeleteProduct(product);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                });
            } else {
                holder.btnDeleteProduct.setVisibility(View.GONE);
            }
        }
        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(context, ProductDetailsActivity.class);
            intent.putExtra("productId", product.id);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView txtProductName, txtProductPrice;
        ImageButton btnEditProduct, btnDeleteProduct;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            txtProductName = itemView.findViewById(R.id.txtProductName);
            txtProductPrice = itemView.findViewById(R.id.txtProductPrice);
            btnEditProduct = itemView.findViewById(R.id.btnEditProduct);
            btnDeleteProduct = itemView.findViewById(R.id.btnDeleteProduct);
        }
    }
} 