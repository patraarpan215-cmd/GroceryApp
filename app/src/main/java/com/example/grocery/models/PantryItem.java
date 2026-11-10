package com.example.grocery.models;

public class PantryItem {
    private String productId;
    private String productName;
    private long expiryDateMillis; // The exact millisecond the food goes bad

    public PantryItem() {}

    public PantryItem(String productId, String productName, long expiryDateMillis) {
        this.productId = productId;
        this.productName = productName;
        this.expiryDateMillis = expiryDateMillis;
    }

    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public long getExpiryDateMillis() { return expiryDateMillis; }
}
