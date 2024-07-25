package com.example.demo;

public class Risk {

    private Long id;
    private String riskName;
    private String riskDetails;
    private String imagePath;

    public Risk() {
        // Default constructor
    }

    public Risk(Long id, String riskName, String riskDetails) {
        this.id = id;
        this.riskName = riskName;
        this.riskDetails = riskDetails;
    }

    // Getters and setters

    public Risk(long long1, String string, String string2, String optString, String optString2) {
		// TODO Auto-generated constructor stub
	}

	public Risk(String name, String descriptionPath) {
		// TODO Auto-generated constructor stub
	}

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
		// TODO Auto-generated method stub
		return null;
	}

	public void setDescriptionPath(String string) {
		// TODO Auto-generated method stub
		
	}
}
