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
import java.util.ArrayList;
import java.util.List;

public class SellerChatListActivity extends AppCompatActivity {
    private static final String TAG = "SellerChatList";
    private RecyclerView recyclerChats;
    private ChatListAdapter adapter;
    private List<Chat> chatList = new ArrayList<>();
    private String sellerId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_chat_list);

        // Set up the Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "No authenticated seller found");
            Toast.makeText(this, "Please login to view chats", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        sellerId = auth.getCurrentUser().getUid();

        // Verify seller account exists
        db.collection("sellers").document(sellerId)
            .get()
            .addOnSuccessListener(sellerDoc -> {
                if (!sellerDoc.exists()) {
                    Log.e(TAG, "Seller account not found");
                    Toast.makeText(this, "Seller account not found", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                setupRecyclerView();
                loadChats();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error verifying seller account", e);
                Toast.makeText(this, "Failed to verify seller account", Toast.LENGTH_SHORT).show();
                finish();
            });
    }

    private void setupRecyclerView() {
        recyclerChats = findViewById(R.id.recyclerChats);
        recyclerChats.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatListAdapter(chatList, chat -> {
            // Find the customer ID (the participant that is not the seller)
            String customerId = null;
            for (String id : chat.participants) {
                if (!id.equals(sellerId)) {
                    customerId = id;
                    break;
                }
            }
            if (customerId != null) {
                final String finalCustomerId = customerId;
                // Verify customer exists before starting chat
                db.collection("users").document(finalCustomerId)
                    .get()
                    .addOnSuccessListener(customerDoc -> {
                        if (customerDoc.exists()) {
                            Log.d(TAG, "Starting chat - Seller: " + sellerId + ", Customer: " + finalCustomerId);
                            Intent intent = new Intent(this, ChatActivity.class);
                            intent.putExtra("otherUserId", finalCustomerId);
                            intent.putExtra("isCustomerInitiator", false);
                            if (chat.productId != null) {
                                intent.putExtra("productId", chat.productId);
                            }
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "Customer account not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error finding customer", e);
                        Toast.makeText(this, "Failed to start chat", Toast.LENGTH_SHORT).show();
                    });
            }
        });
        recyclerChats.setAdapter(adapter);
    }

    private void loadChats() {
        db.collection("chats")
            .whereArrayContains("participants", sellerId)
            .orderBy("lastMessageTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                chatList.clear();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Chat chat = doc.toObject(Chat.class);
                    chat.chatId = doc.getId(); // Make sure chatId is set
                    // Fetch customer name and unseen status
                    String customerId = null;
                    for (String id : chat.participants) {
                        if (!id.equals(sellerId)) {
                            customerId = id;
                            break;
                        }
                    }
                    final String finalCustomerId = customerId;
                    if (customerId != null) {
                        db.collection("users").document(finalCustomerId)
                            .get()
                            .addOnSuccessListener(userDoc -> {
                                String name = userDoc.getString("name");
                                chat.customerName = name != null && !name.isEmpty() ? name : "Unknown Customer";
                                chat.unseenBySeller = doc.getBoolean("unseenBySeller") != null && doc.getBoolean("unseenBySeller");
                                adapter.notifyDataSetChanged();
                            })
                            .addOnFailureListener(e -> {
                                chat.customerName = "Unknown Customer";
                                adapter.notifyDataSetChanged();
                            });
                    }
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