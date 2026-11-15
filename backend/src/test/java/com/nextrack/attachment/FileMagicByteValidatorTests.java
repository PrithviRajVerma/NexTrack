package com.nextrack.attachment;

import com.nextrack.attachment.application.service.FileMagicByteValidator;
import com.nextrack.shared.exception.DisallowedFileTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileMagicByteValidatorTests {

    private FileMagicByteValidator validator;

    @BeforeEach
    void setUp() {
        validator = new FileMagicByteValidator();
    }

    @Test
    @DisplayName("Valid PNG magic bytes and extension are accepted")
    void validPngAccepted() {
        byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00};
        var result = validator.validate("screenshot.png", pngBytes, pngBytes.length);

        assertThat(result.detectedMimeType()).isEqualTo("image/png");
        assertThat(result.sanitizedFilename()).isEqualTo("screenshot.png");
    }

    @Test
    @DisplayName("Valid JPEG magic bytes and extension are accepted")
    void validJpegAccepted() {
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
        var result = validator.validate("photo.jpg", jpegBytes, jpegBytes.length);

        assertThat(result.detectedMimeType()).isEqualTo("image/jpeg");
        assertThat(result.sanitizedFilename()).isEqualTo("photo.jpg");
    }

    @Test
    @DisplayName("Valid PDF header and extension are accepted")
    void validPdfAccepted() {
        byte[] pdfBytes = "%PDF-1.7\n1 0 obj...".getBytes(StandardCharsets.US_ASCII);
        var result = validator.validate("invoice.pdf", pdfBytes, pdfBytes.length);

        assertThat(result.detectedMimeType()).isEqualTo("application/pdf");
        assertThat(result.sanitizedFilename()).isEqualTo("invoice.pdf");
    }

    @Test
    @DisplayName("Valid ZIP header and extension are accepted")
    void validZipAccepted() {
        byte[] zipBytes = new byte[]{0x50, 0x4B, 0x03, 0x04, 0x14, 0x00};
        var result = validator.validate("archive.zip", zipBytes, zipBytes.length);

        assertThat(result.detectedMimeType()).isEqualTo("application/zip");
        assertThat(result.sanitizedFilename()).isEqualTo("archive.zip");
    }

    @Test
    @DisplayName("Valid text file is accepted")
    void validTextAccepted() {
        byte[] textBytes = "This is a plain text log file\nLine 2".getBytes(StandardCharsets.UTF_8);
        var result = validator.validate("server.log", textBytes, textBytes.length);

        assertThat(result.detectedMimeType()).isEqualTo("text/plain");
        assertThat(result.sanitizedFilename()).isEqualTo("server.log");
    }

    @Test
    @DisplayName("Windows PE/MZ executable renamed to .png is rejected as spoofed malware")
    void spoofedExecutablePngRejected() {
        byte[] peBytes = new byte[]{0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00};

        assertThatThrownBy(() -> validator.validate("trojan.png", peBytes, peBytes.length))
                .isInstanceOf(DisallowedFileTypeException.class)
                .hasMessageContaining("Executable binary (MZ/PE header) detected");
    }

    @Test
    @DisplayName("Linux ELF binary renamed to .pdf is rejected")
    void spoofedElfBinaryPdfRejected() {
        byte[] elfBytes = new byte[]{0x7F, 0x45, 0x4C, 0x46, 0x02, 0x01};

        assertThatThrownBy(() -> validator.validate("rootkit.pdf", elfBytes, elfBytes.length))
                .isInstanceOf(DisallowedFileTypeException.class)
                .hasMessageContaining("Linux ELF executable header detected");
    }

    @Test
    @DisplayName("Mismatched content type (valid PNG bytes named .pdf) is rejected as extension spoof")
    void mismatchedContentAndExtensionRejected() {
        byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

        assertThatThrownBy(() -> validator.validate("report.pdf", pngBytes, pngBytes.length))
                .isInstanceOf(DisallowedFileTypeException.class)
                .hasMessageContaining("File content does not match the declared extension '.pdf'");
    }

    @Test
    @DisplayName("Unwhitelisted file extensions (.exe, .sh, .html) are rejected immediately")
    void unwhitelistedExtensionsRejected() {
        byte[] dummy = "echo hello".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> validator.validate("script.sh", dummy, dummy.length))
                .isInstanceOf(DisallowedFileTypeException.class)
                .hasMessageContaining("File extension '.sh' is not permitted");

        assertThatThrownBy(() -> validator.validate("index.html", dummy, dummy.length))
                .isInstanceOf(DisallowedFileTypeException.class)
                .hasMessageContaining("File extension '.html' is not permitted");
    }

    @Test
    @DisplayName("Path traversal sequences in filename are sanitized")
    void pathTraversalSanitized() {
        byte[] pdfBytes = "%PDF-1.4\n...".getBytes(StandardCharsets.US_ASCII);
        var result = validator.validate("../../etc/passwd.pdf", pdfBytes, pdfBytes.length);

        assertThat(result.sanitizedFilename()).doesNotContain("/");
        assertThat(result.sanitizedFilename()).doesNotContain("..");
        assertThat(result.sanitizedFilename()).isEqualTo("passwd.pdf");
    }
}
