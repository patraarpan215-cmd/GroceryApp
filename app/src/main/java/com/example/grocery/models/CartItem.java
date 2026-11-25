package com.example.grocery.models;

public class CartItem {
    private String productId;
    private String productName;
    private double price;
    private int quantity;
    private String addedByUserId; // Field to track roommate / family member who added the item

    // 1. Default empty constructor (Required by Firebase to parse cloud JSON)
    public CartItem() {}

    // 2. Standard Constructor (For regular individual carts)
    public CartItem(String productId, String productName, double price, int quantity) {
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
    }

    // 3. Shared Household Cart Constructor (Includes addedByUserId for bill splitting)
    public CartItem(String productId, String productName, double price, int quantity, String addedByUserId) {
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
        this.addedByUserId = addedByUserId;
    }

    // --- Getters and Setters ---

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getAddedByUserId() {
        return addedByUserId;
    }

    public void setAddedByUserId(String addedByUserId) {
        this.addedByUserId = addedByUserId;
    }
}