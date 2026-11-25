package com.example.grocery.models; // Keep your exact package line at the top!

import java.util.List;

public class Recipe {
    private String id;
    private String name;
    private String description;
    private List<String> ingredientIds;

    public Recipe() {} // Required empty constructor for Firebase

    public Recipe(String id, String name, String description, List<String> ingredientIds) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.ingredientIds = ingredientIds;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() {
        return description != null ? description : "Quick Meal";
    }
    public void setDescription(String description) { this.description = description; }

    public List<String> getIngredientIds() { return ingredientIds; }
    public void setIngredientIds(List<String> ingredientIds) { this.ingredientIds = ingredientIds; }
}