package com.example.myapplication;

import com.google.firebase.Timestamp;
import java.util.Date;
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

    // Required empty constructor for Firestore
    public Order() {}

    // Setter for timestamp to handle both Long and Timestamp from Firestore
    public void setTimestamp(Object timestamp) {
        if (timestamp instanceof Timestamp) {
            this.timestamp = (Timestamp) timestamp;
        } else if (timestamp instanceof Long) {
            // Convert Long (milliseconds) to Timestamp
            this.timestamp = new Timestamp(new Date((Long) timestamp));
        } else {
             // Handle other cases or set to null/default
             this.timestamp = null;
        }
    }

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