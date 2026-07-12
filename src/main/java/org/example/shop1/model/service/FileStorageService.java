package org.example.shop1.model.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    public String storeFile(MultipartFile file) {
        try {
            // ساخت پوشه اگر وجود ندارد
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // ساخت اسم یونیک برای فایل
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
            String newFilename = UUID.randomUUID().toString() + extension;

            // مسیر نهایی
            Path filepath = Paths.get(uploadDir, newFilename);

            // کپی فایل روی هارد
            Files.write(filepath, file.getBytes());

            return newFilename;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file on disk", e);
        }
    }
}