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
    private static final AtomicLong industryCounter = new AtomicLong(0);
    private static final AtomicLong riskCounter = new AtomicLong(0);
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

        Industry newIndustry = new Industry(industryCounter.incrementAndGet(), name, name + ".html");
        saveDescriptionHtml(descriptionHtml, HTML_DIR_PATH + name + ".html");
        saveImageAndSetImagePath(image, newIndustry);

        industries.add(newIndustry);
        saveIndustriesToFile();
        
        try {
            runGitCommands("Added industry: " + name);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

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
            industryToUpdate.setImagePath(imageFileName);
        }

        saveIndustriesToFile();

        try {
            runGitCommands("Updated industry: " + name);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

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

        try {
            runGitCommands("Deleted industry: " + industry.getName());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
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

        Risk newRisk = new Risk(riskCounter.incrementAndGet(), riskName, riskDetails);

        String descriptionFileName = riskName + ".html";
        saveDescriptionHtml(riskDetails, HTML_DIR_PATH + descriptionFileName);
        newRisk.setDescriptionPath("html/" + descriptionFileName);

        if (riskImage != null && !riskImage.isEmpty()) {
            String imageFileName = riskImage.getOriginalFilename();
            saveImage(riskImage, IMAGE_DIR_PATH + imageFileName);
            newRisk.setImagePath(imageFileName);
        }

        industry.getRisks().add(newRisk);
        saveIndustriesToFile();

        try {
            runGitCommands("Added risk: " + riskName + " to industry: " + industry.getName());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

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
            existingRisk.setImagePath(imageFileName);
        }

        saveIndustriesToFile();

        try {
            runGitCommands("Updated risk: " + riskName + " in industry: " + industry.getName());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

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
            String htmlPath = HTML_DIR_PATH + risk.getDescriptionPath();
            deleteDescriptionHtml(htmlPath);
        }

        if (risk.getImagePath() != null) {
            String imagePath = IMAGE_DIR_PATH + risk.getImagePath();
            deleteImage(imagePath);
        }

        industry.getRisks().removeIf(r -> r.getId().equals(riskId));
        saveIndustriesToFile();

        try {
            runGitCommands("Deleted risk: " + risk.getRiskName() + " from industry: " + industry.getName());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void saveDescriptionHtml(String descriptionHtml, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(descriptionHtml);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveImageAndSetImagePath(MultipartFile image, Industry newIndustry) {
        if (image != null && !image.isEmpty()) {
            String imageFileName = image.getOriginalFilename();
            saveImage(image, IMAGE_DIR_PATH + imageFileName);
            newIndustry.setImagePath(imageFileName);
        }
    }

    private void saveImage(MultipartFile image, String path) {
        try {
            byte[] bytes = image.getBytes();
            Files.write(Paths.get(path), bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteDescriptionHtml(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteImage(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadIndustriesFromFile() {
        try {
            String json = new String(Files.readAllBytes(Paths.get("industries.json")));
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                Industry industry = new Industry(
                        jsonObject.getLong("id"),
                        jsonObject.getString("name"),
                        jsonObject.getString("descriptionHtml")
                );
                if (jsonObject.has("imagePath")) {
                    industry.setImagePath(jsonObject.getString("imagePath"));
                }
                if (jsonObject.has("risks")) {
                    JSONArray risksArray = jsonObject.getJSONArray("risks");
                    for (int j = 0; j < risksArray.length(); j++) {
                        JSONObject riskObject = risksArray.getJSONObject(j);
                        Risk risk = new Risk(
                                riskObject.getLong("id"),
                                riskObject.getString("riskName"),
                                riskObject.getString("riskDetails")
                        );
                        if (riskObject.has("imagePath")) {
                            risk.setImagePath(riskObject.getString("imagePath"));
                        }
                        if (riskObject.has("descriptionPath")) {
                            risk.setDescriptionPath(riskObject.getString("descriptionPath"));
                        }
                        industry.getRisks().add(risk);
                    }
                }
                industries.add(industry);
                industryCounter.set(Math.max(industryCounter.get(), industry.getId()));
                for (Risk risk : industry.getRisks()) {
                    riskCounter.set(Math.max(riskCounter.get(), risk.getId()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveIndustriesToFile() {
        JSONArray jsonArray = new JSONArray();
        for (Industry industry : industries) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", industry.getId());
            jsonObject.put("name", industry.getName());
            jsonObject.put("descriptionHtml", industry.getDescriptionHtml());
            if (industry.getImagePath() != null) {
                jsonObject.put("imagePath", industry.getImagePath());
            }
            JSONArray risksArray = new JSONArray();
            for (Risk risk : industry.getRisks()) {
                JSONObject riskObject = new JSONObject();
                riskObject.put("id", risk.getId());
                riskObject.put("riskName", risk.getRiskName());
                riskObject.put("riskDetails", risk.getRiskDetails());
                if (risk.getImagePath() != null) {
                    riskObject.put("imagePath", risk.getImagePath());
                }
                if (risk.getDescriptionPath() != null) {
                    riskObject.put("descriptionPath", risk.getDescriptionPath());
                }
                risksArray.put(riskObject);
            }
            jsonObject.put("risks", risksArray);
            jsonArray.put(jsonObject);
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("industries.json"))) {
            writer.write(jsonArray.toString(4));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void runGitCommands(String commitMessage) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("git", "add", ".");
        pb.directory(new File(System.getProperty("user.dir")));
        Process p = pb.start();
        p.waitFor();

        pb = new ProcessBuilder("git", "commit", "-m", commitMessage);
        pb.directory(new File(System.getProperty("user.dir")));
        p = pb.start();
        p.waitFor();

        pb = new ProcessBuilder("git", "push", "--force");
        pb.directory(new File(System.getProperty("user.dir")));
        p = pb.start();
        p.waitFor();
    }
}
