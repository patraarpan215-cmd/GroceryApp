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

        // Use real product/produce photographs from their image sources where available.
        // Existing Firebase/local images remain as fallbacks for products without a matched source.
        String realImageUrl = getRealProductImageUrl(product.getName());
        int localImage = getLocalProductImage(product.getName());
        Object imageSource = realImageUrl != null ? realImageUrl
                : (localImage != 0 ? localImage : product.getImageUrl());
        Glide.with(context)
                .load(imageSource)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(localImage != 0 ? localImage : android.R.drawable.ic_menu_gallery)
                .centerCrop()
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


    /** Direct product-photo URLs for products visible in the FreshBasket catalogue. */
    private String getRealProductImageUrl(String productName) {
        if (productName == null) return null;
        String n = productName.toLowerCase(java.util.Locale.ROOT);

        // Original branded packaging photographs.
        if (n.contains("aashirvaad") || n.contains("atta") || n.contains("flour"))
            return "https://cdn.mafrservices.com/sys-master-root/h2a/h27/45262867071006/844600_main.jpg";
        if (n.contains("tata salt") || n.equals("salt") || n.contains("salt 1kg"))
            return "https://tiimg.tistatic.com/fp/1/008/011/essential-dietary-nutrient-right-amount-of-double-fortified-iodine-tata-salt-1-kg--200.jpg";
        if (n.contains("maggi") || n.contains("noodle"))
            return "https://cdn.grofers.com/da/cms-assets/cms/product/6d40482f-f382-41cd-92ce-55e5ec2f06aa.jpg";
        if (n.contains("amul") && n.contains("butter") || n.contains("butter"))
            return "https://image.aapkabazar.co/product/6884/1735032700982.png?type=png";
        if (n.contains("india gate") || n.contains("basmati rice"))
            return "https://www.sairas.de/cdn/shop/files/freepik__product-photography-of-a-5kg-bag-of-india-gate-bas__53787.png?v=1767730655&width=1946";
        if (n.contains("red label") || n.contains("brooke bond") || n.contains("tea"))
            return "https://www.bbassets.com/media/uploads/p/xl/266575_20-red-label-tea.jpg";
        if (n.contains("nescafe") || n.contains("coffee"))
            return "https://www.nescafe.com/in/sites/default/files/styles/pdp_banner_image/public/2025-05/IN_2025_Nescafe-Cl_180g-Jar-960x960-3D.png.webp?itok=fhVHMUDz";
        if (n.contains("paneer"))
            return "https://cdn.grofers.com/da/cms-assets/cms/product/b02b1507-6af1-4486-86d4-ac9c680ebcd1.jpg";
        if (n.contains("parle") || n.contains("parle-g"))
            return "https://cdn.dotpe.in/longtail/item_thumbnails/9367125/esQk4lsh-800-800.webp";
        if (n.contains("good day") || n.contains("britannia"))
            return "https://img08.weeecdn.net/product/image/158/812/7A2FAD6289C687CD.png%21c864x0_q80.auto";
        if (n.contains("lays") || n.contains("lay's"))
            return "https://www.vishalmegamart.com/dw/image/v2/BGHT_PRD/on/demandware.static/-/Sites-vmm-fmcg-master-catalog/default/dw023f1b34/images/large/1310019025.JPG?sh=900&sw=900";
        if (n.contains("bhujia"))
            return "https://www.bbassets.com/media/uploads/p/xl/401772_2-haldirams-bhujia-sev.jpg";
        if (n.contains("turmeric") || n.contains("haldi"))
            return "https://www.jiomart.com/images/product/original/490070031/mdh-haldi-turmeric-powder-100-g-product-images-o490070031-p490070031-0-202502271042.jpg?im=Resize%3D%281000%2C1000%29";
        if (n.contains("catch coriander") || n.contains("coriander powder") || n.contains("dhaniya powder"))
            return "https://cdn.grofers.com/app/images/products/sliding_image/18851a.jpg?ts=1688972648";
        if (n.contains("chicken"))
            return "https://cdn.grofers.com/da/cms-assets/cms/product/de6fc1b3-596c-4037-a532-8e1d19c90c53.jpg";

        // Real fresh-produce photographs.
        if (n.contains("onion"))
            return "https://www.mystore.in/s/62ea2c599d1398fa16dbae0a/657766183eb68cb4a6691b8e/on1kg-640x640.PNG";
        if (n.contains("potato"))
            return "https://www.sarasfoods.co.uk/cdn/shop/files/29f6c068-268e-4eaf-98ba-e3a4bb49e636_750x810.jpg?v=1745587452";
        if (n.contains("tomato"))
            return "https://greentomato.club/cdn/shop/products/GT-Vine-Tomatoes.png?v=1652866350";
        if (n.contains("chilli") || n.contains("chili"))
            return "https://easternfarmsllc.com/img/products/bd/green-chilli.jpg";
        if (n.contains("garlic"))
            return "https://img-global.cpcdn.com/steps/9b89f8fb98f60b5a/400x400cq80/photo.jpg";
        if (n.contains("ginger"))
            return "https://media.base44.com/images/public/699266c8c2f2e91c54efa8af/d1d2e18ab_ginger.webp";
        if (n.contains("lemon"))
            return "https://universdeschefs.fr/2146-large_default/zeste-citron.jpg";
        if (n.contains("moong dal") || n.contains("moongdal"))
            return "https://hariomatta.com/cdn/shop/files/mung_dhuli.png?v=1749712933&width=1946";
        if (n.contains("chana dal") || n.contains("toor dal"))
            return "https://cdn.grofers.com/da/cms-assets/cms/product/1ea3d897-b079-4558-828c-860d703edabd.jpg";
        if (n.contains("surf excel"))
            return "https://cdn.grofers.com/da/cms-assets/cms/product/005ddee6-33cd-44a6-8f06-df5989776b80.jpg";
        if (n.contains("milk"))
            return "https://image.cdn.shpy.in/430402/amul-taaza-toned-milky-milk-500-ml-product-images-o590000538-p590049234-0-202602161831-1775566404846.webp?format=webp&width=1200";
        if (n.contains("oil"))
            return "https://media.vyaparify.com/vcards/products/52185/IMG_4927.jpeg";
        if (n.contains("frooti"))
            return "https://www.starquik.com/cdn/shop/files/SQ102748_BOP_fc483ee8-2665-4ded-9fb4-98ee162f9523.jpg?v=1776846160&width=1445";
        if (n.contains("thums"))
            return "https://www.indiaathome.com.au/cdn/shop/files/Thums_up_750ml.png?v=1774420149";
        if (n.contains("everest") || n.contains("garam masala"))
            return "https://i.ebayimg.com/images/g/7GgAAOSwU~FjOWTC/s-l1200.jpg";
        if (n.contains("egg"))
            return "https://cdn.grofers.com/da/cms-assets/cms/product/rc-upload-1773201429611-142.jpg";

        // Additional real photographs for the remaining illustrated catalogue cards.
        if (n.contains("refined sugar") || n.equals("sugar") || n.contains("sugar 1kg"))
            return "https://www.jiomart.com/images/product/original/rvhpoqthyj/dhanraj-white-sugar-sachet-5g-x-200-sachet-product-images-orvhpoqthyj-p611859946-3-202507051640.jpg?im=Resize%3D%281000%2C1000%29";
        if (n.contains("moong"))
            return "https://hariomatta.com/cdn/shop/files/mung_dhuli.png?v=1749712933&width=1946";
        if (n.contains("bread"))
            return "https://media.sobeys.com/original/688957_EA_ml_primary_left_na_010222_VOILA_9a0246cb2874a9f8c8c0150f63ee994a19ee88ed.JPG";
        if (n.contains("apple"))
            return "https://img1.exportersindia.com/product_images/bc-full/2022/10/10973122/kashmiri-a-1664606768_6566289_1682814.jpeg";
        if (n.contains("banana"))
            return "https://www.jiomart.com/images/product/original/590001671/private-label-banana-robusta-1-kg-product-images-o590001671-p590001671-1-202410011654.jpg?im=Resize%3D%281000%2C1000%29";
        if (n.contains("pomegranate"))
            return "https://bakkali.app/cdn/shop/files/Pomegranate-1.49each-for-largemedium-99p-small-3-for-1.20-scaled_7c4f00b8-90de-4d0f-aae0-e888cef4d297.jpg?v=1718359081&width=1214";
        if (n.contains("watermelon"))
            return "https://cdn.shopify.com/s/files/1/0512/2542/8161/products/melon-watermelon-exoticfruitscouk-695603_1024x1024.jpg?v=1645488667";
        if (n.contains("fresh coriander") || n.contains("coriander 1 bunch") || n.contains("dhaniya leaves"))
            return "https://img-global.cpcdn.com/steps/9b89f8fb98f60b5a/400x400cq80/photo.jpg";
        return null;
    }

    /** Returns a bundled image for common grocery products, or 0 when no match exists. */
    private int getLocalProductImage(String productName) {
        if (productName == null) return 0;

        String name = productName.toLowerCase(java.util.Locale.ROOT);
        if (name.contains("atta") || name.contains("aashirvaad") || name.contains("flour")) {
            return R.drawable.product_atta;
        }
        if (name.contains("salt")) {
            return R.drawable.product_salt;
        }
        if (name.contains("maggi") || name.contains("noodle")) {
            return R.drawable.product_noodles;
        }
        if (name.contains("butter")) {
            return R.drawable.product_butter;
        }
        if (name.contains("milk")) {
            return R.drawable.product_milk;
        }
        if (name.contains("oil")) return R.drawable.product_oil;
        if (name.contains("rice")) return R.drawable.product_rice;
        if (name.contains("tea")) return R.drawable.product_tea;
        if (name.contains("coffee")) return R.drawable.product_coffee;
        if (name.contains("paneer")) return R.drawable.product_paneer;
        if (name.contains("onion")) return R.drawable.product_onions;
        if (name.contains("potato")) return R.drawable.product_potatoes;
        if (name.contains("tomato")) return R.drawable.product_tomatoes;
        if (name.contains("chilli")) return R.drawable.product_chillies;
        if (name.contains("coriander") || name.contains("dhaniya")) return R.drawable.product_coriander;
        if (name.contains("garlic")) return R.drawable.product_garlic;
        if (name.contains("ginger")) return R.drawable.product_ginger;
        if (name.contains("lemon")) return R.drawable.product_lemons;
        if (name.contains("apple")) return R.drawable.product_apples;
        if (name.contains("banana")) return R.drawable.product_bananas;
        if (name.contains("pomegranate")) return R.drawable.product_pomegranate;
        if (name.contains("watermelon")) return R.drawable.product_watermelon;
        if (name.contains("moong")) return R.drawable.product_moongdal;
        if (name.contains("chana dal")) return R.drawable.product_chanadal;
        if (name.contains("toor dal")) return R.drawable.product_toordal;
        if (name.contains("bread")) return R.drawable.product_bread;
        if (name.contains("egg")) return R.drawable.product_eggs;
        if (name.contains("chicken")) return R.drawable.product_chicken;
        if (name.contains("surf excel")) return R.drawable.product_surfexcel;
        if (name.contains("lays")) return R.drawable.product_lays;
        if (name.contains("parle")) return R.drawable.product_parleg;
        if (name.contains("good day")) return R.drawable.product_goodday;
        if (name.contains("bhujia")) return R.drawable.product_bhujia;
        if (name.contains("frooti")) return R.drawable.product_frooti;
        if (name.contains("turmeric")) return R.drawable.product_turmeric;
        if (name.contains("dhaniya")) return R.drawable.product_dhaniya;
        if (name.contains("masala")) return R.drawable.product_masala;
        if (name.contains("sugar")) return R.drawable.product_sugar;
        if (name.contains("thums")) return R.drawable.product_thumsup;
        return 0;
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