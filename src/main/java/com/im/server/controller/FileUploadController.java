package com.im.server.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    @Value("${im.upload.path:./uploads}")
    private String uploadPath;

    @Value("${im.upload.url-prefix:/uploads}")
    private String urlPrefix;

    @PostMapping
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        if (file.isEmpty()) {
            result.put("success", false);
            result.put("message", "File is empty");
            return result;
        }

        try {
            // Ensure directory exists
            Path uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // Generate filename
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID().toString() + extension;

            // Save file
            Path targetPath = uploadDir.resolve(newFilename);
            file.transferTo(targetPath.toFile());

            // Return URL
            String fileUrl = urlPrefix + "/" + newFilename;
            result.put("success", true);
            result.put("url", fileUrl);
            result.put("type", getFileType(extension));

        } catch (IOException e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "Upload failed: " + e.getMessage());
        }

        return result;
    }

    private String getFileType(String extension) {
        String ext = extension.toLowerCase();
        if (ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png") || ext.equals(".gif")) {
            return "image";
        } else if (ext.equals(".mp3") || ext.equals(".wav") || ext.equals(".amr")) {
            return "audio";
        }
        return "file";
    }
}
