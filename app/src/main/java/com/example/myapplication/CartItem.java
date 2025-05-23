package com.example.myapplication;

public class CartItem {
    public String productId;
    public String sellerId;
    public String name;
    public String imageUrl;
    public double price;
    public int quantity;
    public String color;
    public String size;

    public CartItem() {}

    public CartItem(String productId, String sellerId, String name, String imageUrl, double price, int quantity, String color, String size) {
        this.productId = productId;
        this.sellerId = sellerId;
        this.name = name;
        this.imageUrl = imageUrl;
        this.price = price;
        this.quantity = quantity;
        this.color = color;
        this.size = size;
    }
} 