package com.example.inventory;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.inventory.databinding.ActivityBillingBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BillingActivity extends AppCompatActivity implements BillingAdapter.OnBillItemClickListener {

    private ActivityBillingBinding binding;
    private DatabaseReference productsRef;
    private DatabaseReference salesRef;
    private BillingAdapter adapter;
    private List<Product> productList;
    private Map<String, Integer> cart = new HashMap<>();
    private double totalAmount = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBillingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        productsRef = FirebaseDatabase.getInstance().getReference("products");
        salesRef = FirebaseDatabase.getInstance().getReference("sales");
        
        productList = new ArrayList<>();
        adapter = new BillingAdapter(productList, this);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        fetchProducts();

        binding.btnGenerateBill.setOnClickListener(v -> generateBill());
    }

    private void fetchProducts() {
        productsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                productList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Product product = dataSnapshot.getValue(Product.class);
                    if (product != null && product.getQuantity() > 0) {
                        productList.add(product);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(BillingActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAddToBill(Product product, int quantity) {
        if (quantity <= 0) {
            Toast.makeText(this, "Quantity must be greater than 0", Toast.LENGTH_SHORT).show();
            return;
        }

        if (quantity > product.getQuantity()) {
            Toast.makeText(this, "Not enough stock", Toast.LENGTH_SHORT).show();
            return;
        }

        int currentQty = 0;
        if (cart.containsKey(product.getId())) {
            currentQty = cart.get(product.getId());
        }
        
        if (currentQty + quantity > product.getQuantity()) {
            Toast.makeText(this, "Total quantity in cart exceeds stock", Toast.LENGTH_SHORT).show();
            return;
        }

        cart.put(product.getId(), currentQty + quantity);
        updateCartSummary();
        Snackbar.make(binding.getRoot(), "Added to cart", Snackbar.LENGTH_SHORT).show();
    }

    private void updateCartSummary() {
        if (cart.isEmpty()) {
            binding.tvCartItems.setText("No items added");
            binding.tvTotalAmount.setText("₹0.00");
            totalAmount = 0.0;
            return;
        }

        StringBuilder summary = new StringBuilder();
        totalAmount = 0.0;

        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            Product p = findProductById(entry.getKey());
            if (p != null) {
                int qty = entry.getValue();
                double price = p.getPrice() * qty;
                totalAmount += price;
                summary.append(p.getName()).append(" (x").append(qty).append("), ");
            }
        }

        if (summary.length() > 2) summary.setLength(summary.length() - 2);
        binding.tvCartItems.setText(summary.toString());
        binding.tvTotalAmount.setText(String.format(Locale.getDefault(), "₹%.2f", totalAmount));
    }

    private Product findProductById(String id) {
        for (Product p : productList) {
            if (p.getId().equals(id)) return p;
        }
        return null;
    }

    private void generateBill() {
        String customerName = binding.etCustomerName.getText().toString().trim();
        String customerPhone = binding.etCustomerPhone.getText().toString().trim();
        String customerAddress = binding.etCustomerAddress.getText().toString().trim();

        if (customerName.isEmpty()) {
            binding.etCustomerName.setError("Name is required");
            return;
        }
        if (customerPhone.length() != 10) {
            binding.etCustomerPhone.setError("Enter 10 digit number");
            return;
        }
        if (customerAddress.isEmpty()) {
            binding.etCustomerAddress.setError("Address is required");
            return;
        }

        if (cart.isEmpty()) {
            Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String billId = salesRef.push().getKey();
        List<SaleItem> saleItems = new ArrayList<>();
        
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            String productId = entry.getKey();
            int billedQty = entry.getValue();
            Product p = findProductById(productId);
            if (p != null) {
                // Update product stock
                int newQty = p.getQuantity() - billedQty;
                productsRef.child(productId).child("quantity").setValue(newQty);
                
                // Add to sale items
                saleItems.add(new SaleItem(p.getName(), billedQty, p.getPrice()));
            }
        }

        Sale sale = new Sale(billId, System.currentTimeMillis(), totalAmount, saleItems, 
                           customerName, customerPhone, customerAddress);

        if (billId != null) {
            salesRef.child(billId).setValue(sale).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    cart.clear();
                    updateCartSummary();
                    Toast.makeText(this, "Bill Generated & Saved!", Toast.LENGTH_LONG).show();
                    finish(); // Go back to main
                }
            });
        }
    }
}
