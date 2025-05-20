package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class CustomerChatListActivity extends AppCompatActivity {
    private static final String TAG = "CustomerChatList";
    private RecyclerView recyclerChats;
    private ChatListAdapter adapter;
    private List<Chat> chatList = new ArrayList<>();
    private String customerId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_chat_list); // reuse the same layout

        // Set up the Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setTitle("Chats");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "No authenticated customer found");
            Toast.makeText(this, "Please login to view chats", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        customerId = auth.getCurrentUser().getUid();
        setupRecyclerView();
        loadChats();
    }

    private void setupRecyclerView() {
        recyclerChats = findViewById(R.id.recyclerChats);
        recyclerChats.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatListAdapter(chatList, chat -> {
            // Find the seller ID (the participant that is not the customer)
            String sellerId = null;
            for (String id : chat.participants) {
                if (!id.equals(customerId)) {
                    sellerId = id;
                    break;
                }
            }
            if (sellerId != null) {
                final String finalSellerId = sellerId;
                // Verify seller exists before starting chat
                db.collection("sellers").document(finalSellerId)
                    .get()
                    .addOnSuccessListener(sellerDoc -> {
                        if (sellerDoc.exists()) {
                            Log.d(TAG, "Starting chat - Customer: " + customerId + ", Seller: " + finalSellerId);
                            Intent intent = new Intent(this, ChatActivity.class);
                            intent.putExtra("otherUserId", finalSellerId);
                            intent.putExtra("isCustomerInitiator", true);
                            if (chat.productId != null) {
                                intent.putExtra("productId", chat.productId);
                            }
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "Shop not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error finding shop", e);
                        Toast.makeText(this, "Failed to start chat", Toast.LENGTH_SHORT).show();
                    });
            }
        }, true);
        recyclerChats.setAdapter(adapter);
    }

    private void loadChats() {
        db.collection("chats")
            .whereArrayContains("participants", customerId)
            .orderBy("lastMessageTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                chatList.clear();
                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    Chat chat = doc.toObject(Chat.class);
                    chat.chatId = doc.getId();
                    chatList.add(chat);
                }
                adapter.notifyDataSetChanged();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading chats", e);
                Toast.makeText(this, "Failed to load chats", Toast.LENGTH_SHORT).show();
            });
    }
} 