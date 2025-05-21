package com.example.myapplication;

import android.app.AlertDialog;
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
    private boolean isCustomerView;

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }
    private OnOrderClickListener listener;

    public OrdersAdapter(List<Order> orders, boolean isCustomerView) {
        this.orders = orders;
        this.db = FirebaseFirestore.getInstance();
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        this.isCustomerView = isCustomerView;
    }

    public void setOnOrderClickListener(OnOrderClickListener listener) {
        this.listener = listener;
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
        
        holder.orderId.setText("Order #" + (order.orderId != null ? order.orderId : ""));
        holder.customerInfo.setText(String.format("Customer: %s\nPhone: %s", 
            order.customerName != null ? order.customerName : "Unknown",
            order.customerPhone != null ? order.customerPhone : "Unknown"));
        
        // Format item details using individual product fields
        StringBuilder itemDetails = new StringBuilder();
        itemDetails.append("Items:\n");
        itemDetails.append(String.format("- %s (x%d) - %s, %s", 
            order.productName != null ? order.productName : "Unknown Product",
            order.quantity > 0 ? order.quantity : 1, // Default to 1 if quantity is 0
            order.selectedColor != null ? order.selectedColor : "N/A",
            order.selectedSize != null ? order.selectedSize : "N/A"
        ));
        
        itemDetails.append(String.format("\nDelivery: %s", 
            order.deliveryAddress != null ? order.deliveryAddress : "No address"));

        holder.orderDetails.setText(itemDetails.toString());
            
        holder.totalPrice.setText(String.format("Total: ₱%.2f", order.totalAmount));
        holder.orderStatus.setText(String.format("Status: %s\nDate: %s", 
            getFriendlyStatus(order.status),
            order.timestamp != null ? dateFormat.format(order.timestamp.toDate()) : "Unknown"));

        // Show/hide buttons based on view type and order status
        holder.btnAccept.setVisibility(View.GONE);
        holder.btnReject.setVisibility(View.GONE);
        holder.btnComplete.setVisibility(View.GONE);
        holder.btnCancel.setVisibility(View.GONE);

        if (isCustomerView) {
            // For customer view, only show cancel button for pending orders
            if (order.status != null && order.status.equalsIgnoreCase("pending")) {
                holder.btnCancel.setVisibility(View.VISIBLE);
            }
        } else {
            // For seller view, show accept/reject for pending orders
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
                }
            }
        }

        // Set up button click listeners
        if (order.orderId != null) {
            if (isCustomerView) {
                holder.btnCancel.setOnClickListener(v -> {
                    new AlertDialog.Builder(v.getContext())
                        .setTitle("Cancel Order")
                        .setMessage("Are you sure you want to cancel this order?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            updateOrderStatus(order.orderId, "canceled", v.getContext(), () -> {
                                int pos = holder.getAdapterPosition();
                                if (pos != RecyclerView.NO_POSITION) {
                                    orders.remove(pos);
                                    notifyItemRemoved(pos);
                                }
                            });
                        })
                        .setNegativeButton("No", null)
                        .show();
                });
            } else {
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
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOrderClick(order);
        });
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

    private String getFriendlyStatus(String status) {
        if (status == null) return "Unknown";
        switch (status.toLowerCase()) {
            case "pending": return "Pending (Waiting for seller)";
            case "accepted": return "Accepted (On way for delivery)";
            case "completed": return "Delivered";
            case "rejected": return "Rejected";
            case "canceled": return "Canceled";
            default: return status;
        }
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderId, customerInfo, orderDetails, totalPrice, orderStatus;
        Button btnAccept, btnReject, btnComplete, btnCancel;

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
            btnCancel = itemView.findViewById(R.id.btnCancel);
        }
    }
} 