package com.example.inventory;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.inventory.databinding.ActivityMainBinding;
import com.example.inventory.databinding.DialogEditProductBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ProductAdapter.OnProductClickListener {

    private ActivityMainBinding binding;
    private DatabaseReference databaseReference;
    private ProductAdapter adapter;
    private List<Product> productList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup the modern branded toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        databaseReference = FirebaseDatabase.getInstance().getReference("products");
        productList = new ArrayList<>();
        adapter = new ProductAdapter(productList, this);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        fetchProducts();

        binding.fabAdd.setOnClickListener(v -> showProductDialog(null));

        binding.btnGoToBilling.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BillingActivity.class);
            startActivity(intent);
        });

        binding.btnGoToHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SalesHistoryActivity.class);
            startActivity(intent);
        });

        setupSearch();
    }

    private void fetchProducts() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                productList.clear();
                int totalStock = 0;
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Product product = dataSnapshot.getValue(Product.class);
                    if (product != null) {
                        productList.add(product);
                        totalStock += product.getQuantity();
                    }
                }
                adapter.updateList(productList);
                binding.tvTotalStock.setText(String.valueOf(totalStock));
                
                if (productList.isEmpty()) {
                    binding.tvEmptyState.setVisibility(View.VISIBLE);
                } else {
                    binding.tvEmptyState.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void showProductDialog(Product product) {
        DialogEditProductBinding dialogBinding = DialogEditProductBinding.inflate(LayoutInflater.from(this));
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogBinding.getRoot());

        if (product != null) {
            dialogBinding.tvDialogTitle.setText("Edit Product");
            dialogBinding.etProductName.setText(product.getName());
            dialogBinding.etProductQuantity.setText(String.valueOf(product.getQuantity()));
            dialogBinding.etProductPrice.setText(String.valueOf(product.getPrice()));
        } else {
            dialogBinding.tvDialogTitle.setText("Add New Product");
        }

        builder.setPositiveButton(product != null ? "Update" : "Add", (dialog, which) -> {
            String name = dialogBinding.etProductName.getText().toString().trim();
            String qtyStr = dialogBinding.etProductQuantity.getText().toString().trim();
            String priceStr = dialogBinding.etProductPrice.getText().toString().trim();

            if (name.isEmpty() || qtyStr.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int quantity = Integer.parseInt(qtyStr);
                double price = Double.parseDouble(priceStr);

                if (quantity <= 0 || price < 0) {
                    Toast.makeText(this, "Invalid values", Toast.LENGTH_SHORT).show();
                    return;
                }

                String id = (product != null) ? product.getId() : databaseReference.push().getKey();
                Product newProduct = new Product(id, name, quantity, price);

                if (id != null) {
                    databaseReference.child(id).setValue(newProduct).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String msg = (product != null) ? "Product updated" : "Product added";
                            Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    @Override
    public void onEditClick(Product product) {
        showProductDialog(product);
    }

    @Override
    public void onDeleteClick(Product product) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete " + product.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    databaseReference.child(product.getId()).removeValue().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Snackbar.make(binding.getRoot(), "Product deleted", Snackbar.LENGTH_LONG)
                                    .setAction("Undo", v -> databaseReference.child(product.getId()).setValue(product))
                                    .show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
