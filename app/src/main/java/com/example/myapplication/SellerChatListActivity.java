package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class SellerChatListActivity extends AppCompatActivity {
    private RecyclerView recyclerChats;
    private ChatListAdapter adapter;
    private List<Chat> chatList = new ArrayList<>();
    private String sellerId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_chat_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) { getSupportActionBar().setDisplayHomeAsUpEnabled(true); getSupportActionBar().setTitle("Customer Chats"); }
        toolbar.setNavigationOnClickListener(v -> finish());

        sellerId = SupabaseManager.getCurrentUserId();
        if (sellerId == null) { finish(); return; }

        recyclerChats = findViewById(R.id.recyclerChats);
        recyclerChats.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatListAdapter(chatList, chat -> {
            String otherId = chat.participants.get(0).equals(sellerId) ? chat.participants.get(1) : chat.participants.get(0);
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("otherUserId", otherId);
            intent.putExtra("productId", chat.productId);
            intent.putExtra("isCustomerInitiator", false);
            startActivity(intent);
        });
        recyclerChats.setAdapter(adapter);

        loadChats();
    }

    private void loadChats() {
        SupabaseManager.getChatList(sellerId, new SupabaseManager.SupabaseCallbackWithChats() {
            @Override public void onResult(boolean success, List<SupabaseManager.Chat> chats, String error) {
                if (success && chats != null) {
                    chatList.clear();
                    for (SupabaseManager.Chat c : chats) {
                        Chat chat = new Chat(); chat.chatId = c.getId();
                        chat.participants = new ArrayList<>(); chat.participants.add(c.getParticipant1()); chat.participants.add(c.getParticipant2());
                        chat.lastMessage = c.getLast_message();
                        chat.productId = c.getProduct_id();
                        chatList.add(chat);
                    }
                    adapter.notifyDataSetChanged();
                }
            }
        });
    }
}
