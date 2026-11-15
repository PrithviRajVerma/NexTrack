package com.nextrack.attachment.domain.repository;

import com.nextrack.attachment.domain.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    List<Attachment> findByIssueIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID issueId);

    Optional<Attachment> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Attachment> findByIdAndIssueIdAndDeletedAtIsNull(UUID id, UUID issueId);

    long countByIssueIdAndDeletedAtIsNull(UUID issueId);
}
