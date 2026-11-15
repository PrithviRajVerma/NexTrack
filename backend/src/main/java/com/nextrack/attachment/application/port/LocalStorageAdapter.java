package com.nextrack.attachment.application.port;

import com.nextrack.shared.exception.StorageOperationException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Component
public class LocalStorageAdapter implements StorageAdapter {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageAdapter.class);

    private final Path storageDir;

    public LocalStorageAdapter(@Value("${nextrack.attachments.storage-dir:/tmp/nextrack-attachments}") String storageDirPath) {
        this.storageDir = Paths.get(storageDirPath).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir);
                log.info("Initialized attachment storage directory at {}", storageDir);
            }
        } catch (IOException e) {
            throw new StorageOperationException("Failed to initialize storage directory: " + storageDir, e);
        }
    }

    @Override
    public void store(String storageKey, InputStream inputStream, long expectedSize) {
        Path targetPath = resolveSecurePath(storageKey);
        try {
            long written = Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            if (expectedSize > 0 && written != expectedSize) {
                // Incomplete stream write, clean up and throw
                deleteQuietly(targetPath);
                throw new StorageOperationException("Storage size mismatch. Expected " + expectedSize + " bytes but wrote " + written);
            }
        } catch (IOException e) {
            deleteQuietly(targetPath);
            throw new StorageOperationException("Failed to store file with storage key: " + storageKey, e);
        }
    }

    @Override
    public InputStream load(String storageKey) {
        Path targetPath = resolveSecurePath(storageKey);
        if (!Files.exists(targetPath) || !Files.isRegularFile(targetPath)) {
            throw new StorageOperationException("File not found on storage adapter for key: " + storageKey);
        }
        try {
            return Files.newInputStream(targetPath);
        } catch (IOException e) {
            throw new StorageOperationException("Failed to open input stream for storage key: " + storageKey, e);
        }
    }

    @Override
    public void delete(String storageKey) {
        Path targetPath = resolveSecurePath(storageKey);
        try {
            Files.deleteIfExists(targetPath);
            log.debug("Deleted storage file for key {}", storageKey);
        } catch (IOException e) {
            log.warn("Failed to delete storage file for key {}: {}", storageKey, e.getMessage());
        }
    }

    @Override
    public boolean exists(String storageKey) {
        Path targetPath = resolveSecurePath(storageKey);
        return Files.exists(targetPath) && Files.isRegularFile(targetPath);
    }

    private Path resolveSecurePath(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("Storage key must not be blank");
        }
        // Strict alphanumeric, dash and underscore sanitization to eliminate path traversal attacks
        String sanitizedKey = storageKey.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path targetPath = storageDir.resolve(sanitizedKey).normalize();

        if (!targetPath.startsWith(storageDir)) {
            throw new SecurityException("Potential path traversal detected for storage key: " + storageKey);
        }
        return targetPath;
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
