package com.example.grocery;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.grocery.models.Product;

import java.util.List;
import java.util.Map;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private final Context context;
    private final List<Product> productList;
    private final Map<String, Integer> cartQuantities;
    private final OnProductActionListener listener;

    public interface OnProductActionListener {
        void onIncreaseQuantity(Product product, int currentQty);
        void onDecreaseQuantity(Product product, int currentQty);
    }

    public ProductAdapter(Context context, List<Product> productList,
                          Map<String, Integer> cartQuantities, OnProductActionListener listener) {
        this.context = context;
        this.productList = productList;
        this.cartQuantities = cartQuantities;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.tvName.setText(product.getName());
        holder.tvPrice.setText("₹" + (int) product.getPrice());

        Glide.with(context)
                .load(product.getImageUrl())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.imgProduct);

        int qty = (product.getId() != null && cartQuantities.containsKey(product.getId()))
                ? cartQuantities.get(product.getId()) : 0;

        if (qty > 0) {
            holder.btnAddToCart.setVisibility(View.GONE);
            holder.layoutQuantityStepper.setVisibility(View.VISIBLE);
            holder.tvItemQuantity.setText(String.valueOf(qty));
        } else {
            holder.btnAddToCart.setVisibility(View.VISIBLE);
            holder.layoutQuantityStepper.setVisibility(View.GONE);
        }

        holder.btnAddToCart.setOnClickListener(v -> listener.onIncreaseQuantity(product, 0));
        holder.btnIncreaseQty.setOnClickListener(v -> listener.onIncreaseQuantity(product, qty));
        holder.btnDecreaseQty.setOnClickListener(v -> listener.onDecreaseQuantity(product, qty));
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvItemQuantity;
        ImageView imgProduct;
        Button btnAddToCart, btnIncreaseQty, btnDecreaseQty;
        LinearLayout layoutQuantityStepper;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvPrice = itemView.findViewById(R.id.tvProductPrice);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            btnAddToCart = itemView.findViewById(R.id.btnAddToCart);
            layoutQuantityStepper = itemView.findViewById(R.id.layoutQuantityStepper);
            btnDecreaseQty = itemView.findViewById(R.id.btnDecreaseQty);
            tvItemQuantity = itemView.findViewById(R.id.tvItemQuantity);
            btnIncreaseQty = itemView.findViewById(R.id.btnIncreaseQty);
        }
    }
}