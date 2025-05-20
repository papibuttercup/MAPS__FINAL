package com.example.myapplication;

import com.google.firebase.Timestamp;
import java.util.List;

public class Chat {
    public String chatId;
    public List<String> participants;
    public String lastMessage;
    public Timestamp lastMessageTime;
    public String productId;
    public String productName;
    public String productImage;
    public Boolean unseenBySeller;
    public String customerName;

    public Chat() {}

    public Chat(String chatId, List<String> participants, String lastMessage, Timestamp lastMessageTime, String productId, String productName, String productImage) {
        this.chatId = chatId;
        this.participants = participants;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.productId = productId;
        this.productName = productName;
        this.productImage = productImage;
    }
} 