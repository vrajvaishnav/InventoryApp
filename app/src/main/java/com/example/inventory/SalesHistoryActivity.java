package com.example.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.inventory.databinding.ActivitySalesHistoryBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SalesHistoryActivity extends AppCompatActivity implements SalesAdapter.OnSaleClickListener {

    private ActivitySalesHistoryBinding binding;
    private DatabaseReference salesRef;
    private SalesAdapter adapter;
    private List<Sale> salesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySalesHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        salesRef = FirebaseDatabase.getInstance().getReference("sales");
        salesList = new ArrayList<>();
        adapter = new SalesAdapter(salesList, this);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        fetchSales();
        setupSearch();
    }

    private void fetchSales() {
        salesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                salesList.clear();
                double totalRevenue = 0;
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Sale sale = dataSnapshot.getValue(Sale.class);
                    if (sale != null) {
                        salesList.add(sale);
                        totalRevenue += sale.getTotalAmount();
                    }
                }
                // Sort by timestamp descending (latest first) using Collections.sort for API 23 compatibility
                Collections.sort(salesList, (s1, s2) -> Long.compare(s2.getTimestamp(), s1.getTimestamp()));
                
                adapter.updateList(salesList);
                binding.tvTotalRevenue.setText(String.format(Locale.getDefault(), "₹%.2f", totalRevenue));
                
                if (salesList.isEmpty()) {
                    binding.tvEmptyHistory.setVisibility(View.VISIBLE);
                } else {
                    binding.tvEmptyHistory.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SalesHistoryActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSearch() {
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return false;
            }
        });
    }

    @Override
    public void onSaleClick(Sale sale) {
        Intent intent = new Intent(this, BillDetailActivity.class);
        intent.putExtra("billId", sale.getBillId());
        startActivity(intent);
    }
}
