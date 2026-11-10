package com.example.grocery.models;

public class User {
    private String uid;
    private String email;
    private double budgetLimit; // Stores the user's maximum spending limit

    private String allergies;


    // Required empty constructor for Firebase
    public User() {}

    public User(String uid, String email, double budgetLimit,String allergies) {
        this.uid = uid;
        this.email = email;
        this.budgetLimit = budgetLimit;
        this.allergies = allergies;
    }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public double getBudgetLimit() { return budgetLimit; }
    public void setBudgetLimit(double budgetLimit) { this.budgetLimit = budgetLimit; }

    public String getAllergies() { return allergies; }
    public void setAllergies(String allergies) { this.allergies = allergies; }
}