package com.ecommerce.ecom.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    @Override
    public String uploadImage(String path, MultipartFile file) throws IOException {

        String originalFilename = file.getOriginalFilename();

        // Check if originalFilename is null or empty
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }

        // Check if the file has an extension
        int dotIndex = originalFilename.lastIndexOf(".");
        if (dotIndex == -1) {
            throw new IllegalArgumentException("File does not have a valid extension");
        }

        String randomId = UUID.randomUUID().toString();
        String fileName = randomId.concat(originalFilename.substring(dotIndex));

        String filePath = path + File.separator + fileName;

        File folder = new File(path);

        if (!folder.exists()) {
            folder.mkdir();
        }

        Files.copy(file.getInputStream(), Paths.get(filePath));

        return fileName;

    }
}
