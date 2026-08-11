package com.techfix.app.models;

public class SparePart {
    private String partId;
    private String name;
    private int quantity;
    private double price;
    private String branchId;

    // Default constructor required for Firebase
    public SparePart() {
    }

    public SparePart(String partId, String name, int quantity, double price, String branchId) {
        this.partId = partId;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.branchId = branchId;
    }

    public String getPartId() {
        return partId;
    }

    public void setPartId(String partId) {
        this.partId = partId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }
}
