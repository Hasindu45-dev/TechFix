package com.techfix.app.models;

public class Technician {
    private String technicianId;
    private String name;
    private String specialization; // e.g. "Laptop", "Mobile"
    private String branchId;
    private boolean availability;

    // Default constructor required for Firebase
    public Technician() {
    }

    public Technician(String technicianId, String name, String specialization, String branchId, boolean availability) {
        this.technicianId = technicianId;
        this.name = name;
        this.specialization = specialization;
        this.branchId = branchId;
        this.availability = availability;
    }

    public String getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(String technicianId) {
        this.technicianId = technicianId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    public boolean isAvailability() {
        return availability;
    }

    public void setAvailability(boolean availability) {
        this.availability = availability;
    }
}
