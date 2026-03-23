package com.example.inventory;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.inventory.databinding.ItemBillDetailBinding;
import java.util.List;
import java.util.Locale;

public class BillItemAdapter extends RecyclerView.Adapter<BillItemAdapter.BillItemViewHolder> {

    private List<SaleItem> items;

    public BillItemAdapter(List<SaleItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public BillItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBillDetailBinding binding = ItemBillDetailBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new BillItemViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BillItemViewHolder holder, int position) {
        SaleItem item = items.get(position);
        holder.binding.tvItemName.setText(item.getProductName());
        holder.binding.tvItemDetails.setText(String.format(Locale.getDefault(), "₹%.2f x %d", item.getPrice(), item.getQuantity()));
        holder.binding.tvItemSubtotal.setText(String.format(Locale.getDefault(), "₹%.2f", item.getPrice() * item.getQuantity()));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class BillItemViewHolder extends RecyclerView.ViewHolder {
        ItemBillDetailBinding binding;

        public BillItemViewHolder(ItemBillDetailBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
