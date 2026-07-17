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
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView recyclerMessages;
    private EditText editMessage;
    private ImageButton btnSend;
    private ImageView productImage;
    private TextView productName, productPrice;
    private View productCard;
    private MessagesAdapter adapter;
    private List<Message> messageList = new ArrayList<>();
    private String chatId, otherUserId, currentUserId, productId;
    private SupabaseManager.ProductModel currentProduct;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        currentUserId = SupabaseManager.getCurrentUserId();
        if (currentUserId == null) { finish(); return; }
        
        otherUserId = getIntent().getStringExtra("otherUserId");
        productId = getIntent().getStringExtra("productId");
        chatId = currentUserId.compareTo(otherUserId) < 0 ? currentUserId + "_" + otherUserId : otherUserId + "_" + currentUserId;

        recyclerMessages = findViewById(R.id.recyclerMessages);
        editMessage = findViewById(R.id.editMessage);
        btnSend = findViewById(R.id.btnSend);
        productImage = findViewById(R.id.productImage);
        productName = findViewById(R.id.productName);
        productPrice = findViewById(R.id.productPrice);
        productCard = findViewById(R.id.productCard);

        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MessagesAdapter(messageList, currentUserId);
        recyclerMessages.setAdapter(adapter);

        if (productId != null) {
            productCard.setVisibility(View.VISIBLE);
            loadProductDetails();
        } else productCard.setVisibility(View.GONE);

        loadMessages();
        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void loadProductDetails() {
        SupabaseManager.getProduct(productId, new SupabaseManager.SupabaseCallbackWithProduct() {
            @Override public void onResult(boolean success, SupabaseManager.ProductModel product, String error) {
                if (success && product != null) {
                    currentProduct = product;
                    productName.setText(product.getName());
                    productPrice.setText("₱" + product.getPrice());
                    
                    String imageUrl = product.getCover_photo_url();
                    if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.startsWith("http")) {
                        imageUrl = Config.PRODUCT_IMAGES_URL + imageUrl;
                    }
                    Glide.with(ChatActivity.this).load(imageUrl).into(productImage);
                    
                    // Update any existing messages that might be waiting for product info
                    for (Message m : messageList) {
                        if (m.getProductId() != null && m.getProductId().equals(productId)) {
                            m.setProductName(product.getName());
                            m.setProductPrice(product.getPrice());
                            m.setProductImage(product.getCover_photo_url());
                        }
                    }
                    adapter.notifyDataSetChanged();
                }
            }
        });
    }

    private void loadMessages() {
        SupabaseManager.getMessages(chatId, new SupabaseManager.SupabaseCallbackWithMessages() {
            @Override public void onResult(boolean success, List<SupabaseManager.ChatMessage> messages, String error) {
                if (success && messages != null) {
                    messageList.clear();
                    for (SupabaseManager.ChatMessage m : messages) {
                        Message msg = new Message();
                        msg.setSenderId(m.getSender_id()); 
                        msg.setContent(m.getContent());
                        msg.setProductId(m.getProduct_id());
                        
                        // Set product details if we have them
                        if (m.getProduct_id() != null) {
                            if (currentProduct != null && m.getProduct_id().equals(currentProduct.getId())) {
                                msg.setProductName(currentProduct.getName());
                                msg.setProductPrice(currentProduct.getPrice());
                                msg.setProductImage(currentProduct.getCover_photo_url());
                            }
                            // If it's a different product, it remains without details until we fetch them (TBD)
                        }
                        
                        // Map timestamp
                        if (m.getCreated_at() != null) {
                            try {
                                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
                                java.util.Date date = sdf.parse(m.getCreated_at());
                                if (date != null) msg.setTimestamp(date.getTime());
                            } catch (Exception e) {
                                Log.e("ChatActivity", "Error parsing date: " + m.getCreated_at());
                            }
                        }

                        messageList.add(msg);
                    }
                    adapter.notifyDataSetChanged();
                    recyclerMessages.scrollToPosition(messageList.size() - 1);
                }
            }
        });
    }

    private void sendMessage() {
        String content = editMessage.getText().toString().trim();
        if (content.isEmpty()) return;
        SupabaseManager.ChatMessage m = new SupabaseManager.ChatMessage(null, chatId, currentUserId, content, null, false, productId);
        SupabaseManager.sendMessage(m, new SupabaseManager.SupabaseCallback() {
            @Override public void onResult(boolean success, String error) {
                if (success) { editMessage.setText(""); loadMessages(); }
            }
        });
    }
}
