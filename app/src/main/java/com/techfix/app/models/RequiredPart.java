package com.techfix.app.models;

public class RequiredPart {
    private String partName;
    private int quantity;

    public RequiredPart() {
    }

    public RequiredPart(String partName, int quantity) {
        this.partName = partName;
        this.quantity = quantity;
    }

    public String getPartName() {
        return partName;
    }

    public void setPartName(String partName) {
        this.partName = partName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
