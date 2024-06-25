package com.example.demo;

public class Risk {
    private Long id;
    private String riskName;
    private String description;
    private String identification;
    private String control;
    private String mitigation;

    public Risk() {
    }



    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIdentification() {
        return identification;
    }

    public void setIdentification(String identification) {
        this.identification = identification;
    }

    public String getControl() {
        return control;
    }

    public void setControl(String control) {
        this.control = control;
    }

    public String getMitigation() {
        return mitigation;
    }

    public void setMitigation(String mitigation) {
        this.mitigation = mitigation;
    }

	public String getRiskName() {
		return riskName;
	}

	public void setRiskName(String riskName) {
		this.riskName = riskName;
	}

	public Risk(Long id, String riskName, String description, String identification, String control,
			String mitigation) {
		super();
		this.id = id;
		this.riskName = riskName;
		this.description = description;
		this.identification = identification;
		this.control = control;
		this.mitigation = mitigation;
	}
	
	
	
}
