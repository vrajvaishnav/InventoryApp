package com.example.inventory;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.inventory.databinding.ItemSaleBinding;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SalesAdapter extends RecyclerView.Adapter<SalesAdapter.SalesViewHolder> implements Filterable {

    private List<Sale> salesList;
    private List<Sale> salesListFull;
    private OnSaleClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

    public interface OnSaleClickListener {
        void onSaleClick(Sale sale);
    }

    public SalesAdapter(List<Sale> salesList, OnSaleClickListener listener) {
        this.salesList = salesList;
        this.salesListFull = new ArrayList<>(salesList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public SalesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSaleBinding binding = ItemSaleBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new SalesViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SalesViewHolder holder, int position) {
        Sale sale = salesList.get(position);
        holder.binding.tvBillId.setText("Bill ID: #" + (sale.getBillId() != null ? sale.getBillId().substring(Math.max(0, sale.getBillId().length() - 5)) : "N/A"));
        holder.binding.tvSaleDate.setText("Date: " + dateFormat.format(new Date(sale.getTimestamp())));
        holder.binding.tvSaleTotal.setText(String.format(Locale.getDefault(), "Total: ₹%.2f", sale.getTotalAmount()));

        holder.itemView.setOnClickListener(v -> listener.onSaleClick(sale));
    }

    @Override
    public int getItemCount() {
        return salesList.size();
    }

    public void updateList(List<Sale> newList) {
        this.salesList = newList;
        this.salesListFull = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @Override
    public Filter getFilter() {
        return salesFilter;
    }

    private Filter salesFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Sale> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(salesListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (Sale item : salesListFull) {
                    String dateStr = dateFormat.format(new Date(item.getTimestamp())).toLowerCase();
                    if (item.getBillId().toLowerCase().contains(filterPattern) || dateStr.contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            salesList.clear();
            if (results.values != null) {
                salesList.addAll((List) results.values);
            }
            notifyDataSetChanged();
        }
    };

    static class SalesViewHolder extends RecyclerView.ViewHolder {
        ItemSaleBinding binding;

        public SalesViewHolder(ItemSaleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
