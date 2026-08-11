package com.techfix.app.models;

public class Payment {
    private String paymentId;
    private String appointmentId;
    private double amount;
    private String status; // "Pending", "Completed"
    private String date;

    // Default constructor required for Firebase
    public Payment() {
    }

    public Payment(String paymentId, String appointmentId, double amount, String status, String date) {
        this.paymentId = paymentId;
        this.appointmentId = appointmentId;
        this.amount = amount;
        this.status = status;
        this.date = date;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
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
