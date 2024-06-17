package com.example.demo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class JsonFileUpdater {

    public static void main(String[] args) {
        // Path to the JSON file
        String filePath = "C:\\Users\\raman\\OneDrive\\Desktop\\demo\\demo\\src\\main\\resources\\data.json";

        try {
            // Read existing JSON file
            File file = new File(filePath);
            if (!file.exists()) {
                System.err.println("File does not exist. Creating a new one.");
                file.createNewFile();
            }

            String jsonContent = new String(Files.readAllBytes(Paths.get(filePath)));
            System.out.println("File content: " + jsonContent);

            JSONObject jsonObject;

            // Handle empty file content
            if (jsonContent.trim().isEmpty()) {
                System.out.println("File is empty. Initializing with a basic JSON structure.");
                jsonObject = new JSONObject();
                jsonObject.put("contacts", new JSONArray());
            } else {
                jsonObject = new JSONObject(jsonContent);
            }

            // Get the contacts array
            JSONArray contactsArray = jsonObject.getJSONArray("contacts");

            // Create a new contact
            JSONObject newContact = new JSONObject();
            newContact.put("name", "Alice Johnson");
            newContact.put("number", "5551234567");

            // Add the new contact to the contacts array
            contactsArray.put(newContact);

            // Write the updated JSON back to the file
            FileWriter writer = new FileWriter(filePath);
            writer.write(jsonObject.toString(4)); // Indent with 4 spaces for readability
            writer.flush();
            writer.close();

            System.out.println("JSON file updated successfully.");

        } catch (IOException e) {
            System.err.println("Error reading or writing file: " + e.getMessage());
        } catch (org.json.JSONException e) {
            System.err.println("Error parsing JSON content: " + e.getMessage());
        }
    }
}

