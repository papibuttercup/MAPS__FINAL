package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.ViewHolder> {
    private List<Order> orders;
    private FirebaseFirestore db;
    private SimpleDateFormat dateFormat;

    public OrdersAdapter(List<Order> orders) {
        this.orders = orders;
        this.db = FirebaseFirestore.getInstance();
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orders.get(position);
        
        holder.orderId.setText("Order #" + order.orderId);
        holder.customerInfo.setText(String.format("Customer: %s\nPhone: %s", 
            order.customerName, order.customerPhone));
        holder.orderDetails.setText(String.format("Items: %d\nDelivery: %s", 
            order.products.size(), order.deliveryAddress));
        holder.totalPrice.setText(String.format("Total: ₱%.2f", order.totalPrice));
        holder.orderStatus.setText(String.format("Status: %s\nDate: %s", 
            order.status, dateFormat.format(order.timestamp.toDate())));

        // Show/hide buttons based on order status
        holder.btnAccept.setVisibility(View.GONE);
        holder.btnReject.setVisibility(View.GONE);
        holder.btnComplete.setVisibility(View.GONE);

        if (order.status != null) {
            switch (order.status.toLowerCase()) {
                case "pending":
                    holder.btnAccept.setVisibility(View.VISIBLE);
                    holder.btnReject.setVisibility(View.VISIBLE);
                    break;
                case "accepted":
                    holder.btnComplete.setVisibility(View.VISIBLE);
                    holder.btnComplete.setText("Mark as Delivered");
                    break;
                // No buttons for completed/rejected
            }
        }

        // Set up button click listeners
        holder.btnAccept.setOnClickListener(v -> {
            updateOrderStatus(order.orderId, "accepted", holder.itemView.getContext(), () -> {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    orders.remove(pos);
                    notifyItemRemoved(pos);
                }
            });
        });
        holder.btnReject.setOnClickListener(v -> updateOrderStatus(order.orderId, "rejected", holder.itemView.getContext(), null));
        holder.btnComplete.setOnClickListener(v -> updateOrderStatus(order.orderId, "completed", holder.itemView.getContext(), null));
    }

    private void updateOrderStatus(String orderId, String newStatus, android.content.Context context, Runnable onSuccess) {
        db.collection("orders").document(orderId)
            .update("status", newStatus)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(context, 
                    "Order status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                if (onSuccess != null) onSuccess.run();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(context,
                    "Failed to update order status: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            });
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderId, customerInfo, orderDetails, totalPrice, orderStatus;
        Button btnAccept, btnReject, btnComplete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            orderId = itemView.findViewById(R.id.tvOrderId);
            customerInfo = itemView.findViewById(R.id.tvCustomerInfo);
            orderDetails = itemView.findViewById(R.id.tvOrderDetails);
            totalPrice = itemView.findViewById(R.id.tvTotalPrice);
            orderStatus = itemView.findViewById(R.id.tvOrderStatus);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnComplete = itemView.findViewById(R.id.btnComplete);
        }
    }
} 