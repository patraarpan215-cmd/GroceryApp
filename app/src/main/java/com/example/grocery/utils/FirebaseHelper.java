package com.example.grocery.utils;

import androidx.annotation.NonNull;

import com.example.grocery.models.CartItem;
import com.example.grocery.models.Order;
import com.example.grocery.models.PantryItem;
import com.example.grocery.models.Product;
import com.example.grocery.models.Recipe;
import com.example.grocery.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FirebaseHelper {

    private final DatabaseReference dbRef;
    private final FirebaseAuth auth;

    // Interface callback for fetching product lists
    public interface ProductFetchCallback {
        void onSuccess(List<Product> productList);
        void onError(String errorMessage);
    }

    // Interface callback for simple database actions
    public interface ActionCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    // Interface to return fetched recipes
    public interface RecipeFetchCallback {
        void onSuccess(List<com.example.grocery.models.Recipe> recipeList);
        void onError(String errorMessage);
    }

    // Interface to return user's digital pantry items
    public interface PantryFetchCallback {
        void onSuccess(List<com.example.grocery.models.PantryItem> pantryList);
        void onError(String errorMessage);
    }

    // Constructor: Connects directly to Firebase services
    public FirebaseHelper() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        this.dbRef = database.getReference();
        this.auth = FirebaseAuth.getInstance();
    }

    // 1. Fetch all products from the cloud database
    public void fetchProducts(ProductFetchCallback callback) {
        dbRef.child("Products").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Product> productList = new ArrayList<>();
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    Product product = itemSnapshot.getValue(Product.class);
                    if (product != null) {
                        productList.add(product);
                    }
                }
                callback.onSuccess(productList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    // 2. Add an item to the logged-in user's online cart
    public void addToCart(CartItem item, ActionCallback callback) {
        if (auth.getCurrentUser() == null) {
            callback.onError("User is not logged in!");
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        DatabaseReference cartRef = dbRef.child("Carts").child(userId).child(item.getProductId());

        cartRef.setValue(item)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Add item to SharedCarts/{groupPin} if Group PIN is active, otherwise personal Carts/{userId}
    public void addToCartWithPin(CartItem item, String groupPin, ActionCallback callback) {
        if (auth.getCurrentUser() == null) {
            if (callback != null) callback.onError("User is not logged in!");
            return;
        }

        DatabaseReference cartRef;
        if (groupPin != null && !groupPin.isEmpty()) {
            cartRef = dbRef.child("SharedCarts").child(groupPin).child(item.getProductId());
        } else {
            String userId = auth.getCurrentUser().getUid();
            cartRef = dbRef.child("Carts").child(userId).child(item.getProductId());
        }

        cartRef.setValue(item)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    // Remove item from SharedCarts/{groupPin} if Group PIN is active, otherwise personal Carts/{userId}
    public void removeFromCartWithPin(String productId, String groupPin, ActionCallback callback) {
        if (auth.getCurrentUser() == null) {
            if (callback != null) callback.onError("User is not logged in!");
            return;
        }

        DatabaseReference itemRef;
        if (groupPin != null && !groupPin.isEmpty()) {
            itemRef = dbRef.child("SharedCarts").child(groupPin).child(productId);
        } else {
            String userId = auth.getCurrentUser().getUid();
            itemRef = dbRef.child("Carts").child(userId).child(productId);
        }

        itemRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }



    public void removeFromCart(String productId, String groupPin, ActionCallback callback) {
        DatabaseReference itemRef;
        if (groupPin != null && !groupPin.isEmpty()) {
            itemRef = dbRef.child("SharedCarts").child(groupPin).child(productId);
        } else {
            if (auth.getCurrentUser() == null) return;
            String userId = auth.getCurrentUser().getUid();
            itemRef = dbRef.child("Carts").child(userId).child(productId);
        }

        itemRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    // 3. Register a new user account with Email and Password
    // 3. Register a new user and create their profile in the database
    public void registerUser(String email, String password, ActionCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && auth.getCurrentUser() != null) {
                        String userId = auth.getCurrentUser().getUid();

                        // Set a default budget of $100 for new users
                        User newUser = new User(userId, email, 100.00,"none");

                        // Save the profile to the "Users" database node
                        dbRef.child("Users").child(userId).setValue(newUser)
                                .addOnSuccessListener(aVoid -> callback.onSuccess())
                                .addOnFailureListener(e -> callback.onError(e.getMessage()));
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                        callback.onError(error);
                    }
                });
    }

    // 4. Log in an existing user
    public void loginUser(String email, String password, ActionCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Login failed";
                        callback.onError(error);
                    }
                });
    }

    // 5. Log out the current user
    public void logoutUser() {
        auth.signOut();
    }

    // Get current logged-in user ID
    public String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }
    // 6. Process the checkout and place the order
    // 6. Process the checkout and sync to the Digital Pantry
    public void placeOrder(List<CartItem> cartItems, ActionCallback callback) {
        if (auth.getCurrentUser() == null) {
            callback.onError("User is not logged in!");
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        String orderId = dbRef.child("Orders").push().getKey();

        double totalAmount = 0;
        for (CartItem item : cartItems) {
            totalAmount += (item.getPrice() * item.getQuantity());
        }

        Order newOrder = new Order(orderId, userId, totalAmount, cartItems, "Pending");

        if (orderId != null) {
            dbRef.child("Orders").child(orderId).setValue(newOrder)
                    .addOnSuccessListener(aVoid -> {

                        // --- NEW PANTRY SYNC LOGIC ---
                        // Assume a standard 5-day shelf life (5 days in milliseconds)
                        long shelfLife = 5L * 24 * 60 * 60 * 1000;
                        long expiryTime = System.currentTimeMillis() + shelfLife;

                        for (CartItem boughtItem : cartItems) {
                            PantryItem pantryItem = new PantryItem(
                                    boughtItem.getProductId(),
                                    boughtItem.getProductName(),
                                    expiryTime
                            );
                            // Save to the user's digital pantry
                            dbRef.child("Pantry").child(userId).child(boughtItem.getProductId()).setValue(pantryItem);
                        }
                        // ------------------------------

                        // Clear the cart after successfully moving items to the pantry
                        dbRef.child("Carts").child(userId).removeValue();
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
        }
    }

    // 7. Update the user's spending limit
    public void updateBudgetLimit(double newLimit, ActionCallback callback) {
        if (auth.getCurrentUser() == null) {
            callback.onError("User not logged in");
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        // Target the specific "budgetLimit" value inside the user's profile
        dbRef.child("Users").child(userId).child("budgetLimit").setValue(newLimit)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // 8. The Budget-Lock Engine: Check budget before adding to cart
    public void smartAddToCart(CartItem newItem, ActionCallback callback) {
        if (auth.getCurrentUser() == null) {
            callback.onError("User is not logged in!");
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        // Phase 1: Fetch the user's custom budget limit
        dbRef.child("Users").child(userId).child("budgetLimit").get().addOnCompleteListener(budgetTask -> {
            if (budgetTask.isSuccessful() && budgetTask.getResult().exists()) {
                double budgetLimit = budgetTask.getResult().getValue(Double.class);

                // Phase 2: Fetch the user's current shopping cart
                dbRef.child("Carts").child(userId).get().addOnCompleteListener(cartTask -> {
                    double currentTotal = 0;

                    if (cartTask.isSuccessful() && cartTask.getResult().exists()) {
                        for (DataSnapshot snapshot : cartTask.getResult().getChildren()) {
                            CartItem existingItem = snapshot.getValue(CartItem.class);
                            if (existingItem != null) {
                                currentTotal += (existingItem.getPrice() * existingItem.getQuantity());
                            }
                        }
                    }

                    // Phase 3: The Comparison Math
                    double addedItemCost = newItem.getPrice() * newItem.getQuantity();
                    double projectedTotal = currentTotal + addedItemCost;

                    if (projectedTotal > budgetLimit) {
                        // Action Blocked: Alert the user how much money they have left
                        double amountLeft = budgetLimit - currentTotal;
                        callback.onError("Budget Locked! You only have $" + String.format("%.2f", amountLeft) + " left.");
                    } else {
                        // Action Approved: Add item to the database
                        dbRef.child("Carts").child(userId).child(newItem.getProductId()).setValue(newItem)
                                .addOnSuccessListener(aVoid -> callback.onSuccess())
                                .addOnFailureListener(e -> callback.onError(e.getMessage()));
                    }
                });
            } else {
                callback.onError("Could not load user budget.");
            }
        });
    }

    // 9. Health & Allergen Safety Shield
    public void safeAddToCart(CartItem cartItem, Product fullProductDetails, ActionCallback callback) {
        if (auth.getCurrentUser() == null) {
            callback.onError("User not logged in");
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        // Step 1: Fetch the user's health profile (allergies)
        dbRef.child("Users").child(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                User currentUser = task.getResult().getValue(User.class);

                if (currentUser != null && currentUser.getAllergies() != null) {
                    String userAllergies = currentUser.getAllergies().toLowerCase();
                    String productIngredients = fullProductDetails.getIngredients().toLowerCase();

                    // Step 2: Cross-reference algorithms
                    String[] allergyList = userAllergies.split(",");
                    boolean isSafe = true;
                    String detectedAllergen = "";

                    for (String allergy : allergyList) {
                        if (productIngredients.contains(allergy.trim())) {
                            isSafe = false;
                            detectedAllergen = allergy;
                            break; // Stop checking, we found a danger
                        }
                    }

                    // Step 3: Block or Approve
                    if (!isSafe) {
                        // Action Blocked: Danger detected
                        callback.onError("Health Shield Alert! This product contains: " + detectedAllergen);
                    } else {
                        // Action Approved: Move to the Budget-Lock check
                        smartAddToCart(cartItem, callback);
                    }
                } else {
                    // No allergies listed, proceed to budget check
                    smartAddToCart(cartItem, callback);
                }
            } else {
                callback.onError("Failed to load health profile.");
            }
        });
    }

    // 10. Recipe-to-Cart: Add all ingredients for a meal
    public void addRecipeToCart(Recipe recipe, ActionCallback callback) {
        if (auth.getCurrentUser() == null) {
            callback.onError("User not logged in");
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        List<String> productIds = recipe.getIngredientIds();

        // Loop through the recipe's ingredient IDs
        for (String pId : productIds) {
            // We use a default price of 0.00 and quantity 1 for the automated bulk-add.
            // In a production app, you would fetch the live price from the Products node first.
            CartItem item = new CartItem(pId, "Recipe Ingredient", 0.00, 1);
            dbRef.child("Carts").child(userId).child(pId).setValue(item);
        }

        callback.onSuccess();
    }

    // 4. Process Checkout: Calculate total, save order, and clear cart
    public void processCheckout(ActionCallback callback) {
        if (auth.getCurrentUser() == null) {
            callback.onError("User is not logged in!");
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        DatabaseReference cartRef = dbRef.child("Carts").child(userId);
        DatabaseReference ordersRef = dbRef.child("Orders").child(userId);

        // Read the cart exactly once
        cartRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    callback.onError("Your cart is empty!");
                    return;
                }

                double totalAmount = 0;
                // We use a generic Object structure here to easily move the whole list of items
                Object cartData = snapshot.getValue();

                // Calculate the math (price * quantity) for the backend records if needed
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    CartItem item = itemSnapshot.getValue(CartItem.class);
                    if (item != null) {
                        totalAmount += (item.getPrice() * item.getQuantity());
                    }
                }

                // Generate a unique Order ID based on the current timestamp
                String orderId = "order_" + System.currentTimeMillis();

                // Save the whole cart data into the Orders node under this new Order ID
                ordersRef.child(orderId).child("items").setValue(cartData);
                ordersRef.child(orderId).child("orderTotal").setValue(totalAmount)
                        .addOnSuccessListener(aVoid -> {
                            // If order is successfully saved, DELETE the current cart
                            cartRef.removeValue();
                            callback.onSuccess();
                        })
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    // 5. Add an item to a Shared Household Cart
    public void addToSharedCart(String groupPin, CartItem item, ActionCallback callback) {
        if (auth.getCurrentUser() == null) {
            callback.onError("User is not logged in!");
            return;
        }

        // We use the shared groupPin to route the data instead of the individual userId
        DatabaseReference sharedCartRef = dbRef.child("SharedCarts").child(groupPin).child(item.getProductId());

        sharedCartRef.setValue(item)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // 6. Listen for real-time updates from Roommates
// This allows the frontend to instantly refresh when someone else adds an item
    public void listenToSharedCart(String groupPin, ValueEventListener listener) {
        dbRef.child("SharedCarts").child(groupPin).addValueEventListener(listener);
    }

    // 4. Fetch all recipes from Firebase ("Recipes" node)
    public void fetchRecipes(RecipeFetchCallback callback) {
        dbRef.child("Recipes").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<com.example.grocery.models.Recipe> recipeList = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    com.example.grocery.models.Recipe recipe = ds.getValue(com.example.grocery.models.Recipe.class);
                    if (recipe != null) {
                        recipeList.add(recipe);
                    }
                }
                callback.onSuccess(recipeList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    // 5. Recipe-to-Cart: Add all ingredients of a selected recipe to the active cart
    public void addRecipeIngredientsToCart(List<String> ingredientIds, String groupPin, ActionCallback callback) {
        if (ingredientIds == null || ingredientIds.isEmpty()) {
            if (callback != null) callback.onError("No ingredients found in this recipe.");
            return;
        }

        String userId = getCurrentUserId();
        if (userId == null) {
            if (callback != null) callback.onError("User not logged in!");
            return;
        }

        dbRef.child("Products").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int addedCount = 0;
                for (String prodId : ingredientIds) {
                    Product product = snapshot.child(prodId).getValue(Product.class);
                    if (product != null) {
                        CartItem item = new CartItem(product.getId(), product.getName(), product.getPrice(), 1, userId);
                        if (groupPin != null && !groupPin.isEmpty()) {
                            dbRef.child("SharedCarts").child(groupPin).child(product.getId()).setValue(item);
                        } else {
                            dbRef.child("Carts").child(userId).child(product.getId()).setValue(item);
                        }
                        addedCount++;
                    }
                }
                if (callback != null) {
                    if (addedCount > 0) callback.onSuccess();
                    else callback.onError("Matching products not found in catalog.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null) callback.onError(error.getMessage());
            }
        });
    }

    // 6. Zero-Waste Pantry Sync: Move checked-out items into "Pantry/{userId}" with expiry dates
    public void syncCartToPantry(List<CartItem> purchasedItems, ActionCallback callback) {
        String userId = getCurrentUserId();
        if (userId == null || purchasedItems == null || purchasedItems.isEmpty()) return;

        dbRef.child("Products").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long nowMillis = System.currentTimeMillis();
                long oneDayMillis = 24L * 60L * 60L * 1000L;

                for (CartItem cartItem : purchasedItems) {
                    Product prod = snapshot.child(cartItem.getProductId()).getValue(Product.class);
                    int shelfLifeDays = (prod != null) ? prod.getShelfLifeDays() : 5; // Default 5 days
                    long expiryDateMillis = nowMillis + (shelfLifeDays * oneDayMillis);

                    com.example.grocery.models.PantryItem pantryItem = new com.example.grocery.models.PantryItem(
                            cartItem.getProductId(),
                            cartItem.getProductName(),
                            expiryDateMillis
                    );
                    dbRef.child("Pantry").child(userId).child(cartItem.getProductId()).setValue(pantryItem);
                }
                if (callback != null) callback.onSuccess();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null) callback.onError(error.getMessage());
            }
        });
    }
}

