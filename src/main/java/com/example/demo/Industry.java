package com.example.demo;

import java.util.ArrayList;
import java.util.List;

public class Industry {
    private Long id;
    private String name;
    private String descriptionHtml; // HTML description file path
    private String imagePath; // Path to the image
    private List<Risk> risks = new ArrayList<>();

    public Industry() {
    }

    public Industry(Long id, String name, String descriptionHtml) {
        this.id = id;
        this.name = name;
        this.descriptionHtml = descriptionHtml;
    }

    // Getters and Setters

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
}
