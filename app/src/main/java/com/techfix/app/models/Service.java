package com.techfix.app.models;

public class Service {
    private String serviceId;
    private String name;
    private String category; // "Computer" or "Mobile"
    private String description;
    private double price;
    private String duration; // e.g. "1 hour", "3 days"
    private String imageURL;

    // Default constructor required for Firebase
    public Service() {
    }

    public Service(String serviceId, String name, String category, String description, double price, String duration, String imageURL) {
        this.serviceId = serviceId;
        this.name = name;
        this.category = category;
        this.description = description;
        this.price = price;
        this.duration = duration;
        this.imageURL = imageURL;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }
}
