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

public class ProductListAdapter extends RecyclerView.Adapter<ProductListAdapter.ViewHolder> {
    private List<Product> products;
    private Context context;
    private OnEditProductListener editListener;

    public interface OnEditProductListener {
        void onEditProduct(Product product);
    }

    public void setOnEditProductListener(OnEditProductListener listener) {
        this.editListener = listener;
    }

    public ProductListAdapter(Context context, List<Product> products) {
        this.context = context;
        this.products = products;
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
        if (holder.btnEditProduct != null) {
            holder.btnEditProduct.setOnClickListener(v -> {
                if (editListener != null) editListener.onEditProduct(product);
            });
        }
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView txtProductName, txtProductPrice;
        ImageButton btnEditProduct;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            txtProductName = itemView.findViewById(R.id.txtProductName);
            txtProductPrice = itemView.findViewById(R.id.txtProductPrice);
            btnEditProduct = itemView.findViewById(R.id.btnEditProduct);
        }
    }
} 