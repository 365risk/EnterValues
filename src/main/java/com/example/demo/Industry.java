package com.example.demo;

import java.util.ArrayList;
import java.util.List;

public class Industry {
    private static long idCounter = 1; // Static counter for IDs
    private Long id;
    private String name;
    private String descriptionHtml;
    private String imagePath;
    private List<Risk> risks = new ArrayList<>();

    // Constructor without imagePath
    public Industry(Long id, String name, String descriptionHtml) {
        this.id = id; // Use the provided id
        this.name = name;
        this.descriptionHtml = descriptionHtml;
    }

    // Constructor with imagePath
    public Industry(Long id, String name, String descriptionHtml, String imagePath) {
        this(id, name, descriptionHtml);
        this.imagePath = imagePath;
    }

    // Constructor that auto-generates the id
    public Industry(String name, String descriptionHtml) {
        this.id = idCounter++; // Auto-generate id
        this.name = name;
        this.descriptionHtml = descriptionHtml;
    }

    // Constructor with imagePath that auto-generates the id
    public Industry(String name, String descriptionHtml, String imagePath) {
        this(name, descriptionHtml);
        this.imagePath = imagePath;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescriptionHtml() {
        return descriptionHtml;
    }

    public void setDescriptionHtml(String descriptionHtml) {
        this.descriptionHtml = descriptionHtml;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public List<Risk> getRisks() {
        return risks;
    }

    public void setRisks(List<Risk> risks) {
        this.risks = risks;
    }

    @Override
    public String toString() {
        return "Industry{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", descriptionHtml='" + descriptionHtml + '\'' +
                ", imagePath='" + imagePath + '\'' +
                ", risks=" + risks +
                '}';
    }
}
