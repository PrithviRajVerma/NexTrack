package com.nextrack.attachment.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextrack.attachment.api.dto.response.AttachmentResponse;
import com.nextrack.attachment.application.port.StorageAdapter;
import com.nextrack.attachment.domain.entity.Attachment;
import com.nextrack.attachment.domain.repository.AttachmentRepository;
import com.nextrack.audit.application.service.AuditService;
import com.nextrack.issue.application.service.IssueService;
import com.nextrack.project.application.service.ProjectService;
import com.nextrack.shared.exception.AttachmentNotFoundException;
import com.nextrack.shared.exception.StorageOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.NamedInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@NamedInterface
@Service
public class AttachmentService {

    private static final Logger log = LoggerFactory.getLogger(AttachmentService.class);
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L; // 10 MB strict limit
    private static final int HEADER_PROBE_SIZE = 4096;

    private final AttachmentRepository attachmentRepository;
    private final StorageAdapter storageAdapter;
    private final FileMagicByteValidator magicByteValidator;
    private final IssueService issueService;
    private final ProjectService projectService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public record AttachmentDownload(InputStream stream, String filename, String contentType, long sizeBytes) {}

    public AttachmentService(
            AttachmentRepository attachmentRepository,
            StorageAdapter storageAdapter,
            FileMagicByteValidator magicByteValidator,
            IssueService issueService,
            ProjectService projectService,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.attachmentRepository = attachmentRepository;
        this.storageAdapter = storageAdapter;
        this.magicByteValidator = magicByteValidator;
        this.issueService = issueService;
        this.projectService = projectService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    public AttachmentResponse uploadAttachment(UUID issueId, MultipartFile file, UUID uploaderId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Attachment file cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new MaxUploadSizeExceededException(MAX_FILE_SIZE_BYTES);
        }

        // 1. Authorize user against issue & enclosing project
        UUID projectId = issueService.getIssueProjectId(issueId, uploaderId);
        projectService.assertUserCanContribute(projectId, uploaderId);

        // 2. Validate magic bytes and whitelist against declared extension
        byte[] headerBytes = extractHeaderBytes(file);
        FileMagicByteValidator.ValidationResult validation = magicByteValidator.validate(
                file.getOriginalFilename(),
                headerBytes,
                file.getSize()
        );

        // 3. Generate opaque server-side storage key
        String storageKey = UUID.randomUUID().toString();

        // 4. Stream to Storage Adapter while computing SHA-256 checksum
        String checksum;
        try (InputStream in = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream dis = new DigestInputStream(in, digest)) {
                storageAdapter.store(storageKey, dis, file.getSize());
                checksum = HexFormat.of().formatHex(digest.digest());
            }
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest algorithm not available", e);
        } catch (IOException e) {
            storageAdapter.delete(storageKey);
            throw new StorageOperationException("Failed to stream attachment to storage adapter", e);
        }

        // 5. Persist metadata to database with COMPENSATING ROLLBACK on failure
        Attachment attachment = new Attachment(
                issueId,
                uploaderId,
                validation.sanitizedFilename(),
                storageKey,
                validation.detectedMimeType(),
                file.getSize(),
                checksum
        );

        Attachment saved;
        try {
            saved = persistAttachmentMetadata(attachment);
        } catch (Exception ex) {
            log.error("Compensating action triggered: Deleting stored file {} due to DB persistence failure", storageKey, ex);
            storageAdapter.delete(storageKey);
            if (ex instanceof RuntimeException re) {
                throw re;
            }
            throw new StorageOperationException("Failed to persist attachment metadata", ex);
        }

        // 6. Record audit event
        try {
            String afterPayload = objectMapper.writeValueAsString(Map.of(
                    "attachmentId", saved.getId().toString(),
                    "filename", saved.getOriginalFilename(),
                    "sizeBytes", saved.getSizeBytes(),
                    "contentType", saved.getContentType()
            ));
            auditService.recordEvent(
                    uploaderId,
                    "ATTACHMENT",
                    saved.getId(),
                    "ATTACHMENT_UPLOADED",
                    null,
                    afterPayload,
                    null
            );
        } catch (JsonProcessingException ignored) {}

        log.info("Successfully uploaded attachment {} ({}) for issue {}",
                saved.getId(), saved.getOriginalFilename(), issueId);
        return AttachmentResponse.fromEntity(saved);
    }

    @Transactional
    public Attachment persistAttachmentMetadata(Attachment attachment) {
        return attachmentRepository.save(attachment);
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> listAttachments(UUID issueId, UUID requestingUserId) {
        // Verifies read permission on enclosing project
        issueService.getIssueProjectId(issueId, requestingUserId);

        return attachmentRepository.findByIssueIdAndDeletedAtIsNullOrderByCreatedAtDesc(issueId)
                .stream()
                .map(AttachmentResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public AttachmentDownload downloadAttachment(UUID issueId, UUID attachmentId, UUID requestingUserId) {
        // Verifies read permission on enclosing project
        issueService.getIssueProjectId(issueId, requestingUserId);

        Attachment attachment = attachmentRepository.findByIdAndIssueIdAndDeletedAtIsNull(attachmentId, issueId)
                .orElseThrow(() -> new AttachmentNotFoundException("Attachment not found with id: " + attachmentId));

        InputStream stream = storageAdapter.load(attachment.getStorageKey());
        return new AttachmentDownload(
                stream,
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                attachment.getSizeBytes()
        );
    }

    @Transactional
    public void deleteAttachment(UUID issueId, UUID attachmentId, UUID requestingUserId) {
        UUID projectId = issueService.getIssueProjectId(issueId, requestingUserId);
        projectService.assertUserCanContribute(projectId, requestingUserId);

        Attachment attachment = attachmentRepository.findByIdAndIssueIdAndDeletedAtIsNull(attachmentId, issueId)
                .orElseThrow(() -> new AttachmentNotFoundException("Attachment not found with id: " + attachmentId));

        attachment.markDeleted();
        attachmentRepository.save(attachment);

        try {
            String afterPayload = objectMapper.writeValueAsString(Map.of(
                    "attachmentId", attachment.getId().toString(),
                    "filename", attachment.getOriginalFilename()
            ));
            auditService.recordEvent(
                    requestingUserId,
                    "ATTACHMENT",
                    attachment.getId(),
                    "ATTACHMENT_DELETED",
                    null,
                    afterPayload,
                    null
            );
        } catch (JsonProcessingException ignored) {}

        log.info("Soft-deleted attachment {} for issue {}", attachmentId, issueId);
    }

    private byte[] extractHeaderBytes(MultipartFile file) {
        try (InputStream in = new BufferedInputStream(file.getInputStream())) {
            int toRead = (int) Math.min(file.getSize(), HEADER_PROBE_SIZE);
            byte[] header = new byte[toRead];
            int read = 0;
            while (read < toRead) {
                int r = in.read(header, read, toRead - read);
                if (r == -1) break;
                read += r;
            }
            if (read < toRead) {
                byte[] exact = new byte[read];
                System.arraycopy(header, 0, exact, 0, read);
                return exact;
            }
            return header;
        } catch (IOException e) {
            throw new StorageOperationException("Failed to read header bytes from uploaded file", e);
        }
    }
}
