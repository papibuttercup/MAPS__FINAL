package com.example.myapplication;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Date;
import java.text.ParseException;

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
        String currentUserId = SupabaseManager.getCurrentUserId();
        
        if (currentUserId == null) return;

        String otherUserId = null;
        if (chat.participants != null) {
            for (String id : chat.participants) {
                if (!id.equals(currentUserId)) {
                    otherUserId = id;
                    break;
                }
            }
        }

        if (otherUserId != null) {
            final String finalOtherUserId = otherUserId;
            SupabaseManager.getUserProfile(otherUserId, new SupabaseManager.SupabaseCallbackWithProfile() {
                @Override
                public void onResult(boolean success, SupabaseManager.Profile profile, String error) {
                    if (success && profile != null) {
                        if (isCustomerList) {
                            String shopName = profile.getShop_name();
                            holder.tvUser.setText(shopName != null ? shopName : "Unknown Shop");
                        } else {
                            String firstName = profile.getFirst_name();
                            String lastName = profile.getLast_name();
                            String fullName = "";
                            if (firstName != null) fullName += firstName;
                            if (lastName != null) fullName += (fullName.isEmpty() ? "" : " ") + lastName;
                            holder.tvUser.setText(!fullName.isEmpty() ? fullName : "Unknown Customer");
                        }
                    } else {
                        holder.tvUser.setText(isCustomerList ? "Unknown Shop" : "Unknown Customer");
                    }
                }
            });
        } else {
            holder.tvUser.setText("Unknown");
        }

        holder.tvLastMessage.setText(chat.lastMessage != null ? chat.lastMessage : "");
        
        // Show last message time
        if (chat.lastMessageTime != null) {
            try {
                // Supabase timestamps are usually ISO 8601
                SimpleDateFormat isoSdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date date = isoSdf.parse(chat.lastMessageTime);
                SimpleDateFormat displaySdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                holder.tvTime.setText(displaySdf.format(date));
            } catch (ParseException e) {
                holder.tvTime.setText("");
            }
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
