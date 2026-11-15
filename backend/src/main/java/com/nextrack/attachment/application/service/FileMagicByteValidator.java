package com.nextrack.attachment.application.service;

import com.nextrack.shared.exception.DisallowedFileTypeException;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class FileMagicByteValidator {

    private static final Logger log = LoggerFactory.getLogger(FileMagicByteValidator.class);
    private static final int HEADER_READ_LIMIT = 4096;

    private final Tika tika = new Tika();

    private static final Map<String, Set<String>> EXTENSION_TO_MIME = Map.ofEntries(
            Map.entry("jpg", Set.of("image/jpeg")),
            Map.entry("jpeg", Set.of("image/jpeg")),
            Map.entry("png", Set.of("image/png")),
            Map.entry("gif", Set.of("image/gif")),
            Map.entry("pdf", Set.of("application/pdf")),
            Map.entry("zip", Set.of("application/zip", "application/x-zip-compressed")),
            Map.entry("txt", Set.of("text/plain")),
            Map.entry("md", Set.of("text/plain", "text/markdown", "text/x-web-markdown")),
            Map.entry("csv", Set.of("text/plain", "text/csv")),
            Map.entry("json", Set.of("text/plain", "application/json")),
            Map.entry("log", Set.of("text/plain"))
    );

    private static final Set<String> ALLOWED_MIME_PREFIXES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "application/pdf",
            "application/zip",
            "application/x-zip-compressed",
            "text/plain",
            "text/markdown",
            "text/csv",
            "application/json"
    );

    public record ValidationResult(String detectedMimeType, String sanitizedFilename) {}

    /**
     * Inspects the input stream's magic bytes, validates file integrity against the declared filename extension,
     * and guarantees that spoofed executables or unwhitelisted types are rejected.
     */
    public ValidationResult validate(String originalFilename, byte[] headerBytes, long totalSize) {
        String sanitized = sanitizeFilename(originalFilename);
        String extension = extractExtension(sanitized).toLowerCase(Locale.ROOT);

        if (!EXTENSION_TO_MIME.containsKey(extension)) {
            throw new DisallowedFileTypeException(
                    "File extension '." + extension + "' is not permitted. Allowed: " + EXTENSION_TO_MIME.keySet()
            );
        }

        if (headerBytes == null || headerBytes.length == 0) {
            if (totalSize == 0 && (extension.equals("txt") || extension.equals("md") || extension.equals("log"))) {
                return new ValidationResult("text/plain", sanitized);
            }
            throw new DisallowedFileTypeException("File payload is empty or unreadable");
        }

        // 1. Explicit dangerous signature checks (prevent Trojan/EXE spoofing under .png, .pdf, etc.)
        assertNoMaliciousSignatures(headerBytes);

        // 2. Explicit signature matching
        String explicitMime = detectExplicitMagicBytes(headerBytes);

        // 3. Apache Tika secondary heuristic inspection
        String tikaMime = tika.detect(headerBytes, sanitized);

        String effectiveMime = explicitMime != null ? explicitMime : tikaMime;

        // Clean parameters e.g., "text/plain; charset=ISO-8859-1" -> "text/plain"
        if (effectiveMime != null && effectiveMime.contains(";")) {
            effectiveMime = effectiveMime.substring(0, effectiveMime.indexOf(';')).trim();
        }

        // 4. Verify detected MIME is in whitelist
        if (effectiveMime == null || !ALLOWED_MIME_PREFIXES.contains(effectiveMime)) {
            throw new DisallowedFileTypeException(
                    "Disallowed MIME type detected: '" + effectiveMime + "'. Only safe document and image formats are allowed."
            );
        }

        // 5. Cross-check extension against detected MIME (anti-spoofing guard)
        Set<String> allowedMimesForExt = EXTENSION_TO_MIME.get(extension);
        if (allowedMimesForExt != null && !allowedMimesForExt.contains(effectiveMime)) {
            // Special handling for text-based formats detected as text/plain
            boolean isTextCompat = allowedMimesForExt.contains("text/plain") && effectiveMime.startsWith("text/");
            if (!isTextCompat) {
                log.warn("MIME spoofing detected: filename='{}', declaredExt='{}', detectedMime='{}'",
                        sanitized, extension, effectiveMime);
                throw new DisallowedFileTypeException(
                        "File content does not match the declared extension '." + extension +
                                "'. Detected MIME type: " + effectiveMime
                );
            }
        }

        return new ValidationResult(effectiveMime, sanitized);
    }

    private String detectExplicitMagicBytes(byte[] b) {
        if (b.length >= 8) {
            // PNG: 89 50 4E 47 0D 0A 1A 0A
            if ((b[0] & 0xFF) == 0x89 && b[1] == 0x50 && b[2] == 0x4E && b[3] == 0x47 &&
                b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
                return "image/png";
            }
        }

        if (b.length >= 3) {
            // JPEG: FF D8 FF
            if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
                return "image/jpeg";
            }
        }

        if (b.length >= 6) {
            // GIF: "GIF87a" or "GIF89a"
            String header = new String(b, 0, 6, StandardCharsets.US_ASCII);
            if (header.equals("GIF87a") || header.equals("GIF89a")) {
                return "image/gif";
            }
        }

        if (b.length >= 5) {
            // PDF: "%PDF-"
            String header = new String(b, 0, 5, StandardCharsets.US_ASCII);
            if (header.equals("%PDF-")) {
                return "application/pdf";
            }
        }

        if (b.length >= 4) {
            // ZIP: "PK\003\004" or "PK\005\006" or "PK\007\008"
            if (b[0] == 0x50 && b[1] == 0x4B &&
                ((b[2] == 0x03 && b[3] == 0x04) ||
                 (b[2] == 0x05 && b[3] == 0x06) ||
                 (b[2] == 0x07 && b[3] == 0x08))) {
                return "application/zip";
            }
        }

        // Text check: verify no binary null bytes in the sample and mostly ASCII/UTF-8 printable
        if (isTextPayload(b)) {
            return "text/plain";
        }

        return null;
    }

    private void assertNoMaliciousSignatures(byte[] b) {
        if (b.length >= 2) {
            // Windows PE / MZ executable
            if (b[0] == 0x4D && b[1] == 0x5A) {
                throw new DisallowedFileTypeException("Executable binary (MZ/PE header) detected. Upload rejected.");
            }
        }
        if (b.length >= 4) {
            // Linux ELF executable: 7F 45 4C 46
            if (b[0] == 0x7F && b[1] == 0x45 && b[2] == 0x4C && b[3] == 0x46) {
                throw new DisallowedFileTypeException("Linux ELF executable header detected. Upload rejected.");
            }
            // Java class file: CA FE BA BE
            if ((b[0] & 0xFF) == 0xCA && (b[1] & 0xFF) == 0xFE &&
                (b[2] & 0xFF) == 0xBA && (b[3] & 0xFF) == 0xBE) {
                throw new DisallowedFileTypeException("Compiled Java bytecode (.class) detected. Upload rejected.");
            }
            // Mach-O binaries: FEEDFACE, FEEDFACF, CEFAEDFE, CFFAEDFE
            if (((b[0] & 0xFF) == 0xFE && (b[1] & 0xFF) == 0xED && (b[2] & 0xFF) == 0xFA) ||
                ((b[0] & 0xFF) == 0xCE && (b[1] & 0xFF) == 0xFA && (b[2] & 0xFF) == 0xED)) {
                throw new DisallowedFileTypeException("Mach-O executable binary detected. Upload rejected.");
            }
        }
    }

    private boolean isTextPayload(byte[] bytes) {
        int checkLen = Math.min(bytes.length, 512);
        for (int i = 0; i < checkLen; i++) {
            byte b = bytes[i];
            if (b == 0x00) {
                return false; // Binary null byte indicates non-text
            }
        }
        return true;
    }

    public String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "attachment-" + UUID.randomUUID().toString().substring(0, 8);
        }
        // Strip path traversal sequences (both / and \)
        String clean = filename.replace("\\", "/");
        int lastSlash = clean.lastIndexOf('/');
        if (lastSlash >= 0) {
            clean = clean.substring(lastSlash + 1);
        }
        // Replace non-whitelisted characters
        clean = clean.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (clean.isBlank() || clean.equals(".")) {
            clean = "attachment-" + UUID.randomUUID().toString().substring(0, 8);
        }
        return clean;
    }

    public String extractExtension(String filename) {
        int dotIdx = filename.lastIndexOf('.');
        if (dotIdx < 0 || dotIdx == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIdx + 1);
    }
}
