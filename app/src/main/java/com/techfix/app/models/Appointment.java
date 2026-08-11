package com.techfix.app.models;

public class Appointment {
    private String appointmentId;
    private String customerId;
    private String serviceId;
    private String deviceModel;
    private String problemDescription;
    private String imageURL;
    private String assignedBranch; // Branch ID (or name)
    private String assignedTechnician; // Technician ID (or name)
    private String status; // e.g. "Request Submitted", "Assigned to Branch", etc.
    private String date; // Preferred date formatted as string

    // Default constructor required for Firebase
    public Appointment() {
    }

    public Appointment(String appointmentId, String customerId, String serviceId, String deviceModel, 
                       String problemDescription, String imageURL, String assignedBranch, 
                       String assignedTechnician, String status, String date) {
        this.appointmentId = appointmentId;
        this.customerId = customerId;
        this.serviceId = serviceId;
        this.deviceModel = deviceModel;
        this.problemDescription = problemDescription;
        this.imageURL = imageURL;
        this.assignedBranch = assignedBranch;
        this.assignedTechnician = assignedTechnician;
        this.status = status;
        this.date = date;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public String getProblemDescription() {
        return problemDescription;
    }

    public void setProblemDescription(String problemDescription) {
        this.problemDescription = problemDescription;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public String getAssignedBranch() {
        return assignedBranch;
    }

    public void setAssignedBranch(String assignedBranch) {
        this.assignedBranch = assignedBranch;
    }

    public String getAssignedTechnician() {
        return assignedTechnician;
    }

    public void setAssignedTechnician(String assignedTechnician) {
        this.assignedTechnician = assignedTechnician;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
