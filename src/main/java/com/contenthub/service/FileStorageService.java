package com.contenthub.service;

import com.contenthub.exception.AppExceptions.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.base-dir}")
    private String baseDir;

    private Path baseDirPath() {
        return Paths.get(baseDir).toAbsolutePath().normalize();
    }

    /**
     * Saves the file under baseDir/subDir/<uuid>_<originalName> and
     * returns the path relative to baseDir (stored in the DB).
     */
    public String store(MultipartFile file, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("A file is required.");
        }
        try {
            String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
            String safeName = original.replaceAll("[^a-zA-Z0-9._-]", "_");
            String storedName = UUID.randomUUID() + "_" + safeName;

            Path targetDir = baseDirPath().resolve(subDir).normalize();
            Files.createDirectories(targetDir);

            Path targetFile = targetDir.resolve(storedName).normalize();
            if (!targetFile.startsWith(targetDir)) {
                throw new BadRequestException("Invalid file name.");
            }

            file.transferTo(targetFile);
            return subDir + "/" + storedName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }

    public Path resolve(String relativePath) {
        Path resolved = baseDirPath().resolve(relativePath).normalize();
        if (!resolved.startsWith(baseDirPath())) {
            throw new BadRequestException("Invalid file path.");
        }
        return resolved;
    }

    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (IOException ignored) {
            // Non-fatal: leftover file on disk shouldn't block the DB operation
        }
    }
}
