package com.example.myapplication;

import java.util.List;

public class Chat {
    public String chatId;
    public List<String> participants;
    public String lastMessage;
    public String lastMessageTime; // Changed from Timestamp to String (ISO 8601) for Supabase
    public String productId;
    public String productName;
    public String productImage;
    public Boolean unseenBySeller;
    public String customerName;

    public Chat() {}

    public Chat(String chatId, List<String> participants, String lastMessage, String lastMessageTime, String productId, String productName, String productImage) {
        this.chatId = chatId;
        this.participants = participants;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.productId = productId;
        this.productName = productName;
        this.productImage = productImage;
    }
}
