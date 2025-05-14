package com.example.myapplication;

import java.util.List;

public class Product {
    public String id;
    public String sellerId;
    public String imageUrl;
    public int imageResId;
    public String name;
    public String price;
    public List<Integer> colorList;
    public boolean isAvailable;
    public int stock;

    public Product(int imageResId, String name, String price, List<Integer> colorList, int stock) {
        this.imageResId = imageResId;
        this.name = name;
        this.price = price;
        this.colorList = colorList;
        this.isAvailable = true;
        this.stock = stock;
    }

    public Product(int imageResId, String name, String price, List<Integer> colorList) {
        this(imageResId, name, price, colorList, 0);
    }

    public Product() {}
} 