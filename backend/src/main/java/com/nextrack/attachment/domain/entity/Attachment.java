package com.nextrack.attachment.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attachments")
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "issue_id", nullable = false)
    private UUID issueId;

    @Column(name = "uploader_id", nullable = false)
    private UUID uploaderId;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "storage_key", nullable = false, unique = true, length = 128)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 128)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(nullable = false, length = 64)
    private String checksum;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Attachment() {}

    public Attachment(
            UUID issueId,
            UUID uploaderId,
            String originalFilename,
            String storageKey,
            String contentType,
            long sizeBytes,
            String checksum
    ) {
        this.issueId = issueId;
        this.uploaderId = uploaderId;
        this.originalFilename = originalFilename;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.checksum = checksum;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getIssueId() {
        return issueId;
    }

    public UUID getUploaderId() {
        return uploaderId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getChecksum() {
        return checksum;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void markDeleted() {
        this.deletedAt = Instant.now();
    }
}
