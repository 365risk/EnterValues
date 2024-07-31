package com.example.demo;

public class Risk {

    private static long idCounter = 1; // Static counter for auto-incrementing IDs

    private Long id;
    private String riskName;
    private String riskDetails;
    private String imagePath;
    private String descriptionPath; // Optional field

    // Default constructor
    public Risk() {
        this.id = idCounter++; // Initialize ID and increment counter
    }

    // Constructor with required fields
    public Risk(Long id, String riskName, String riskDetails) {
        this.id = idCounter++; // Initialize ID and increment counter
        this.riskName = riskName;
        this.riskDetails = riskDetails;
    }

    // Constructor with all fields
    public Risk(long long1, String string, String string2, String optString, String optString2) {
        this.id = idCounter++; // Initialize ID and increment counter
        this.riskName = string;
        this.riskDetails = string2;
        this.imagePath = optString;
        this.descriptionPath = optString2;
    }

    public Risk(String name, String descriptionPath) {
        this.id = idCounter++; // Initialize ID and increment counter
        this.riskName = name;
        this.descriptionPath = descriptionPath;
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

    public void setDescriptionPath(String string) {
        this.descriptionPath = string;
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
