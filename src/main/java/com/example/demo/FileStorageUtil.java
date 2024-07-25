package com.example.demo;


import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.web.multipart.MultipartFile;

public class FileStorageUtil {

    private static final String HTML_DIR_PATH = "src/main/resources/html/";
    private static final String IMAGE_DIR_PATH = "src/main/resources/images/";
    private static final Logger LOGGER = Logger.getLogger(FileStorageUtil.class.getName());

    public static void saveDescriptionHtml(String descriptionHtml, Long industryId) {
        try {
            File htmlDir = new File(HTML_DIR_PATH);
            if (!htmlDir.exists()) {
                boolean dirsCreated = htmlDir.mkdirs(); // Ensure the directory exists
                LOGGER.log(Level.INFO, "Created HTML directory: {0}", dirsCreated);
            }

            String fileName = industryId + "_description.html";
            File htmlFile = new File(htmlDir, fileName);

            try (PrintWriter writer = new PrintWriter(htmlFile)) {
                writer.println(descriptionHtml);
            }

            LOGGER.log(Level.INFO, "HTML description saved for industry ID: {0}", industryId);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error saving HTML description for industry ID: " + industryId, e);
        }
    }

    public static void saveImageAndSetImagePath(MultipartFile image, Industry industry) {
        if (image != null && !image.isEmpty()) {
            try {
                File imageDir = new File(IMAGE_DIR_PATH);
                if (!imageDir.exists()) {
                    boolean dirsCreated = imageDir.mkdirs(); // Ensure the directory exists
                    LOGGER.log(Level.INFO, "Created Images directory: {0}", dirsCreated);
                }

                String imageName = industry.getId() + "_" + image.getOriginalFilename();
                File imageFile = new File(imageDir, imageName);

                try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                    fos.write(image.getBytes());
                }
                industry.setImagePath("images/" + imageName); // Set image path relative to 'images/'

                LOGGER.log(Level.INFO, "Image saved for industry ID: {0}", industry.getId());
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error saving image for industry ID: " + industry.getId(), e);
            }
        }
    }
}
