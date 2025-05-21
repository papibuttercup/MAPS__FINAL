package com.example.myapplication;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private RecyclerView recyclerMessages;
    private EditText editMessage;
    private ImageButton btnSend;
    private ImageView productImage;
    private TextView productName, productPrice;
    private View productCard;
    private MessagesAdapter adapter;
    private List<Message> messageList = new ArrayList<>();
    private String chatId;
    private String otherUserId;
    private String currentUserId;
    private FirebaseFirestore db;
    private String productId;
    private boolean isCustomerInitiator;
    private boolean hasSentProductMessage = false;
    private String currentProductImageUrl = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

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
            Log.e(TAG, "No authenticated user found");
            Toast.makeText(this, "Please login to use chat", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUserId = auth.getCurrentUser().getUid();
        otherUserId = getIntent().getStringExtra("otherUserId");
        productId = getIntent().getStringExtra("productId");
        isCustomerInitiator = getIntent().getBooleanExtra("isCustomerInitiator", true);

        if (otherUserId == null || otherUserId.isEmpty()) {
            Log.e(TAG, "otherUserId is null or empty");
            Toast.makeText(this, "Invalid chat partner", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Initializing chat - Current User: " + currentUserId + ", Other User: " + otherUserId);

        // Initialize views
        recyclerMessages = findViewById(R.id.recyclerMessages);
        editMessage = findViewById(R.id.editMessage);
        btnSend = findViewById(R.id.btnSend);
        productImage = findViewById(R.id.productImage);
        productName = findViewById(R.id.productName);
        productPrice = findViewById(R.id.productPrice);
        productCard = findViewById(R.id.productCard);

        // Set up RecyclerView
        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MessagesAdapter(messageList, currentUserId);
        recyclerMessages.setAdapter(adapter);

        // Get product info from intent (for instant display)
        String productNameStr = getIntent().getStringExtra("productName");
        String productImageStr = getIntent().getStringExtra("productImage");
        double productPriceVal = getIntent().getDoubleExtra("productPrice", 0.0);
        if (productImageStr != null && !productImageStr.isEmpty()) {
            currentProductImageUrl = productImageStr;
        }

        // Show/hide product card
        if (productId != null) {
            productCard.setVisibility(View.VISIBLE);
            if (productNameStr != null && !productNameStr.isEmpty()) productName.setText(productNameStr);
            if (productPriceVal > 0) productPrice.setText("₱" + productPriceVal);
            if (productImageStr != null && !productImageStr.isEmpty()) {
                Glide.with(this).load(productImageStr).into(productImage);
                currentProductImageUrl = productImageStr;
            }
            loadProductDetails();
        } else {
            productCard.setVisibility(View.GONE);
        }

        // Initialize or get existing chat
        initializeChat();

        // Set up send button
        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void initializeChat() {
        // Try to find current user in 'users' collection first
        db.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener(currentUserDoc -> {
                if (currentUserDoc.exists()) {
                    proceedWithChatInit();
                } else {
                    // Try 'sellers' collection as fallback
                    db.collection("sellers").document(currentUserId)
                        .get()
                        .addOnSuccessListener(sellerDoc -> {
                            if (sellerDoc.exists()) {
                                proceedWithChatInit();
                            } else {
                                Log.e(TAG, "Current user not found in users or sellers collection: " + currentUserId);
                                Toast.makeText(this, "Your account was not found. Please re-login or contact support.", Toast.LENGTH_LONG).show();
                                finish();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error checking sellers collection", e);
                            Toast.makeText(this, "Failed to verify your account (seller check)", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking users collection", e);
                Toast.makeText(this, "Failed to verify your account (user check)", Toast.LENGTH_SHORT).show();
                finish();
            });
    }

    private void proceedWithChatInit() {
        List<String> participants = Arrays.asList(currentUserId, otherUserId);
        participants.sort(String::compareTo);
        chatId = participants.get(0) + "_" + participants.get(1);
        Log.d(TAG, "Generated chat ID: " + chatId);
        db.collection("chats").document(chatId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (!documentSnapshot.exists()) {
                    createNewChat(participants);
                } else {
                    Log.d(TAG, "Existing chat found");
                    loadMessages();
                    // Mark as seen if seller
                    markMessagesSeenIfSeller();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking chat existence", e);
                Toast.makeText(this, "Failed to initialize chat (chat doc)", Toast.LENGTH_SHORT).show();
            });
    }

    private void createNewChat(List<String> participants) {
        Map<String, Object> chat = new HashMap<>();
        chat.put("participants", participants);
        chat.put("lastMessage", "");
        chat.put("lastMessageTime", Timestamp.now());
        chat.put("unseenBySeller", isCustomerInitiator); // If customer starts, mark unseen
        if (productId != null) {
            chat.put("productId", productId);
        }
        Log.d(TAG, "Creating new chat with ID: " + chatId);
        db.collection("chats").document(chatId)
            .set(chat)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Chat created successfully");
                loadMessages();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error creating chat", e);
                Toast.makeText(this, "Failed to create chat", Toast.LENGTH_SHORT).show();
            });
    }

    private void loadProductDetails() {
        if (productId != null) {
            db.collection("products").document(productId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Product product = documentSnapshot.toObject(Product.class);
                        if (product != null) {
                            productName.setText(product.name);
                            productPrice.setText("₱" + product.price);
                            if (product.coverPhotoUri != null) {
                                Glide.with(this)
                                    .load(product.coverPhotoUri)
                                    .into(productImage);
                                currentProductImageUrl = product.coverPhotoUri;
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading product details", e);
                    productCard.setVisibility(View.GONE);
                });
        }
    }

    private void loadMessages() {
        db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    Log.e(TAG, "Error loading messages", error);
                    Toast.makeText(this, "Failed to load messages", Toast.LENGTH_SHORT).show();
                    return;
                }

                hasSentProductMessage = false;
                if (value != null) {
                    messageList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        Message message = doc.toObject(Message.class);
                        messageList.add(message);
                        if (productId != null && productId.equals(message.getProductId())) {
                            hasSentProductMessage = true;
                        }
                    }
                    adapter.notifyDataSetChanged();
                    if (!messageList.isEmpty()) {
                        recyclerMessages.scrollToPosition(messageList.size() - 1);
                    }
                }
            });
    }

    private void sendMessage() {
        String messageText = editMessage.getText().toString().trim();
        if (messageText.isEmpty()) return;
        Message message = new Message();
        message.setSenderId(currentUserId);
        message.setContent(messageText);
        message.setTimestamp(System.currentTimeMillis());

        // Only attach product info if not already sent
        if (!hasSentProductMessage && productCard.getVisibility() == View.VISIBLE && productId != null) {
            message.setProductId(productId);
            message.setProductName(productName.getText().toString());
            message.setProductImage(currentProductImageUrl);
            try {
                String priceStr = productPrice.getText().toString().replace("₱", "").replace(",", "").trim();
                message.setProductPrice(Double.parseDouble(priceStr));
            } catch (Exception e) {
                message.setProductPrice(0.0);
            }
            hasSentProductMessage = true;
        }

        db.collection("chats").document(chatId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener(documentReference -> {
                // Update last message in chat document
                Map<String, Object> updates = new HashMap<>();
                updates.put("lastMessage", messageText);
                updates.put("lastMessageTime", Timestamp.now());
                if (isCustomerInitiator || !currentUserId.equals(otherUserId)) {
                    updates.put("unseenBySeller", true);
                }
                db.collection("chats").document(chatId)
                    .update(updates)
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error updating chat document", e);
                    });
                editMessage.setText("");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error sending message", e);
                Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
            });
    }

    private void markMessagesSeenIfSeller() {
        // If the current user is the seller, mark messages as seen
        if (!isCustomerInitiator && currentUserId.equals(otherUserId)) return; // Defensive
        db.collection("chats").document(chatId)
            .update("unseenBySeller", false);
    }
} 