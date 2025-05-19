package com.example.myapplication;

public class CartItem {
    public String productId;
    public String name;
    public String imageUrl;
    public double price;
    public int quantity;

    public CartItem() {}

    public CartItem(String productId, String name, String imageUrl, double price, int quantity) {
        this.productId = productId;
        this.name = name;
        this.imageUrl = imageUrl;
        this.price = price;
        this.quantity = quantity;
    }
} 