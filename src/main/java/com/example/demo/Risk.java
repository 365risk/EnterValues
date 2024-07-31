package com.example.demo;

import java.util.concurrent.atomic.AtomicLong;

public class Risk {

    private static AtomicLong idCounter = new AtomicLong(1); // AtomicLong for thread-safe auto-incrementing IDs

    private Long id;
    private String riskName;
    private String riskDetails;
    private String imagePath;
    private String descriptionPath; // Optional field

    // Default constructor
    public Risk() {}

    // Constructor with ID and all fields
    public Risk(Long id, String riskName, String riskDetails, String imagePath, String descriptionPath) {
        this.id = id;
        this.riskName = riskName;
        this.riskDetails = riskDetails;
        this.imagePath = imagePath;
        this.descriptionPath = descriptionPath;
    }

    // Constructor with ID and required fields
    public Risk(Long id, String riskName, String riskDetails) {
        this(id, riskName, riskDetails, null, null); // Default imagePath and descriptionPath to null
    }

    // Static method to get the next ID
    public static long getNextId() {
        return idCounter.getAndIncrement();
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRiskName() {
        return riskName;
    }

    public void setRiskName(String riskName) {
        this.riskName = riskName;
    }

    public String getRiskDetails() {
        return riskDetails;
    }

    public void setRiskDetails(String riskDetails) {
        this.riskDetails = riskDetails;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getDescriptionPath() {
        return descriptionPath;
    }

    public void setDescriptionPath(String descriptionPath) {
        this.descriptionPath = descriptionPath;
    }

    @Override
    public String toString() {
        return "Risk{" +
                "id=" + id +
                ", riskName='" + riskName + '\'' +
                ", riskDetails='" + riskDetails + '\'' +
                ", imagePath='" + imagePath + '\'' +
                ", descriptionPath='" + descriptionPath + '\'' +
                '}';
    }
}
