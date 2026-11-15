package com.nextrack.attachment.api.dto.response;

import com.nextrack.attachment.domain.entity.Attachment;

import java.time.Instant;
import java.util.UUID;

public record AttachmentResponse(
        UUID id,
        UUID issueId,
        UUID uploaderId,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String checksum,
        Instant createdAt
) {
    public static AttachmentResponse fromEntity(Attachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getIssueId(),
                attachment.getUploaderId(),
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getChecksum(),
                attachment.getCreatedAt()
        );
    }
}
