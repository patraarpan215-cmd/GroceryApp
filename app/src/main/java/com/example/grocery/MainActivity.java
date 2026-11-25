package com.example.grocery;

import androidx.annotation.NonNull;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.grocery.models.CartItem;
import com.example.grocery.models.Product;
import com.example.grocery.utils.ExpiryWorker;
import com.example.grocery.utils.FirebaseHelper;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private double currentCartTotal = 0.0;
    private double userBudget = 0.0;
    private TextView tvBudgetStatus;

    private static final String PREFS_NAME = "FreshBasketPrefs";
    private static final String KEY_BUDGET = "user_budget";
    private static final int NOTIFICATION_PERMISSION_CODE = 101;

    private RecyclerView recyclerViewProducts;
    private ProgressBar progressBar;
    private TextView tvModeIndicator;
    private MaterialButton btnSharedCartMode, btnViewCart;

    private EditText etSearchProducts;
    private android.widget.ImageView btnClearSearch;

    private ProductAdapter productAdapter;

    private final List<Product> allProductsList = new ArrayList<>();
    private final List<Product> productList = new ArrayList<>();
    private FirebaseHelper firebaseHelper;

    private String activeGroupPin = null;
    private com.google.firebase.database.DatabaseReference activeCartRef;
    private com.google.firebase.database.ValueEventListener cartTotalListener;

    private com.google.firebase.database.DatabaseReference groupMembersRef;
    private com.google.firebase.database.ValueEventListener groupMembersListener;

    private final java.util.Map<String, Integer> cartQuantities = new java.util.HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvBudgetStatus = findViewById(R.id.tvBudgetStatus);
        MaterialButton btnSetBudget = findViewById(R.id.btnSetBudget);

        // Load saved budget from SharedPreferences
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        userBudget = prefs.getFloat(KEY_BUDGET, 0f);
        updateBudgetBanner();

        // Set Budget Button Click Listener
        btnSetBudget.setOnClickListener(v -> {
            android.widget.EditText input = new android.widget.EditText(this);
            input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            input.setHint("Enter budget in ₹ (0 to clear)");

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Lock Your Budget")
                    .setView(input)
                    .setPositiveButton("Set", (dialog, which) -> {
                        String val = input.getText().toString().trim();
                        if (!val.isEmpty()) {
                            userBudget = Double.parseDouble(val);
                            updateBudgetBanner();

                            android.content.SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                            editor.putFloat(KEY_BUDGET, (float) userBudget);
                            editor.apply();

                            String msg = (userBudget > 0)
                                    ? "Budget locked at ₹" + userBudget
                                    : "Budget lock removed";
                            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // 1. Connect UI elements
        recyclerViewProducts = findViewById(R.id.recyclerViewProducts);
        progressBar = findViewById(R.id.progressBar);
        tvModeIndicator = findViewById(R.id.tvModeIndicator);
        btnSharedCartMode = findViewById(R.id.btnSharedCartMode);
        btnViewCart = findViewById(R.id.btnViewCart);
        etSearchProducts = findViewById(R.id.etSearchProducts);
        btnClearSearch = findViewById(R.id.btnClearSearch);


        // 2. Initialize Firebase helper
        firebaseHelper = new FirebaseHelper();

        // 3. Set up 2-column grid for grocery cards
        recyclerViewProducts.setLayoutManager(new GridLayoutManager(this, 2));
        productAdapter = new ProductAdapter(this, productList, cartQuantities, new ProductAdapter.OnProductActionListener() {
            @Override
            public void onIncreaseQuantity(Product product, int currentQty) {
                checkBudgetAndAddToCart(product);
            }

            @Override
            public void onDecreaseQuantity(Product product, int currentQty) {
                decreaseProductQuantity(product, currentQty);
            }
        });
        recyclerViewProducts.setAdapter(productAdapter);

        setupSearchBar();

        // 4. Load products and sync live cart total
        loadProducts();
        listenToCartTotal();

        // 5. Button Click Listeners
        btnSharedCartMode.setOnClickListener(v -> showGroupPinDialog());

        MaterialButton btnRecipes = findViewById(R.id.btnRecipes);
        btnRecipes.setOnClickListener(v -> showRecipesDialog());

        btnViewCart.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(MainActivity.this, CartActivity.class);
            intent.putExtra("GROUP_PIN", activeGroupPin);
            startActivity(intent);
        });

        // 6. Keep Pantry Expiry background alerts active
        requestNotificationPermission();
        scheduleExpiryCheck();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh cart total & remaining budget when returning from CartActivity
        listenToCartTotal();
    }

    private void setupSearchBar() {
        etSearchProducts.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString();
                btnClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                filterProducts(query);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> etSearchProducts.setText(""));
    }

    private void filterProducts(String query) {
        productList.clear();
        if (query == null || query.trim().isEmpty()) {
            productList.addAll(allProductsList);
        } else {
            String lowerQuery = query.trim().toLowerCase();
            for (Product p : allProductsList) {
                boolean matchesName = p.getName() != null && p.getName().toLowerCase().contains(lowerQuery);
                boolean matchesCategory = p.getCategory() != null && p.getCategory().toLowerCase().contains(lowerQuery);
                if (matchesName || matchesCategory) {
                    productList.add(p);
                }
            }
        }
        productAdapter.notifyDataSetChanged();
    }



    private void updateBudgetBanner() {
        if (tvBudgetStatus == null) return;
        if (userBudget > 0) {
            double residual = Math.max(0.0, userBudget - currentCartTotal);
            tvBudgetStatus.setText("Budget: ₹" + (int) userBudget + " | Left: ₹" + (int) residual);
        } else {
            tvBudgetStatus.setText("Budget: Not Set");
        }
    }

    private void loadProducts() {
        progressBar.setVisibility(View.VISIBLE);
        firebaseHelper.fetchProducts(new FirebaseHelper.ProductFetchCallback() {
            @Override
            public void onSuccess(List<Product> fetchedProducts) {
                progressBar.setVisibility(View.GONE);
                allProductsList.clear();
                allProductsList.addAll(fetchedProducts);
                String currentQuery = (etSearchProducts != null) ? etSearchProducts.getText().toString() : "";
                filterProducts(currentQuery);
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void listenToCartTotal() {
        if (activeCartRef != null && cartTotalListener != null) {
            activeCartRef.removeEventListener(cartTotalListener);
        }

        String userId = firebaseHelper.getCurrentUserId();
        if (activeGroupPin != null && !activeGroupPin.isEmpty()) {
            activeCartRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("SharedCarts").child(activeGroupPin);
        } else if (userId != null) {
            activeCartRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("Carts").child(userId);
        } else {
            return;
        }

        cartTotalListener = new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                double total = 0.0;
                cartQuantities.clear();

                for (com.google.firebase.database.DataSnapshot ds : snapshot.getChildren()) {
                    CartItem item = ds.getValue(CartItem.class);
                    if (item != null && item.getProductId() != null) {
                        total += item.getPrice() * item.getQuantity();
                        cartQuantities.put(item.getProductId(), item.getQuantity());
                    }
                }

                currentCartTotal = total;
                updateBudgetBanner();

                if (productAdapter != null) {
                    productAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
        };

        activeCartRef.addValueEventListener(cartTotalListener);
    }

    private void showGroupPinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Switch Cart Mode");
        builder.setMessage("Enter a 4-digit Group PIN to sync with roommates, or switch back to your Personal Cart:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("e.g. 1234");
        if (activeGroupPin != null) {
            input.setText(activeGroupPin);
        }
        builder.setView(input);

        builder.setPositiveButton("Join Group PIN", (dialog, which) -> {
            String pin = input.getText().toString().trim();
            if (pin.isEmpty()) {
                switchToPersonalCart();
            } else {
                activeGroupPin = pin;
                registerAndListenGroupMembers(pin);
                currentCartTotal = 0.0;
                updateBudgetBanner();
                listenToCartTotal();
                Toast.makeText(this, "Switched to Shared Cart #" + pin, Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNeutralButton("Personal Cart", (dialog, which) -> switchToPersonalCart());
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void registerAndListenGroupMembers(String pin) {
        final String finalPin = pin;
        if (groupMembersRef != null && groupMembersListener != null) {
            groupMembersRef.removeEventListener(groupMembersListener);
        }

        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        groupMembersRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("SharedCartMembers").child(pin);

        // Save current user's display name/email under this Group PIN
        if (currentUser != null) {
            String email = currentUser.getEmail();
            String displayName = (email != null && email.contains("@"))
                    ? email.substring(0, email.indexOf("@")) : "Roommate";
            groupMembersRef.child(currentUser.getUid()).setValue(displayName);
        }

        // Listen live for total people in this Group PIN
        groupMembersListener = new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                long memberCount = snapshot.getChildrenCount();
                tvModeIndicator.setText("Mode: Shared Cart (#" + pin + ") • " + memberCount + " People Joined");
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
        };
        groupMembersRef.addValueEventListener(groupMembersListener);
    }

    private void switchToPersonalCart() {
        if (groupMembersRef != null && groupMembersListener != null) {
            groupMembersRef.removeEventListener(groupMembersListener);
        }
        activeGroupPin = null;
        tvModeIndicator.setText("Mode: Personal Cart (Tap 'Group PIN' for Roommate Sync)");
        currentCartTotal = 0.0;
        updateBudgetBanner();
        listenToCartTotal();
        Toast.makeText(this, "Switched to Personal Cart", Toast.LENGTH_SHORT).show();
    }

    private void checkBudgetAndAddToCart(Product clickedProduct) {
        String userId = firebaseHelper.getCurrentUserId();

        // If no budget is set, add directly without restrictions
        if (userBudget <= 0) {
            addItemToActiveCart(clickedProduct, userId, false);
            return;
        }

        // Always read the exact live cart total from Firebase before checking residual budget
        com.google.firebase.database.DatabaseReference checkRef;
        if (activeGroupPin != null && !activeGroupPin.isEmpty()) {
            checkRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("SharedCarts").child(activeGroupPin);
        } else if (userId != null) {
            checkRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("Carts").child(userId);
        } else {
            Toast.makeText(this, "Please log in first!", Toast.LENGTH_SHORT).show();
            return;
        }

        checkRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                double liveCartTotal = 0.0;
                for (com.google.firebase.database.DataSnapshot ds : snapshot.getChildren()) {
                    CartItem existing = ds.getValue(CartItem.class);
                    if (existing != null) {
                        liveCartTotal += existing.getPrice() * existing.getQuantity();
                    }
                }
                currentCartTotal = liveCartTotal;
                updateBudgetBanner();

                double residualBudget = userBudget - liveCartTotal;

                // Item fits cleanly inside the remaining residual budget -> Add immediately!
                if (clickedProduct.getPrice() <= residualBudget) {
                    addItemToActiveCart(clickedProduct, userId, false);
                    return;
                }

                // Item exceeds residual budget -> Find all products that fit within residualBudget
                showResidualSmartAlternatives(clickedProduct, residualBudget, userId);
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error checking cart: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showResidualSmartAlternatives(Product clickedProduct, double residualBudget, String userId) {
        double safeResidual = Math.max(0.0, residualBudget);
        List<Product> matchingAlternatives = new ArrayList<>();

        for (Product p : productList) {
            if (p != null
                    && p.getId() != null
                    && !p.getId().equals(clickedProduct.getId())
                    && p.getPrice() > 0
                    && p.getPrice() <= safeResidual) {
                matchingAlternatives.add(p);
            }
        }

        // Rank best-fitting items first:
        // 1. Same category items appear first
        // 2. Highest price within the residual budget appears first (best value fit, not always Salt)
        java.util.Collections.sort(matchingAlternatives, (p1, p2) -> {
            boolean p1SameCat = clickedProduct.getCategory() != null
                    && clickedProduct.getCategory().equalsIgnoreCase(p1.getCategory());
            boolean p2SameCat = clickedProduct.getCategory() != null
                    && clickedProduct.getCategory().equalsIgnoreCase(p2.getCategory());

            if (p1SameCat != p2SameCat) {
                return p1SameCat ? -1 : 1;
            }
            return Double.compare(p2.getPrice(), p1.getPrice());
        });

        if (!matchingAlternatives.isEmpty()) {
            String[] optionLabels = new String[matchingAlternatives.size()];
            for (int i = 0; i < matchingAlternatives.size(); i++) {
                Product alt = matchingAlternatives.get(i);
                String catTag = (alt.getCategory() != null) ? " [" + alt.getCategory() + "]" : "";
                optionLabels[i] = alt.getName() + catTag + " — ₹" + (int) alt.getPrice();
            }

            new AlertDialog.Builder(this)
                    .setTitle("💡 Residual Budget: ₹" + (int) safeResidual + " Left")
                    .setItems(optionLabels, (dialog, which) -> {
                        Product chosenAlt = matchingAlternatives.get(which);
                        addItemToActiveCart(chosenAlt, userId, true);
                    })
                    .setNeutralButton("Add Original (₹" + (int) clickedProduct.getPrice() + ")", (d, w) -> {
                        addItemToActiveCart(clickedProduct, userId, false);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("⚠️ Budget Limit Reached")
                    .setMessage("Only ₹" + (int) safeResidual + " left in your ₹" + (int) userBudget
                            + " budget.\n\n'" + clickedProduct.getName() + "' (₹" + (int) clickedProduct.getPrice()
                            + ") exceeds your remaining balance, and no items fit within ₹" + (int) safeResidual + ".")
                    .setPositiveButton("Add Anyway", (d, w) -> addItemToActiveCart(clickedProduct, userId, false))
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    private void addItemToActiveCart(Product product, String userId, boolean isSwap) {
        int existingQty = (product.getId() != null && cartQuantities.containsKey(product.getId()))
                ? cartQuantities.get(product.getId()) : 0;
        int newQty = existingQty + 1;

        CartItem item = new CartItem(product.getId(), product.getName(), product.getPrice(), newQty, userId);
        firebaseHelper.addToCartWithPin(item, activeGroupPin, new FirebaseHelper.ActionCallback() {
            @Override
            public void onSuccess() {
                String target = (activeGroupPin != null && !activeGroupPin.isEmpty())
                        ? "Shared Cart #" + activeGroupPin : "Personal Cart";
                String prefix = isSwap ? "Smart Pick: " : "";
                Toast.makeText(MainActivity.this, prefix + product.getName() + " (x" + newQty + ") in " + target, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String err) {
                Toast.makeText(MainActivity.this, err, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void decreaseProductQuantity(Product product, int currentQty) {
        if (product.getId() == null) return;
        String userId = firebaseHelper.getCurrentUserId();

        if (currentQty > 1) {
            int newQty = currentQty - 1;
            CartItem updated = new CartItem(product.getId(), product.getName(), product.getPrice(), newQty, userId);
            firebaseHelper.addToCartWithPin(updated, activeGroupPin, new FirebaseHelper.ActionCallback() {
                @Override
                public void onSuccess() {}

                @Override
                public void onError(String err) {
                    Toast.makeText(MainActivity.this, err, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            firebaseHelper.removeFromCartWithPin(product.getId(), activeGroupPin, new FirebaseHelper.ActionCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(MainActivity.this, product.getName() + " removed", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String err) {
                    Toast.makeText(MainActivity.this, err, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    private void scheduleExpiryCheck() {
        PeriodicWorkRequest expiryWorkRequest =
                new PeriodicWorkRequest.Builder(ExpiryWorker.class, 24, TimeUnit.HOURS).build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "PantryExpiryCheck",
                ExistingPeriodicWorkPolicy.KEEP,
                expiryWorkRequest
        );
    }

    private void showRecipesDialog() {
        firebaseHelper.fetchRecipes(new FirebaseHelper.RecipeFetchCallback() {
            @Override
            public void onSuccess(List<com.example.grocery.models.Recipe> recipeList) {
                if (recipeList.isEmpty()) {
                    Toast.makeText(MainActivity.this, "No recipes found in Firebase.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String[] recipeNames = new String[recipeList.size()];
                for (int i = 0; i < recipeList.size(); i++) {
                    recipeNames[i] = recipeList.get(i).getName() + " — " + recipeList.get(i).getDescription();
                }

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("🍲 Recipe-to-Cart (Tap to Add Ingredients)")
                        .setItems(recipeNames, (dialog, which) -> {
                            com.example.grocery.models.Recipe selected = recipeList.get(which);
                            firebaseHelper.addRecipeIngredientsToCart(
                                    selected.getIngredientIds(),
                                    activeGroupPin,
                                    new FirebaseHelper.ActionCallback() {
                                        @Override
                                        public void onSuccess() {
                                            Toast.makeText(MainActivity.this, "Added all ingredients for " + selected.getName() + "!", Toast.LENGTH_SHORT).show();
                                        }

                                        @Override
                                        public void onError(String errorMessage) {
                                            Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                                        }
                                    }
                            );
                        })
                        .setNegativeButton("Close", null)
                        .show();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(MainActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}