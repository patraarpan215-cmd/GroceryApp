package com.example.grocery;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.grocery.models.CartItem;
import com.example.grocery.utils.FirebaseHelper;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class CartActivity extends AppCompatActivity {

    private RecyclerView recyclerViewCart;
    private TextView tvCartHeaderTitle, tvEmptyCart, tvCartGrandTotal;
    private MaterialButton btnBackToHome, btnCheckout;

    private CartAdapter cartAdapter;
    private final List<CartItem> cartItemList = new ArrayList<>();
    private FirebaseHelper firebaseHelper;

    private String groupPin = null;
    private boolean isSharedMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        // 1. Connect UI elements from activity_cart.xml
        recyclerViewCart = findViewById(R.id.recyclerViewCart);
        tvCartHeaderTitle = findViewById(R.id.tvCartHeaderTitle);
        tvEmptyCart = findViewById(R.id.tvEmptyCart);
        tvCartGrandTotal = findViewById(R.id.tvCartGrandTotal);
        btnBackToHome = findViewById(R.id.btnBackToHome);
        btnCheckout = findViewById(R.id.btnCheckout);

        firebaseHelper = new FirebaseHelper();

        // 2. Check if MainActivity passed a Roommate Group PIN
        groupPin = getIntent().getStringExtra("GROUP_PIN");
        isSharedMode = (groupPin != null && !groupPin.isEmpty());

        if (isSharedMode) {
            tvCartHeaderTitle.setText("Shared Cart (#" + groupPin + ")");
        } else {
            tvCartHeaderTitle.setText("My Shopping Cart");
        }

        // 3. Set up vertical list for cart rows
        recyclerViewCart.setLayoutManager(new LinearLayoutManager(this));
        cartAdapter = new CartAdapter(this, cartItemList, groupPin);
        recyclerViewCart.setAdapter(cartAdapter);

        // 4. Listen for live cart updates from Firebase
        loadCartItems();

        // 5. Button Click Listeners
        btnBackToHome.setOnClickListener(v -> finish());
        btnCheckout.setOnClickListener(v -> handleCheckout());
    }

    private void loadCartItems() {
        ValueEventListener cartListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                cartItemList.clear();
                int grandTotal = 0;

                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    CartItem item = itemSnapshot.getValue(CartItem.class);
                    if (item != null) {
                        cartItemList.add(item);
                        grandTotal += (int) (item.getPrice() * item.getQuantity());
                    }
                }

                cartAdapter.notifyDataSetChanged();
                tvCartGrandTotal.setText("₹" + grandTotal);

                // Show empty state text if no items exist
                if (cartItemList.isEmpty()) {
                    tvEmptyCart.setVisibility(View.VISIBLE);
                    recyclerViewCart.setVisibility(View.GONE);
                } else {
                    tvEmptyCart.setVisibility(View.GONE);
                    recyclerViewCart.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CartActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        if (isSharedMode) {
            firebaseHelper.listenToSharedCart(groupPin, cartListener);
        } else {
            String userId = firebaseHelper.getCurrentUserId();
            if (userId != null) {
                FirebaseDatabase.getInstance().getReference("Carts").child(userId)
                        .addValueEventListener(cartListener);
            }
        }
    }

    private void handleCheckout() {
        if (cartItemList.isEmpty()) {
            Toast.makeText(this, "Your cart is empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseHelper.syncCartToPantry(new java.util.ArrayList<>(cartItemList), null);
        if (isSharedMode) {
            // Archive shared order and clear the shared room cart
            String orderId = "shared_order_" + System.currentTimeMillis();
            // Sync purchased items to Digital Pantry with shelf-life expiry dates

            FirebaseDatabase.getInstance().getReference("Orders").child("Shared_" + groupPin).child(orderId)
                    .setValue(cartItemList)
                    .addOnSuccessListener(aVoid -> {
                        FirebaseDatabase.getInstance().getReference("SharedCarts").child(groupPin).removeValue();
                        Toast.makeText(this, "Shared Order Placed Successfully!", Toast.LENGTH_LONG).show();
                        finish();
                    });
        } else {
            // Use your backend processCheckout() method for personal cart
            firebaseHelper.processCheckout(new FirebaseHelper.ActionCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(CartActivity.this, "Order Placed Successfully!", Toast.LENGTH_LONG).show();
                    finish();
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(CartActivity.this, "Checkout failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void initiateCheckout() {
        if (cartItemList == null || cartItemList.isEmpty()) {
            Toast.makeText(this, "Your cart is empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            Toast.makeText(this, "Please log in first!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Personal Cart Checkout (No Split-Bill needed)
        // Personal Cart Checkout (No Split-Bill needed)
        if (groupPin == null || groupPin.isEmpty()) {
            double grandTotal = 0.0;
            for (CartItem item : cartItemList) {
                grandTotal += item.getPrice() * item.getQuantity();
            }

            final double finalPersonalTotal = grandTotal;
            final String finalUserId = currentUserId;

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Confirm Personal Order")
                    .setMessage("Items: " + cartItemList.size() + "\nTotal Payable: ₹" + (int) finalPersonalTotal
                            + "\n\nItems will be synced to your Digital Pantry for expiry tracking.")
                    .setPositiveButton("Place Order", (d, w) -> finalizeOrderAndSyncPantry(finalUserId, finalPersonalTotal, null))
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }

        // Group PIN Checkout: Fetch joined member names & calculate Split-Bill
        com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("SharedCartMembers").child(groupPin)
                .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                        java.util.Map<String, String> memberNames = new java.util.HashMap<>();
                        for (com.google.firebase.database.DataSnapshot ds : snapshot.getChildren()) {
                            String name = ds.getValue(String.class);
                            if (ds.getKey() != null && name != null) {
                                memberNames.put(ds.getKey(), name);
                            }
                        }

                        // Calculate individual share per contributor
                        java.util.Map<String, Double> splitTotals = new java.util.HashMap<>();
                        double grandTotal = 0.0;

                        for (CartItem item : cartItemList) {
                            double itemTotal = item.getPrice() * item.getQuantity();
                            grandTotal += itemTotal;

                            // Note: If your CartItem getter is getAddedByUserId(), replace getUserId() below
                            String contributorId = (item.getAddedByUserId() != null) ? item.getAddedByUserId() : currentUserId;
                            double prev = splitTotals.containsKey(contributorId) ? splitTotals.get(contributorId) : 0.0;
                            splitTotals.put(contributorId, prev + itemTotal);
                        }

                        int totalJoined = Math.max(memberNames.size(), splitTotals.size());
                        int totalContributors = splitTotals.size();

                        StringBuilder summary = new StringBuilder();
                        summary.append("Group PIN: #").append(groupPin).append("\n");
                        summary.append("People in Group: ").append(totalJoined)
                                .append(" (").append(totalContributors).append(" added items)\n");
                        summary.append("────────────────────────\n");

                        for (java.util.Map.Entry<String, Double> entry : splitTotals.entrySet()) {
                            String uid = entry.getKey();
                            String label = memberNames.containsKey(uid) ? memberNames.get(uid) : "Roommate";
                            if (uid.equals(currentUserId)) {
                                label += " (You)";
                            }
                            summary.append("• ").append(label).append(": ₹").append((int) Math.round(entry.getValue())).append("\n");
                        }

                        summary.append("────────────────────────\n");
                        summary.append("Grand Total: ₹").append((int) Math.round(grandTotal));

                        final double finalTotal = grandTotal;
                        new androidx.appcompat.app.AlertDialog.Builder(CartActivity.this)
                                .setTitle("🧾 Roommate Split-Bill Summary")
                                .setMessage(summary.toString())
                                .setPositiveButton("Confirm & Place Order", (d, w) ->
                                        finalizeOrderAndSyncPantry(currentUserId, finalTotal, splitTotals))
                                .setNegativeButton("Cancel", null)
                                .show();
                    }

                    @Override
                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                        Toast.makeText(CartActivity.this, "Error loading group members", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void finalizeOrderAndSyncPantry(String currentUserId, double grandTotal,
                                            java.util.Map<String, Double> splitTotals) {
        com.google.firebase.database.DatabaseReference rootRef =
                com.google.firebase.database.FirebaseDatabase.getInstance().getReference();

        String orderId = rootRef.child("Orders").push().getKey();
        if (orderId == null) return;

        // 1. Save Order Record
        java.util.Map<String, Object> orderData = new java.util.HashMap<>();
        orderData.put("orderId", orderId);
        orderData.put("placedBy", currentUserId);
        orderData.put("groupPin", groupPin != null ? groupPin : "PERSONAL");
        orderData.put("totalAmount", grandTotal);
        orderData.put("timestamp", System.currentTimeMillis());
        orderData.put("items", cartItemList);
        if (splitTotals != null) {
            orderData.put("splitBreakdown", splitTotals);
        }
        rootRef.child("Orders").child(orderId).setValue(orderData);

        // 2. Sync Purchased Items to Digital Pantry with Shelf-Life Expiry
        long now = System.currentTimeMillis();
        long oneDayMillis = 24L * 60L * 60L * 1000L;

        rootRef.child("Products").addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot productSnap) {
                for (CartItem item : cartItemList) {
                    int shelfLifeDays = 7; // Safe default if product has no shelfLifeDays set
                    if (item.getProductId() != null && productSnap.hasChild(item.getProductId())) {
                        Integer days = productSnap.child(item.getProductId())
                                .child("shelfLifeDays").getValue(Integer.class);
                        if (days != null && days > 0) {
                            shelfLifeDays = days;
                        }
                    }

                    long expiryTimestamp = now + (shelfLifeDays * oneDayMillis);
                    java.util.Map<String, Object> pantryItem = new java.util.HashMap<>();
                    pantryItem.put("productId", item.getProductId());
                    pantryItem.put("productName", item.getProductName());
                    pantryItem.put("quantity", item.getQuantity());
                    pantryItem.put("purchasedAt", now);
                    pantryItem.put("expiryTimestamp", expiryTimestamp);

                    if (item.getProductId() != null) {
                        rootRef.child("Pantry").child(currentUserId).child(item.getProductId()).setValue(pantryItem);
                    }
                }

                // 3. Clear Active Cart (SharedCart or Personal Cart)
                com.google.firebase.database.DatabaseReference cartToClear =
                        (groupPin != null && !groupPin.isEmpty())
                                ? rootRef.child("SharedCarts").child(groupPin)
                                : rootRef.child("Carts").child(currentUserId);

                cartToClear.removeValue().addOnSuccessListener(aVoid -> {
                    Toast.makeText(CartActivity.this, "Order placed & synced to Digital Pantry!", Toast.LENGTH_LONG).show();
                    finish();
                });
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
        });
    }
}