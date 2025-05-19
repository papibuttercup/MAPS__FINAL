package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import com.example.myapplication.Product;
import com.example.myapplication.R;
import com.example.myapplication.BuyNowActivity;

public class ProductAdapter1 extends RecyclerView.Adapter<ProductAdapter1.ViewHolder> {

    private Context context;
    private List<Product> productList;

    public ProductAdapter1(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.txtProductName.setText(product.name);
        holder.txtProductPrice.setText(String.valueOf(product.price));
        holder.txtProductStock.setText(String.valueOf(product.stock));

        holder.btnBuyNow.setOnClickListener(v -> {
            Intent intent = new Intent(context, BuyNowActivity.class);
            intent.putExtra("productId", product.id);
            intent.putExtra("sellerId", product.sellerId);
            intent.putExtra("productName", product.name);
            intent.putExtra("productPrice", product.price);
            intent.putExtra("productStock", product.stock != null ? product.stock : 0);
            intent.putExtra("quantity", 1); // default quantity
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView txtProductName;
        public TextView txtProductPrice;
        public TextView txtProductStock;
        public Button btnBuyNow;

        public ViewHolder(View view) {
            super(view);
            txtProductName = view.findViewById(R.id.txtProductName);
            txtProductPrice = view.findViewById(R.id.txtProductPrice);

            btnBuyNow = view.findViewById(R.id.btnBuyNow);
        }
    }
} 