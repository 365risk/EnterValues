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
        executeGitCommand("git add .");
        executeGitCommand("git commit -m \"Added new industry: " + name + "\"");
        executeGitCommand("git push origin main origin main");
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
            String imageFileName = image.getOriginalFilename();
            saveImage(image, IMAGE_DIR_PATH + imageFileName);
            industryToUpdate.setImagePath("images/" + imageFileName);
        }

        saveIndustriesToFile();
        executeGitCommand("git add .");
        executeGitCommand("git commit -m \"Updated industry: " + name + "\"");
        executeGitCommand("git push origin main");
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
        executeGitCommand("git add .");
        executeGitCommand("git commit -m \"Deleted industry with id: " + id + "\"");
        executeGitCommand("git push origin main");
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
            String imageFileName = riskImage.getOriginalFilename();
            saveImage(riskImage, IMAGE_DIR_PATH + imageFileName);
            newRisk.setImagePath("images/" + imageFileName);
        }

        industry.getRisks().add(newRisk);
        saveIndustriesToFile();
        executeGitCommand("git add .");
        executeGitCommand("git commit -m \"Added risk to industry with id: " + id + "\"");
        executeGitCommand("git push origin main");

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

        String oldDescriptionPath = existingRisk.getDescriptionPath();
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
            String imageFileName = riskImage.getOriginalFilename();
            saveImage(riskImage, IMAGE_DIR_PATH + imageFileName);
            existingRisk.setImagePath("images/" + imageFileName);
        }

        saveIndustriesToFile();
        executeGitCommand("git add .");
        executeGitCommand("git commit -m \"Updated risk in industry with id: " + industryId + "\"");
        executeGitCommand("git push origin main");

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

        if (risk.getImagePath() != null) {
            String riskImagePath = IMAGE_DIR_PATH + risk.getImagePath();
            deleteImage(riskImagePath);
        }

        if (risk.getDescriptionPath() != null) {
            String riskDescriptionPath = HTML_DIR_PATH + risk.getDescriptionPath();
            deleteDescriptionHtml(riskDescriptionPath);
        }

        industry.getRisks().removeIf(r -> r.getId().equals(riskId));
        saveIndustriesToFile();
        executeGitCommand("git add .");
        executeGitCommand("git commit -m \"Deleted risk with id: " + riskId + " from industry with id: " + industryId + "\"");
        executeGitCommand("git push origin main");
    }

    private void saveDescriptionHtml(String content, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(content);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save HTML description", e);
        }
    }

    private void deleteDescriptionHtml(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            if (!file.delete()) {
                LOGGER.log(Level.SEVERE, "Failed to delete HTML file: {0}", filePath);
            }
        }
    }

    private void saveImage(MultipartFile image, String filePath) {
        try {
            Files.copy(image.getInputStream(), Paths.get(filePath));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save image", e);
        }
    }

    private void deleteImage(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            if (!file.delete()) {
                LOGGER.log(Level.SEVERE, "Failed to delete image file: {0}", filePath);
            }
        }
    }

    private void saveImageAndSetImagePath(MultipartFile image, Industry industry) {
        if (image != null && !image.isEmpty()) {
            // Generate a unique filename
            String imageFileName = UUID.randomUUID().toString() + "-" + image.getOriginalFilename();
            String imagePath = IMAGE_DIR_PATH + imageFileName;

            // Save the image
            try {
                Files.copy(image.getInputStream(), Paths.get(imagePath));
                // Set the image path in the industry object
                industry.setImagePath("images/" + imageFileName);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to save image", e);
            }
        }
    }

    private void saveIndustriesToFile() {
        JSONArray jsonArray = new JSONArray();
        for (Industry industry : industries) {
            JSONObject jsonIndustry = new JSONObject();
            jsonIndustry.put("id", industry.getId());
            jsonIndustry.put("name", industry.getName());
            jsonIndustry.put("descriptionHtml", industry.getDescriptionHtml());
            jsonIndustry.put("imagePath", industry.getImagePath());

            JSONArray jsonRisks = new JSONArray();
            for (Risk risk : industry.getRisks()) {
                JSONObject jsonRisk = new JSONObject();
                jsonRisk.put("id", risk.getId());
                jsonRisk.put("riskName", risk.getRiskName());
                jsonRisk.put("riskDetails", risk.getRiskDetails());
                jsonRisk.put("imagePath", risk.getImagePath());
                jsonRisks.put(jsonRisk);
            }
            jsonIndustry.put("risks", jsonRisks);

            jsonArray.put(jsonIndustry);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("src/main/resources/industries.json"))) {
            writer.write(jsonArray.toString(4));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save industries to file", e);
        }
    }

    private static void loadIndustriesFromFile() {
        File file = new File("src/main/resources/industries.json");
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
                JSONArray jsonArray = new JSONArray(content.toString());
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonIndustry = jsonArray.getJSONObject(i);
                    Long id = jsonIndustry.getLong("id");
                    String name = jsonIndustry.getString("name");
                    String descriptionHtml = jsonIndustry.getString("descriptionHtml");
                    String imagePath = jsonIndustry.optString("imagePath", null);

                    Industry industry = new Industry(id, name, descriptionHtml);
                    industry.setImagePath(imagePath);

                    JSONArray jsonRisks = jsonIndustry.getJSONArray("risks");
                    for (int j = 0; j < jsonRisks.length(); j++) {
                        JSONObject jsonRisk = jsonRisks.getJSONObject(j);
                        Long riskId = jsonRisk.getLong("id");
                        String riskName = jsonRisk.getString("riskName");
                        String riskDetails = jsonRisk.getString("riskDetails");
                        String riskImagePath = jsonRisk.optString("imagePath", null);

                        Risk risk = new Risk(riskId, riskName, riskDetails);
                        risk.setImagePath(riskImagePath);

                        industry.getRisks().add(risk);
                    }

                    industries.add(industry);
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to load industries from file", e);
            }
        }
    }

    private void executeGitCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOGGER.info(line);
                }
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Failed to execute git command: " + command, e);
        }
    }
}
