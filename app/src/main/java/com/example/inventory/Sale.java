package com.example.inventory;

import java.util.List;

public class Sale {
    private String billId;
    private long timestamp;
    private double totalAmount;
    private List<SaleItem> items;
    private String customerName;
    private String customerPhone;
    private String customerAddress;

    public Sale() {
    }

    public Sale(String billId, long timestamp, double totalAmount, List<SaleItem> items, 
                String customerName, String customerPhone, String customerAddress) {
        this.billId = billId;
        this.timestamp = timestamp;
        this.totalAmount = totalAmount;
        this.items = items;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.customerAddress = customerAddress;
    }

    public String getBillId() {
        return billId;
    }

    public void setBillId(String billId) {
        this.billId = billId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public List<SaleItem> getItems() {
        return items;
    }

    public void setItems(List<SaleItem> items) {
        this.items = items;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public void setCustomerAddress(String customerAddress) {
        this.customerAddress = customerAddress;
    }
}
