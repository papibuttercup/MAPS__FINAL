package com.example.myapplication;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Locale;
import com.google.firebase.Timestamp;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {
    public interface OnChatClickListener {
        void onChatClick(Chat chat);
    }

    private List<Chat> chatList;
    private OnChatClickListener listener;
    private boolean isCustomerList = false;

    public ChatListAdapter(List<Chat> chatList, OnChatClickListener listener) {
        this.chatList = chatList;
        this.listener = listener;
    }

    public ChatListAdapter(List<Chat> chatList, OnChatClickListener listener, boolean isCustomerList) {
        this.chatList = chatList;
        this.listener = listener;
        this.isCustomerList = isCustomerList;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_list, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chatList.get(position);
        if (isCustomerList) {
            // Find seller UID
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String sellerId = null;
            for (String id : chat.participants) {
                if (!id.equals(currentUserId)) {
                    sellerId = id;
                    break;
                }
            }
            if (sellerId != null && !sellerId.isEmpty()) {
                FirebaseFirestore.getInstance().collection("sellers").document(sellerId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        String shopName = doc.getString("shopName");
                        holder.tvUser.setText(shopName != null ? shopName : "Unknown Shop");
                    })
                    .addOnFailureListener(e -> {
                        holder.tvUser.setText("Unknown Shop");
                    });
            } else {
                holder.tvUser.setText("Unknown Shop");
            }
        } else {
            // Seller chat list: fetch customer name from Firestore
            String customerId = "";
            for (String id : chat.participants) {
                if (!id.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                    customerId = id;
                    break;
                }
            }
            final String finalCustomerId = customerId;
            if (!finalCustomerId.isEmpty()) {
                FirebaseFirestore.getInstance().collection("users").document(finalCustomerId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        String firstName = doc.getString("firstName");
                        String lastName = doc.getString("lastName");
                        String fullName = "";
                        if (firstName != null) fullName += firstName;
                        if (lastName != null) fullName += (fullName.isEmpty() ? "" : " ") + lastName;
                        holder.tvUser.setText(!fullName.isEmpty() ? fullName : "Unknown Customer");
                    })
                    .addOnFailureListener(e -> {
                        holder.tvUser.setText("Unknown Customer");
                    });
            } else {
                holder.tvUser.setText("Unknown Customer");
            }
        }
        holder.tvLastMessage.setText(chat.lastMessage != null ? chat.lastMessage : "");
        // Show last message time
        if (chat.lastMessageTime != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            String timeStr = sdf.format(chat.lastMessageTime.toDate());
            holder.tvTime.setText(timeStr);
        } else {
            holder.tvTime.setText("");
        }
        // Unseen message indicator
        if (chat.unseenBySeller != null && chat.unseenBySeller) {
            holder.tvLastMessage.setTypeface(null, Typeface.BOLD);
            holder.tvUser.setTypeface(null, Typeface.BOLD);
            holder.tvTime.setTypeface(null, Typeface.BOLD);
            holder.badgeUnseen.setVisibility(View.VISIBLE);
        } else {
            holder.tvLastMessage.setTypeface(null, Typeface.NORMAL);
            holder.tvUser.setTypeface(null, Typeface.NORMAL);
            holder.tvTime.setTypeface(null, Typeface.NORMAL);
            holder.badgeUnseen.setVisibility(View.GONE);
        }
        holder.itemView.setOnClickListener(v -> listener.onChatClick(chat));
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvUser, tvLastMessage, badgeUnseen, tvTime;
        ChatViewHolder(View itemView) {
            super(itemView);
            tvUser = itemView.findViewById(R.id.tvUser);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            badgeUnseen = itemView.findViewById(R.id.badgeUnseen);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
} 