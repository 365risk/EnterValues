package com.example.demo;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/industries")
public class IndustryController {

    private static final String JSON_FILE_PATH = "src/main/resources/data.json";
   // private static final String IMAGE_DIR_PATH = "D:" + File.separator + "eclipse jee" + File.separator + "Reactjs" + File.separator + "365risk" + File.separator + "public" + File.separator + "images" + File.separator;
      private static final String IMAGE_DIR_PATH = "https://gramanagendra.github.io/365risk/images/";
    private static final AtomicLong counter = new AtomicLong(0);
    private static List<Industry> industries = new ArrayList<>();

    static {
        loadIndustriesFromFile();
    }

    @GetMapping
    public List<Industry> getAllIndustries() {
        return industries;
    }

    @PostMapping
    public Industry addIndustry(@RequestParam String name,
                                @RequestParam String description,
                                @RequestParam(required = false) MultipartFile image) {
        Industry newIndustry = new Industry(counter.incrementAndGet(), name, description);

        if (image != null && !image.isEmpty()) {
            try {
                String imageName = newIndustry.getId() + "_" + image.getOriginalFilename();
                File imageFile = new File(IMAGE_DIR_PATH + imageName);
                imageFile.getParentFile().mkdirs(); // Ensure the directory exists
                try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                    fos.write(image.getBytes());
                }
                newIndustry.setImagePath("images/" + imageName); // Set image path relative to 'images/'
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        industries.add(newIndustry);
        saveIndustriesToFile();
        return newIndustry;
    }

    @GetMapping("/{id}")
    public Industry getIndustryById(@PathVariable Long id) {
        return industries.stream()
                .filter(industry -> industry.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Industry not found with id: " + id));
    }

    @PutMapping("/{id}")
    public Industry updateIndustry(@PathVariable Long id,
                                   @RequestParam String name,
                                   @RequestParam String description,
                                   @RequestParam(required = false) MultipartFile image) {
        Industry industry = industries.stream()
                .filter(ind -> ind.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Industry not found with id: " + id));

        industry.setName(name);
        industry.setDescription(description);

        if (image != null && !image.isEmpty()) {
            try {
                String imageName = id + "_" + image.getOriginalFilename();
                File imageFile = new File(IMAGE_DIR_PATH + imageName);
                imageFile.getParentFile().mkdirs(); // Ensure the directory exists
                try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                    fos.write(image.getBytes());
                }
                industry.setImagePath("images/" + imageName); // Set image path relative to 'images/'
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        saveIndustriesToFile();
        return industry;
    }

    @DeleteMapping("/{id}")
    public void deleteIndustry(@PathVariable Long id) {
        industries.removeIf(industry -> industry.getId().equals(id));
        saveIndustriesToFile();
    }

    @PostMapping("/{id}/risks")
    public Risk addRiskToIndustry(@PathVariable Long id, @RequestBody Risk riskData) {
        Industry industry = industries.stream()
                .filter(ind -> ind.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Industry not found with id: " + id));

        Risk newRisk = new Risk(counter.incrementAndGet(), riskData.getRiskName(), riskData.getDescription(), riskData.getIdentification(), riskData.getControl(), riskData.getMitigation());
        industry.getRisks().add(newRisk);
        saveIndustriesToFile();

        return newRisk;
    }

    @GetMapping("/{industryId}/risks/{riskId}")
    public Risk getRiskById(@PathVariable Long industryId, @PathVariable Long riskId) {
        Industry industry = industries.stream()
                .filter(ind -> ind.getId().equals(industryId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Industry not found with id: " + industryId));

        return industry.getRisks().stream()
                .filter(risk -> risk.getId().equals(riskId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Risk not found with id: " + riskId));
    }

    @PutMapping("/{industryId}/risks/{riskId}")
    public Risk updateRiskInIndustry(@PathVariable Long industryId, @PathVariable Long riskId, @RequestBody Risk updatedRisk) {
        Industry industry = industries.stream()
                .filter(ind -> ind.getId().equals(industryId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Industry not found with id: " + industryId));

        Risk existingRisk = industry.getRisks().stream()
                .filter(risk -> risk.getId().equals(riskId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Risk not found with id: " + riskId));

        existingRisk.setRiskName(updatedRisk.getRiskName());
        existingRisk.setDescription(updatedRisk.getDescription());
        existingRisk.setIdentification(updatedRisk.getIdentification());
        existingRisk.setControl(updatedRisk.getControl());
        existingRisk.setMitigation(updatedRisk.getMitigation());

        saveIndustriesToFile();
        return existingRisk;
    }

    @DeleteMapping("/{industryId}/risks/{riskId}")
    public void deleteRiskFromIndustry(@PathVariable Long industryId, @PathVariable Long riskId) {
        Industry industry = industries.stream()
                .filter(ind -> ind.getId().equals(industryId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Industry not found with id: " + industryId));

        industry.getRisks().removeIf(risk -> risk.getId().equals(riskId));
        saveIndustriesToFile();
    }

    private static void saveIndustriesToFile() {
        try (FileWriter writer = new FileWriter(JSON_FILE_PATH)) {
            JSONArray jsonArray = new JSONArray();
            for (Industry industry : industries) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", industry.getId());
                jsonObject.put("name", industry.getName());
                jsonObject.put("description", industry.getDescription());
                jsonObject.put("imagePath", industry.getImagePath());

                JSONArray risksArray = new JSONArray();
                for (Risk risk : industry.getRisks()) {
                    JSONObject riskObject = new JSONObject();
                    riskObject.put("id", risk.getId());
                    riskObject.put("riskName", risk.getRiskName());
                    riskObject.put("description", risk.getDescription());
                    riskObject.put("identification", risk.getIdentification());
                    riskObject.put("control", risk.getControl());
                    riskObject.put("mitigation", risk.getMitigation());
                    risksArray.put(riskObject);
                }
                jsonObject.put("risks", risksArray);

                jsonArray.put(jsonObject);
            }
            JSONObject jsonRoot = new JSONObject();
            jsonRoot.put("industries", jsonArray);
            writer.write(jsonRoot.toString(4));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadIndustriesFromFile() {
        try {
            File file = new File(JSON_FILE_PATH);
            if (file.exists()) {
                String content = new String(Files.readAllBytes(file.toPath()));
                JSONObject jsonRoot = new JSONObject(content);
                JSONArray jsonArray = jsonRoot.getJSONArray("industries");

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    Industry industry = new Industry();
                    industry.setId(jsonObject.getLong("id"));
                    industry.setName(jsonObject.getString("name"));
                    industry.setDescription(jsonObject.getString("description"));
                    industry.setImagePath(jsonObject.optString("imagePath", null));

                    JSONArray risksArray = jsonObject.getJSONArray("risks");
                    for (int j = 0; j < risksArray.length(); j++) {
                        JSONObject riskObject = risksArray.getJSONObject(j);
                        Risk risk = new Risk();
                        risk.setId(riskObject.getLong("id"));
                        risk.setRiskName(riskObject.getString("riskName"));
                        risk.setDescription(riskObject.getString("description"));
                        risk.setIdentification(riskObject.getString("identification"));
                        risk.setControl(riskObject.getString("control"));
                        risk.setMitigation(riskObject.getString("mitigation"));
                        industry.getRisks().add(risk);
                    }
                    industries.add(industry);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
