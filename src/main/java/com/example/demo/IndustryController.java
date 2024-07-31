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
            String imageFileName = riskImage.getOriginalFilename();
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
            String descriptionPath = HTML_DIR_PATH + risk.getDescriptionPath();
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
            String imageFileName = image.getOriginalFilename();
            saveImage(image, IMAGE_DIR_PATH + imageFileName);
            industry.setImagePath("images/" + imageFileName);
        }
    }

    private void saveImage(MultipartFile image, String path) {
        try {
            Files.write(Paths.get(path), image.getBytes());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save image file", e);
        }
    }

    private void deleteImage(String path) {
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete image file", e);
        }
    }

    private void saveDescriptionHtml(String html, String path) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write(html);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save HTML file", e);
        }
    }

    private void deleteDescriptionHtml(String path) {
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete HTML file", e);
        }
    }

    private static void loadIndustriesFromFile() {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("src/main/resources/industries.json"))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            
            while ((line = bufferedReader.readLine()) != null) {
                jsonContent.append(line);
            }

            JSONArray industriesArray = new JSONArray(jsonContent.toString());

            for (int i = 0; i < industriesArray.length(); i++) {
                JSONObject industryJson = industriesArray.getJSONObject(i);

                Long id = industryJson.getLong("id");
                String name = industryJson.getString("name");
                String descriptionHtml = industryJson.getString("descriptionHtml");
                String imagePath = industryJson.optString("imagePath", null);

                Industry industry = new Industry(id, name, descriptionHtml);
                industry.setImagePath(imagePath);

                JSONArray risksArray = industryJson.getJSONArray("risks");
                for (int j = 0; j < risksArray.length(); j++) {
                    JSONObject riskJson = risksArray.getJSONObject(j);

                    Long riskId = riskJson.getLong("id");
                    String riskName = riskJson.getString("riskName");
                    String riskDetails = riskJson.getString("riskDetails");
                    String riskDescriptionPath = riskJson.optString("descriptionPath", null);
                    String riskImagePath = riskJson.optString("imagePath", null);

                    Risk risk = new Risk(riskId, riskName, riskDetails);
                    risk.setDescriptionPath(riskDescriptionPath);
                    risk.setImagePath(riskImagePath);

                    industry.getRisks().add(risk);
                }

                industries.add(industry);
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load industries from file", e);
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

            // Git operations
            executeGitCommand("git add src/main/resources/industries.json");
            executeGitCommand("git commit -m \"Updated industries.json\"");
            executeGitCommand("git push");

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save industries to file", e);
        }
    }

    private void executeGitCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line); // Print Git command output to the console
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Failed to execute Git command: " + command, e);
        }
    }
}
