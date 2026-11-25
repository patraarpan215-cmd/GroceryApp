package com.example.grocery;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.grocery.models.CartItem;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private final Context context;
    private final List<CartItem> cartItemList;
    private boolean isSharedMode;

    private String groupPin;

    public CartAdapter(Context context, List<CartItem> cartItemList, String groupPin) {
        this.context = context;
        this.cartItemList = cartItemList;
        this.groupPin = groupPin;
    }

    // Allows CartActivity to toggle the roommate tag on/off dynamically
    public void setSharedMode(boolean sharedMode) {
        this.isSharedMode = sharedMode;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItemList.get(position);

        int unitPrice = (int) item.getPrice();
        int quantity = item.getQuantity();
        int rowTotal = unitPrice * quantity; // Fixed missing '*' symbol

        // 1. Set product name, quantity breakdown, and row subtotal in Rupees
        holder.tvItemName.setText(item.getProductName());
        holder.tvItemQty.setText("Qty: " + quantity + "  ×  ₹" + unitPrice);
        holder.tvItemTotal.setText("₹" + rowTotal);

        // 2. Show which roommate added the item when in Shared Cart Mode
        if (isSharedMode && item.getAddedByUserId() != null && !item.getAddedByUserId().isEmpty()) {
            holder.tvAddedBy.setVisibility(View.VISIBLE);
            String shortId = item.getAddedByUserId().length() > 6
                    ? item.getAddedByUserId().substring(0, 6)
                    : item.getAddedByUserId();
            holder.tvAddedBy.setText("Added by: User_" + shortId);
        } else {
            holder.tvAddedBy.setVisibility(View.GONE);
        }

        // 3. Remove item from Firebase Cart when delete icon is tapped
        holder.btnRemoveItem.setOnClickListener(v -> {
            new com.example.grocery.utils.FirebaseHelper().removeFromCart(
                    item.getProductId(),
                    groupPin,
                    new com.example.grocery.utils.FirebaseHelper.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            android.widget.Toast.makeText(context, item.getProductName() + " removed", android.widget.Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            android.widget.Toast.makeText(context, "Error: " + errorMessage, android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        });
    }

    @Override
    public int getItemCount() {
        return cartItemList.size();
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemName, tvItemQty, tvAddedBy, tvItemTotal;
        ImageButton btnRemoveItem;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tvCartItemName);
            tvItemQty = itemView.findViewById(R.id.tvCartItemQty);
            tvAddedBy = itemView.findViewById(R.id.tvCartAddedBy);
            tvItemTotal = itemView.findViewById(R.id.tvCartItemTotal);
            btnRemoveItem = itemView.findViewById(R.id.btnRemoveItem);
        }
    }


}