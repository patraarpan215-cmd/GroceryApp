package com.example.grocery.models;

public class Product {
    private String id;
    private String name;
    private String category;
    private double price;
    private String imageUrl;

    private String ingredients;

    private int shelfLifeDays;

    // Required empty constructor for Firebase
    public Product() {}

    // Constructor to create a new product easily
    public Product(String id, String name, String category, double price, String imageUrl,String ingredients) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.imageUrl = imageUrl;
        this.ingredients = ingredients;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
    public String getIngredients() { return ingredients; }

    public int getShelfLifeDays() {
        return shelfLifeDays > 0 ? shelfLifeDays : 7; // Default to 7 days if not set in Firebase
    }

    public void setShelfLifeDays(int shelfLifeDays) {
        this.shelfLifeDays = shelfLifeDays;
    }
}