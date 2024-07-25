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

    private static final String HTML_DIR_PATH = "https://github.com/GRamaNagendra/EnterValues/blob/fbc30e4544b92eddc4250ac839dbd9ab2eb50b9c/src/main/resources/html";
    private static final String IMAGE_DIR_PATH = "https://github.com/GRamaNagendra/EnterValues/blob/fbc30e4544b92eddc4250ac839dbd9ab2eb50b9c/src/main/resources/images";
    private static final AtomicLong counter = new AtomicLong(0);
    private static List<Industry> industries = new ArrayList<>();

    private static final Logger LOGGER = Logger.getLogger(IndustryController.class.getName());

    static {
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
            String oldFilePath = HTML_DIR_PATH + oldDescriptionPath;
            deleteDescriptionHtml(oldFilePath);
        }
        saveDescriptionHtml(descriptionHtml, HTML_DIR_PATH + newDescriptionPath);

        if (image != null && !image.isEmpty()) {
            if (industryToUpdate.getImagePath() != null) {
                String oldImagePath = IMAGE_DIR_PATH + industryToUpdate.getImagePath();
                deleteImage(oldImagePath);
            }
            String imageFileName =  image.getOriginalFilename();
            saveImage(image, IMAGE_DIR_PATH + imageFileName);
            industryToUpdate.setImagePath("images/" + imageFileName);
        }

        saveIndustriesToFile();
        return industryToUpdate;
    }

    @DeleteMapping("/{id}")
    public void deleteIndustry(@PathVariable Long id) {
        Industry industry = industries.stream()
                .filter(ind -> ind.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Industry not found with id: " + id));

        if (industry.getImagePath() != null) {
            String imagePath = IMAGE_DIR_PATH + industry.getImagePath();
            deleteImage(imagePath);
        }

        if (industry.getDescriptionHtml() != null) {
            String htmlPath = HTML_DIR_PATH + industry.getDescriptionHtml();
            deleteDescriptionHtml(htmlPath);
        }

        for (Risk risk : industry.getRisks()) {
            if (risk.getDescriptionPath() != null) {
                String riskHtmlPath = HTML_DIR_PATH + risk.getDescriptionPath();
                deleteDescriptionHtml(riskHtmlPath);
            }
            if (risk.getImagePath() != null) {
                String riskImagePath = IMAGE_DIR_PATH + risk.getImagePath();
                deleteImage(riskImagePath);
            }
        }

        industries.removeIf(ind -> ind.getId().equals(id));
        saveIndustriesToFile();
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
            String imageFileName =  riskImage.getOriginalFilename();
            saveImage(riskImage, IMAGE_DIR_PATH + imageFileName);
            newRisk.setImagePath("images/" + imageFileName);
        }

        industry.getRisks().add(newRisk);
        saveIndustriesToFile();

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
       System.out.println(oldDescriptionPath);
        String newDescriptionPath = "html/" + riskName + ".html";
        existingRisk.setRiskName(riskName);
        existingRisk.setRiskDetails(riskDetails);

        if (oldDescriptionPath != null && !oldDescriptionPath.equals(newDescriptionPath)) {
            String oldFilePath = HTML_DIR_PATH + oldDescriptionPath;
            deleteDescriptionHtml(oldFilePath);
        }
        saveDescriptionHtml(riskDetails, HTML_DIR_PATH + riskName + ".html");
        existingRisk.setDescriptionPath(newDescriptionPath);

        if (riskImage != null && !riskImage.isEmpty()) {
            if (existingRisk.getImagePath() != null) {
                String oldImagePath = IMAGE_DIR_PATH + existingRisk.getImagePath();
                deleteImage(oldImagePath);
            }
            String imageFileName =  riskImage.getOriginalFilename();
            saveImage(riskImage, IMAGE_DIR_PATH + imageFileName);
            existingRisk.setImagePath("images/" + imageFileName);
        }

        saveIndustriesToFile();
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

        if (risk.getRiskName() != null) {
            String descriptionPath = HTML_DIR_PATH + risk.getRiskName() + ".html";
            deleteDescriptionHtml(descriptionPath);
        }
        if (risk.getImagePath() != null) {
            String imagePath = IMAGE_DIR_PATH + risk.getImagePath();
            deleteImage(imagePath);
        }

        industry.getRisks().removeIf(r -> r.getId().equals(riskId));
        saveIndustriesToFile();
    }

    private void saveImageAndSetImagePath(MultipartFile image, Industry industry) {
        if (image != null && !image.isEmpty()) {
            String imageFileName =  image.getOriginalFilename();
            saveImage(image, IMAGE_DIR_PATH + imageFileName);
            industry.setImagePath("images/" + imageFileName);
        }
    }

    private void saveDescriptionHtml(String htmlContent, String filePath) {
        try (FileWriter fileWriter = new FileWriter(filePath)) {
            fileWriter.write(htmlContent);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save HTML description", e);
        }
    }

    private void deleteDescriptionHtml(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete HTML description", e);
        }
    }

    private void saveImage(MultipartFile image, String filePath) {
        try (InputStream inputStream = image.getInputStream();
             OutputStream outputStream = new FileOutputStream(filePath)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
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

    private void saveIndustriesToFile() {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("src/main/resources/industries.json"))) {
            bufferedWriter.write("[\n"); // Start the array
            boolean firstIndustry = true;

            for (Industry industry : industries) {
                if (!firstIndustry) {
                    bufferedWriter.write(",\n"); // Add a comma between JSON objects
                }
                firstIndustry = false;

                JSONObject industryJson = new JSONObject();
                industryJson.put("id", industry.getId());
                industryJson.put("name", industry.getName());
                industryJson.put("descriptionHtml", industry.getDescriptionHtml());
                industryJson.put("imagePath", industry.getImagePath());

                JSONArray risksArray = new JSONArray();
                for (Risk risk : industry.getRisks()) {
                    JSONObject riskJson = new JSONObject();
                    riskJson.put("id", risk.getId());
                    riskJson.put("riskName", risk.getRiskName());
                    riskJson.put("riskDetails", risk.getRiskDetails());
                    riskJson.put("descriptionPath", risk.getDescriptionPath());
                    riskJson.put("imagePath", risk.getImagePath());
                    risksArray.put(riskJson);
                }
                industryJson.put("risks", risksArray);

                // Convert to pretty-printed JSON string and write to file
                String industryJsonPretty = industryJson.toString(4); // Indent with 4 spaces
                bufferedWriter.write(industryJsonPretty);
            }
            
            bufferedWriter.write("\n]"); // End the array
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save industries to file", e);
        }
    }


    private static void loadIndustriesFromFile() {
        try {
            File file = new File("src/main/resources/industries.json");
            if (file.exists()) {
                byte[] bytes = Files.readAllBytes(file.toPath());
                String jsonString = new String(bytes);
                JSONArray industriesArray = new JSONArray(jsonString);

                industries = new ArrayList<>();
                for (int i = 0; i < industriesArray.length(); i++) {
                    JSONObject industryJson = industriesArray.getJSONObject(i);
                    Industry industry = new Industry(
                            industryJson.getLong("id"),
                            industryJson.getString("name"),
                            industryJson.getString("descriptionHtml")
                    );
                    industry.setImagePath(industryJson.optString("imagePath", null));

                    JSONArray risksArray = industryJson.optJSONArray("risks");
                    List<Risk> risks = new ArrayList<>();
                    if (risksArray != null) {
                        for (int j = 0; j < risksArray.length(); j++) {
                            JSONObject riskJson = risksArray.getJSONObject(j);
                            Risk risk = new Risk(
                                    riskJson.getLong("id"),
                                    riskJson.getString("riskName"),
                                    riskJson.getString("riskDetails")
                            );
                            risk.setDescriptionPath(riskJson.optString("descriptionPath", null));
                            risk.setImagePath(riskJson.optString("imagePath", null));
                            risks.add(risk);
                        }
                    }
                    industry.setRisks(risks);
                    industries.add(industry);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load industries from file", e);
        }
    }
}
