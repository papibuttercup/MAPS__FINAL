package com.example.myapplication;

import com.google.firebase.Timestamp;
import java.util.List;
import java.util.Map;

public class Order {
    public String orderId;
    public String customerId;
    public String sellerId;
    public List<Map<String, Object>> products;
    public double totalPrice;
    public String status;
    public Timestamp timestamp;
    public String deliveryAddress;
    public String customerName;
    public String customerPhone;

    // Required empty constructor for Firestore
    public Order() {}

    public Order(String customerId, String sellerId, List<Map<String, Object>> products, 
                double totalPrice, String deliveryAddress, String customerName, String customerPhone) {
        this.customerId = customerId;
        this.sellerId = sellerId;
        this.products = products;
        this.totalPrice = totalPrice;
        this.status = "pending";
        this.timestamp = Timestamp.now();
        this.deliveryAddress = deliveryAddress;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
    }
} 