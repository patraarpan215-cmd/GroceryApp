package com.example.grocery.models;

import java.util.List;

public class Order {
    private String orderId;
    private String userId;
    private double totalAmount;
    private List<CartItem> items;
    private String status;

    // Required empty constructor for Firebase
    public Order() {}

    public Order(String orderId, String userId, double totalAmount, List<CartItem> items, String status) {
        this.orderId = orderId;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.items = items;
        this.status = status; // e.g., "Pending", "Delivered"
    }

    // Getters
    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public double getTotalAmount() { return totalAmount; }
    public List<CartItem> getItems() { return items; }
    public String getStatus() { return status; }
}