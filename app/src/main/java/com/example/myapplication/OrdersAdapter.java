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
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Date;
import java.text.ParseException;

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.ViewHolder> {
    private List<SupabaseManager.Order> orders;
    private SimpleDateFormat dateFormat;
    private boolean isCustomerView;

    public interface OnOrderClickListener {
        void onOrderClick(SupabaseManager.Order order);
    }
    private OnOrderClickListener listener;

    public OrdersAdapter(List<SupabaseManager.Order> orders, boolean isCustomerView) {
        this.orders = orders;
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
        SupabaseManager.Order order = orders.get(position);
        
        holder.orderId.setText("Order #" + (order.getId() != null ? order.getId() : ""));
        String customerName = order.getCustomer_name() != null && !order.getCustomer_name().isEmpty() ? order.getCustomer_name() : "Unknown Customer";
        String customerPhone = order.getCustomer_phone() != null && !order.getCustomer_phone().isEmpty() ? order.getCustomer_phone() : "N/A";
        holder.customerInfo.setText(String.format("Customer: %s\nPhone: %s", customerName, customerPhone));

        // Format item details
        StringBuilder itemDetails = new StringBuilder();
        itemDetails.append("Items:\n");
        
        // SupabaseManager.Order doesn't have products list in the current Serializable data class, 
        // but it has total_amount. We might need to fetch order items separately.
        // For now, we'll just show the delivery address.
        
        String address = order.getDelivery_address() != null && !order.getDelivery_address().isEmpty() ? order.getDelivery_address() : "No address provided";
        itemDetails.append(String.format("Delivery: %s", address));

        holder.orderDetails.setText(itemDetails.toString().trim());
            
        double finalAmount = order.getTotal_amount();
        holder.totalPrice.setText(String.format("Total: ₱%.2f", finalAmount));
        
        String dateStr = "Unknown";
        if (order.getCreated_at() != null) {
            try {
                SimpleDateFormat isoSdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date date = isoSdf.parse(order.getCreated_at());
                dateStr = dateFormat.format(date);
            } catch (ParseException e) {
                dateStr = order.getCreated_at();
            }
        }

        holder.orderStatus.setText(String.format("Status: %s\nDate: %s", 
            getFriendlyStatus(order.getStatus()),
            dateStr));

        // Show/hide buttons based on view type and order status
        holder.btnAccept.setVisibility(View.GONE);
        holder.btnReject.setVisibility(View.GONE);
        holder.btnComplete.setVisibility(View.GONE);
        holder.btnCancel.setVisibility(View.GONE);

        String status = order.getStatus();
        if (isCustomerView) {
            if (status != null && status.equalsIgnoreCase("pending")) {
                holder.btnCancel.setVisibility(View.VISIBLE);
            }
        } else {
            if (status != null) {
                switch (status.toLowerCase()) {
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
        if (order.getId() != null) {
            if (isCustomerView) {
                holder.btnCancel.setOnClickListener(v -> {
                    new AlertDialog.Builder(v.getContext())
                        .setTitle("Cancel Order")
                        .setMessage("Are you sure you want to cancel this order?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            updateOrderStatus(order.getId(), "canceled", v.getContext(), () -> {
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
                    updateOrderStatus(order.getId(), "accepted", holder.itemView.getContext(), () -> {
                        int pos = holder.getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            orders.remove(pos);
                            notifyItemRemoved(pos);
                        }
                    });
                });
                holder.btnReject.setOnClickListener(v -> updateOrderStatus(order.getId(), "rejected", holder.itemView.getContext(), null));
                holder.btnComplete.setOnClickListener(v -> updateOrderStatus(order.getId(), "completed", holder.itemView.getContext(), null));
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOrderClick(order);
        });
    }

    private void updateOrderStatus(String orderId, String newStatus, android.content.Context context, Runnable onSuccess) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("status", newStatus);
        
        SupabaseManager.updateOrder(orderId, updates, new SupabaseManager.SupabaseCallback() {
            @Override
            public void onResult(boolean success, String error) {
                if (success) {
                    Toast.makeText(context, "Order updated to " + newStatus, Toast.LENGTH_SHORT).show();
                    if (onSuccess != null) onSuccess.run();
                } else {
                    Toast.makeText(context, "Failed to update order: " + error, Toast.LENGTH_SHORT).show();
                }
            }
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
