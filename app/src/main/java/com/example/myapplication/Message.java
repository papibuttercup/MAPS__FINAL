package com.example.myapplication;

public class Message {
    private String senderId;
    private String content;
    private long timestamp;
    private boolean isRead;

    public Message() {
        // Required empty constructor for Firestore
    }

    public Message(String senderId, String content, long timestamp) {
        this.senderId = senderId;
        this.content = content;
        this.timestamp = timestamp;
        this.isRead = false;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
} 