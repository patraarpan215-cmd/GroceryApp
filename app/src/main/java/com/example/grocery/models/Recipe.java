package com.example.grocery.models;

import java.util.List;

public class Recipe {
    private String id;
    private String name;
    private List<String> ingredientProductIds; // Stores the IDs of required products

    public Recipe() {} // Required empty constructor

    public Recipe(String id, String name, List<String> ingredientProductIds) {
        this.id = id;
        this.name = name;
        this.ingredientProductIds = ingredientProductIds;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public List<String> getIngredientProductIds() { return ingredientProductIds; }
}