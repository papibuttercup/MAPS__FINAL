package com.example.myapplication;

import java.util.List;
import java.util.Map;

public class Order {
    public String orderId;
    public String customerId;
    public String sellerId;
    public List<Map<String, Object>> products;
    public double totalPrice;
    public String status;
    public String timestamp; // Changed to String for Supabase ISO 8601
    public String deliveryAddress;
    public String customerName;
    public String customerPhone;
    public String productId;
    public String productName;
    public double price;
    public int quantity;
    public String imageUrl;
    public String paymentMethod;
    public String selectedColor;
    public String selectedSize;
    public double totalAmount;
    public String additionalDetails;
    public java.util.Date orderDate;

    public Order() {}

    public Order(String customerId, String sellerId, List<Map<String, Object>> products, 
                double totalPrice, String deliveryAddress, String customerName, String customerPhone) {
        this.customerId = customerId;
        this.sellerId = sellerId;
        this.products = products;
        this.totalPrice = totalPrice;
        this.status = "pending";
        this.deliveryAddress = deliveryAddress;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
    }
}
