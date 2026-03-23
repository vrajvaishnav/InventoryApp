package com.example.inventory;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.inventory.databinding.ItemBillProductBinding;
import java.util.List;
import java.util.Locale;

public class BillingAdapter extends RecyclerView.Adapter<BillingAdapter.BillingViewHolder> {

    private List<Product> productList;
    private OnBillItemClickListener listener;

    public interface OnBillItemClickListener {
        void onAddToBill(Product product, int quantity);
    }

    public BillingAdapter(List<Product> productList, OnBillItemClickListener listener) {
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BillingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBillProductBinding binding = ItemBillProductBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new BillingViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BillingViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.binding.tvProductName.setText(product.getName());
        holder.binding.tvProductPrice.setText(String.format(Locale.getDefault(), "₹%.2f", product.getPrice()));
        holder.binding.tvAvailableStock.setText("Stock: " + product.getQuantity());

        holder.binding.btnAddBill.setOnClickListener(v -> {
            String qtyStr = holder.binding.etBillQuantity.getText().toString();
            if (!qtyStr.isEmpty()) {
                try {
                    int qty = Integer.parseInt(qtyStr);
                    listener.onAddToBill(product, qty);
                    holder.binding.etBillQuantity.setText("");
                } catch (NumberFormatException e) {
                    holder.binding.etBillQuantity.setError("Invalid number");
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class BillingViewHolder extends RecyclerView.ViewHolder {
        ItemBillProductBinding binding;

        public BillingViewHolder(ItemBillProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
