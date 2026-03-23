package com.example.inventory;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.inventory.databinding.ActivityBillDetailBinding;
import com.example.inventory.databinding.DialogEditProductBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BillDetailActivity extends AppCompatActivity {

    private ActivityBillDetailBinding binding;
    private DatabaseReference salesRef;
    private DatabaseReference productsRef;
    private String billId;
    private Sale currentSale;
    private BillItemAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBillDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        billId = getIntent().getStringExtra("billId");
        salesRef = FirebaseDatabase.getInstance().getReference("sales").child(billId);
        productsRef = FirebaseDatabase.getInstance().getReference("products");

        binding.rvBillItems.setLayoutManager(new LinearLayoutManager(this));

        fetchBillDetails();

        binding.btnDeleteBill.setOnClickListener(v -> confirmDelete());
        binding.btnEditBill.setOnClickListener(v -> showEditDialog());
        
        binding.ivCallCustomer.setOnClickListener(v -> {
            if (currentSale != null && currentSale.getCustomerPhone() != null) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + currentSale.getCustomerPhone()));
                startActivity(intent);
            }
        });

        binding.ivCopyAddress.setOnClickListener(v -> {
            if (currentSale != null && currentSale.getCustomerAddress() != null) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Address", currentSale.getCustomerAddress());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Address copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchBillDetails() {
        salesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentSale = snapshot.getValue(Sale.class);
                if (currentSale != null) {
                    displaySale(currentSale);
                } else {
                    finish(); // If bill is deleted
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(BillDetailActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displaySale(Sale sale) {
        binding.tvDetailCustomerName.setText("Name: " + sale.getCustomerName());
        binding.tvDetailCustomerPhone.setText("Phone: " + sale.getCustomerPhone());
        binding.tvDetailCustomerAddress.setText("Address: " + sale.getCustomerAddress());
        
        binding.tvDetailBillId.setText("BILL #" + (sale.getBillId() != null ? sale.getBillId().substring(Math.max(0, sale.getBillId().length() - 5)) : "N/A"));
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        binding.tvDetailDate.setText("Date: " + dateFormat.format(new Date(sale.getTimestamp())));
        binding.tvDetailTotal.setText(String.format(Locale.getDefault(), "₹%.2f", sale.getTotalAmount()));

        adapter = new BillItemAdapter(sale.getItems());
        binding.rvBillItems.setAdapter(adapter);
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Bill")
                .setMessage("Delete this bill and RESTORE product stock?")
                .setPositiveButton("Restore & Delete", (dialog, which) -> restoreStockAndDelete())
                .setNeutralButton("Just Delete", (dialog, which) -> deleteBillOnly())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteBillOnly() {
        salesRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Bill deleted", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void restoreStockAndDelete() {
        // Restore stock for each item
        for (SaleItem item : currentSale.getItems()) {
            productsRef.orderByChild("name").equalTo(item.getProductName()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        Product p = ds.getValue(Product.class);
                        if (p != null) {
                            int newQty = p.getQuantity() + item.getQuantity();
                            ds.getRef().child("quantity").setValue(newQty);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
        deleteBillOnly();
    }

    private void showEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Bill Items");

        List<SaleItem> tempItems = new ArrayList<>(currentSale.getItems());
        String[] itemNames = new String[tempItems.size()];
        for (int i = 0; i < tempItems.size(); i++) {
            itemNames[i] = tempItems.get(i).getProductName() + " (Qty: " + tempItems.get(i).getQuantity() + ")";
        }

        builder.setItems(itemNames, (dialog, which) -> {
            SaleItem selectedItem = tempItems.get(which);
            showQuantityEditDialog(selectedItem, which);
        });

        builder.setNegativeButton("Close", null);
        builder.show();
    }

    private void showQuantityEditDialog(SaleItem item, int index) {
        DialogEditProductBinding dialogBinding = DialogEditProductBinding.inflate(LayoutInflater.from(this));
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogBinding.getRoot());

        dialogBinding.tvDialogTitle.setText("Edit " + item.getProductName() + " Quantity");
        dialogBinding.etProductName.setVisibility(android.view.View.GONE); 
        dialogBinding.etProductPrice.setVisibility(android.view.View.GONE);
        dialogBinding.etProductQuantity.setHint("Quantity in Bill");
        dialogBinding.etProductQuantity.setText(String.valueOf(item.getQuantity()));

        builder.setPositiveButton("Update Bill", (dialog, which) -> {
            String qtyStr = dialogBinding.etProductQuantity.getText().toString();
            if (!qtyStr.isEmpty()) {
                int newQty = Integer.parseInt(qtyStr);
                if (newQty > 0) {
                    item.setQuantity(newQty);
                    recalculateTotalAndSave();
                }
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void recalculateTotalAndSave() {
        double newTotal = 0;
        for (SaleItem item : currentSale.getItems()) {
            newTotal += item.getPrice() * item.getQuantity();
        }
        currentSale.setTotalAmount(newTotal);
        salesRef.setValue(currentSale).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Bill updated", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
