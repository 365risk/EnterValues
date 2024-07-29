package com.example.demo;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/industries")
public class IndustryController {

    private static final String HTML_DIR_PATH = "src/main/resources/html/";
    private static final String IMAGE_DIR_PATH = "src/main/resources/images/";
    private static final String COUNTER_FILE_PATH = "src/main/resources/counter.txt";
    private static final AtomicLong counter = new AtomicLong(0);
    private static List<Industry> industries = new ArrayList<>();

    private static final Logger LOGGER = Logger.getLogger(IndustryController.class.getName());

    static {
        loadCounterFromFile();
        loadIndustriesFromFile();
    }

    @GetMapping
    public List<Industry> getAllIndustries() {
        return industries;
    }

    @PostMapping
    public Industry addIndustry(@RequestParam String name,
                                @RequestParam String descriptionHtml,
                                @RequestParam(required = false) MultipartFile image) {
        LOGGER.log(Level.INFO, "Adding industry with name: {0}", name);

        Industry newIndustry = new Industry(counter.incrementAndGet(), name, name + ".html");
        saveDescriptionHtml(descriptionHtml, HTML_DIR_PATH + name + ".html");
        saveImageAndSetImagePath(image, newIndustry);

        industries.add(newIndustry);
        saveIndustriesToFile();
        saveCounterToFile(); // Ensure counter value is saved
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
                                   @RequestParam String descriptionHtml,
                                   @RequestParam(required = false) MultipartFile image) {
        Industry industryToUpdate = industries.stream()
                .filter(ind -> ind.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Industry not found with id: " + id));

        String oldDescriptionPath = industryToUpdate.getDescriptionHtml();
        String newDescriptionPath = name + ".html";
        industryToUpdate.setName(name);
        industryToUpdate.setDescriptionHtml(newDescriptionPath);

        if (!oldDescriptionPath.equals(newDescriptionPath)) {
            deleteDescriptionHtml(HTML_DIR_PATH + oldDescriptionPath);
        }
        saveDescriptionHtml(descriptionHtml, HTML_DIR_PATH + newDescriptionPath);

        if (image != null && !image.isEmpty()) {
            if (industryToUpdate.getImagePath() != null) {
                deleteImage(IMAGE_DIR_PATH + industryToUpdate.getImagePath());
            }
            saveImage(image, IMAGE_DIR_PATH + image.getOriginalFilename());
            industryToUpdate.setImagePath("images/" + image.getOriginalFilename());
        }

        saveIndustriesToFile();
        saveCounterToFile(); // Ensure counter value is saved
        return industryToUpdate;
    }

    @DeleteMapping("/{id}")
    public void deleteIndustry(@PathVariable Long id) {
        Industry industry = industries.stream()
                .filter(ind -> ind.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Industry not found with id: " + id));

        if (industry.getImagePath() != null) {
            deleteImage(IMAGE_DIR_PATH + industry.getImagePath());
        }

        if (industry.getDescriptionHtml() != null) {
            deleteDescriptionHtml(HTML_DIR_PATH + industry.getDescriptionHtml());
        }

        for (Risk risk : industry.getRisks()) {
            if (risk.getDescriptionPath() != null) {
                deleteDescriptionHtml(HTML_DIR_PATH + risk.getDescriptionPath());
            }
            if (risk.getImagePath() != null) {
                deleteImage(IMAGE_DIR_PATH + risk.getImagePath());
            }
        }

        industries.removeIf(ind -> ind.getId().equals(id));
        saveIndustriesToFile();
        saveCounterToFile(); // Ensure counter value is saved
    }

    @PostMapping("/{id}/risks")
    public ResponseEntity<Risk> addRiskToIndustry(
            @PathVariable Long id,
            @RequestParam String riskName,
            @RequestParam String riskDetails,
            @RequestParam(required = false) MultipartFile riskImage) {

        Industry industry = industries.stream()
                .filter(ind -> ind.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Industry not found with id: " + id));

        Risk newRisk = new Risk(counter.incrementAndGet(), riskName, riskDetails);
        String descriptionFileName = riskName + ".html";
        saveDescriptionHtml(riskDetails, HTML_DIR_PATH + descriptionFileName);
        newRisk.setDescriptionPath("html/" + descriptionFileName);

        if (riskImage != null && !riskImage.isEmpty()) {
            saveImage(riskImage, IMAGE_DIR_PATH + riskImage.getOriginalFilename());
            newRisk.setImagePath("images/" + riskImage.getOriginalFilename());
        }

        industry.getRisks().add(newRisk);
        saveIndustriesToFile();
        saveCounterToFile(); // Ensure counter value is saved

        return ResponseEntity.ok(newRisk);
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
    public ResponseEntity<Risk> updateRiskInIndustry(@PathVariable Long industryId,
                                                     @PathVariable Long riskId,
                                                     @RequestParam String riskName,
                                                     @RequestParam String riskDetails,
                                                     @RequestParam(required = false) MultipartFile riskImage) {

        Industry industry = industries.stream()
                .filter(ind -> ind.getId().equals(industryId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Industry not found with id: " + industryId));

        Risk existingRisk = industry.getRisks().stream()
                .filter(risk -> risk.getId().equals(riskId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Risk not found with id: " + riskId));

        String oldDescriptionPath = existingRisk.getRiskName() + ".html";
        String newDescriptionPath = "html/" + riskName + ".html";
        existingRisk.setRiskName(riskName);
        existingRisk.setRiskDetails(riskDetails);

        if (oldDescriptionPath != null && !oldDescriptionPath.equals(newDescriptionPath)) {
            deleteDescriptionHtml(HTML_DIR_PATH + oldDescriptionPath);
        }
        saveDescriptionHtml(riskDetails, HTML_DIR_PATH + riskName + ".html");
        existingRisk.setDescriptionPath(newDescriptionPath);

        if (riskImage != null && !riskImage.isEmpty()) {
            if (existingRisk.getImagePath() != null) {
                deleteImage(IMAGE_DIR_PATH + existingRisk.getImagePath());
            }
            saveImage(riskImage, IMAGE_DIR_PATH + riskImage.getOriginalFilename());
            existingRisk.setImagePath("images/" + riskImage.getOriginalFilename());
        }

        saveIndustriesToFile();
        saveCounterToFile(); // Ensure counter value is saved
        return ResponseEntity.ok(existingRisk);
    }

    @DeleteMapping("/{industryId}/risks/{riskId}")
    public void deleteRiskFromIndustry(@PathVariable Long industryId, @PathVariable Long riskId) {
        Industry industry = industries.stream()
                .filter(ind -> ind.getId().equals(industryId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Industry not found with id: " + industryId));

        Risk risk = industry.getRisks().stream()
                .filter(r -> r.getId().equals(riskId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Risk not found with id: " + riskId));

        if (risk.getDescriptionPath() != null) {
            deleteDescriptionHtml(HTML_DIR_PATH + risk.getDescriptionPath());
        }
        if (risk.getImagePath() != null) {
            deleteImage(IMAGE_DIR_PATH + risk.getImagePath());
        }

        industry.getRisks().removeIf(r -> r.getId().equals(riskId));
        saveIndustriesToFile();
        saveCounterToFile(); // Ensure counter value is saved
    }

    private void saveImageAndSetImagePath(MultipartFile image, Industry industry) {
        if (image != null && !image.isEmpty()) {
            String imageFileName = image.getOriginalFilename();
            saveImage(image, IMAGE_DIR_PATH + imageFileName);
            industry.setImagePath("images/" + imageFileName);
        }
    }

    private void saveImage(MultipartFile image, String filePath) {
        try {
            Files.write(Paths.get(filePath), image.getBytes());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save image", e);
        }
    }

    private void deleteImage(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete image", e);
        }
    }

    private void saveDescriptionHtml(String htmlContent, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(htmlContent);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save description HTML", e);
        }
    }

    private void deleteDescriptionHtml(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete description HTML", e);
        }
    }

    private static void saveIndustriesToFile() {
        JSONArray jsonArray = new JSONArray();
        for (Industry industry : industries) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", industry.getId());
            jsonObject.put("name", industry.getName());
            jsonObject.put("descriptionHtml", industry.getDescriptionHtml());
            jsonObject.put("imagePath", industry.getImagePath());

            JSONArray risksArray = new JSONArray();
            for (Risk risk : industry.getRisks()) {
                JSONObject riskObject = new JSONObject();
                riskObject.put("id", risk.getId());
                riskObject.put("riskName", risk.getRiskName());
                riskObject.put("riskDetails", risk.getRiskDetails());
                riskObject.put("descriptionPath", risk.getDescriptionPath());
                riskObject.put("imagePath", risk.getImagePath());
                risksArray.put(riskObject);
            }
            jsonObject.put("risks", risksArray);

            jsonArray.put(jsonObject);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("src/main/resources/industries.json"))) {
            writer.write(jsonArray.toString(4));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save industries to file", e);
        }
    }

    private static void loadIndustriesFromFile() {
        try {
            File industriesFile = new File("src/main/resources/industries.json");
            if (industriesFile.exists()) {
                String content = new String(Files.readAllBytes(industriesFile.toPath()));
                JSONArray jsonArray = new JSONArray(content);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    Industry industry = new Industry(
                            jsonObject.getLong("id"),
                            jsonObject.getString("name"),
                            jsonObject.getString("descriptionHtml")
                    );
                    industry.setImagePath(jsonObject.optString("imagePath"));
    
                    JSONArray risksArray = jsonObject.optJSONArray("risks");
                    if (risksArray != null) {
                        for (int j = 0; j < risksArray.length(); j++) {
                            JSONObject riskObject = risksArray.getJSONObject(j);
                            Risk risk = new Risk(
                                    riskObject.getLong("id"),
                                    riskObject.getString("riskName"),
                                    riskObject.getString("riskDetails")
                            );
                            risk.setDescriptionPath(riskObject.optString("descriptionPath", null));
                            risk.setImagePath(riskObject.optString("imagePath", null));
                            industry.getRisks().add(risk);
                        }
                    }
    
                    industries.add(industry);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load industries from file", e);
        }
    }
    

    private static void loadCounterFromFile() {
        try {
            File counterFile = new File(COUNTER_FILE_PATH);
            if (counterFile.exists()) {
                String counterValue = new String(Files.readAllBytes(counterFile.toPath()));
                counter.set(Long.parseLong(counterValue.trim()));
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load counter from file", e);
        }
    }

    private static void saveCounterToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(COUNTER_FILE_PATH))) {
            writer.write(String.valueOf(counter.get()));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save counter to file", e);
        }
    }
}
