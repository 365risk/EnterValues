package com.example.demo;
    import org.json.JSONArray;
    import org.json.JSONObject;
    import org.springframework.web.bind.annotation.*;

    import java.io.File;
    import java.io.FileWriter;
    import java.io.IOException;
    import java.nio.file.Files;
    import java.nio.file.Paths;
    import java.util.ArrayList;
    import java.util.List;
    import java.util.concurrent.atomic.AtomicLong;

    @RestController
    @RequestMapping("/api/contacts")
    public class ContactController {

        private static final String FILE_PATH = "C:\\\\Users\\\\raman\\\\OneDrive\\\\Desktop\\\\demo\\\\demo\\\\src\\\\main\\\\resources\\\\data.json";
        private static final AtomicLong counter = new AtomicLong(0);
        private static List<Contact> contacts = new ArrayList<>();

        static {
            // Initialize with some dummy data for testing
            contacts.add(new Contact(counter.incrementAndGet(), "John Doe", "1234567890"));
            contacts.add(new Contact(counter.incrementAndGet(), "Jane Smith", "0987654321"));
        }

        @GetMapping
        public List<Contact> getContacts() {
            return contacts;
        }

        @PostMapping
        public Contact addContact(@RequestBody Contact newContact) {
            newContact.setId(counter.incrementAndGet());
            contacts.add(newContact);
            saveContactsToFile();
            return newContact;
        }

        @PutMapping("/{id}")
        public Contact updateContact(@PathVariable Long id, @RequestBody Contact updatedContact) {
            for (Contact contact : contacts) {
                if (contact.getId().equals(id)) {
                    contact.setName(updatedContact.getName());
                    contact.setNumber(updatedContact.getNumber());
                    saveContactsToFile();
                    return contact;
                }
            }
            throw new RuntimeException("Contact not found with id: " + id);
        }

        @DeleteMapping("/{id}")
        public void deleteContact(@PathVariable Long id) {
            contacts.removeIf(contact -> contact.getId().equals(id));
            saveContactsToFile();
        }

        @GetMapping("/{id}")
        public Contact getContactById(@PathVariable Long id) {
            return contacts.stream()
                    .filter(contact -> contact.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Contact not found with id: " + id));
        }

        private void saveContactsToFile() {
            try (FileWriter writer = new FileWriter(FILE_PATH)) {
                JSONArray jsonArray = new JSONArray();
                for (Contact contact : contacts) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("id", contact.getId());
                    jsonObject.put("name", contact.getName());
                    jsonObject.put("number", contact.getNumber());
                    jsonArray.put(jsonObject);
                }
                JSONObject jsonRoot = new JSONObject();
                jsonRoot.put("contacts", jsonArray);
                writer.write(jsonRoot.toString(4));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
