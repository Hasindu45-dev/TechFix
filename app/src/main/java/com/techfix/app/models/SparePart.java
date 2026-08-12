package com.techfix.app.models;

public class SparePart {
    private String partId;
    private String name;
    private int quantity;
    private double price;
    private String branchId;
    private String description;
    private String category;
    private int minimumStockLevel;
    private String imageURL;
    private long createdAt;
    private long updatedAt;

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

    public SparePart(String partId, String name, int quantity, double price, String branchId,
                     String description, String category, int minimumStockLevel, String imageURL,
                     long createdAt, long updatedAt) {
        this.partId = partId;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.branchId = branchId;
        this.description = description;
        this.category = category;
        this.minimumStockLevel = minimumStockLevel;
        this.imageURL = imageURL;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getMinimumStockLevel() {
        return minimumStockLevel;
    }

    public void setMinimumStockLevel(int minimumStockLevel) {
        this.minimumStockLevel = minimumStockLevel;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
