package com.nextrack.attachment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextrack.attachment.application.port.StorageAdapter;
import com.nextrack.attachment.application.service.AttachmentService;
import com.nextrack.attachment.application.service.FileMagicByteValidator;
import com.nextrack.attachment.domain.entity.Attachment;
import com.nextrack.attachment.domain.repository.AttachmentRepository;
import com.nextrack.audit.application.service.AuditService;
import com.nextrack.issue.application.service.IssueService;
import com.nextrack.project.application.service.ProjectService;
import com.nextrack.shared.exception.StorageOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceCompensatingTests {

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private StorageAdapter storageAdapter;

    @Mock
    private IssueService issueService;

    @Mock
    private ProjectService projectService;

    @Mock
    private AuditService auditService;

    private FileMagicByteValidator magicByteValidator;
    private AttachmentService attachmentService;

    @BeforeEach
    void setUp() {
        magicByteValidator = new FileMagicByteValidator();
        attachmentService = new AttachmentService(
                attachmentRepository,
                storageAdapter,
                magicByteValidator,
                issueService,
                projectService,
                auditService,
                new ObjectMapper()
        );
    }

    @Test
    @DisplayName("Compensating action: stored file is deleted immediately if DB metadata persistence fails")
    void storageCleanedUpWhenDbFails() {
        UUID issueId = UUID.randomUUID();
        UUID uploaderId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        when(issueService.getIssueProjectId(issueId, uploaderId)).thenReturn(projectId);
        doNothing().when(projectService).assertUserCanContribute(projectId, uploaderId);

        // Simulate DB failure on save
        when(attachmentRepository.save(any(Attachment.class)))
                .thenThrow(new DataIntegrityViolationException("Simulated unique constraint error"));

        byte[] validPng = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile file = new MockMultipartFile("file", "diagram.png", "image/png", validPng);

        assertThatThrownBy(() -> attachmentService.uploadAttachment(issueId, file, uploaderId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("Simulated unique constraint error");

        // Verify that storageAdapter.store was called, and subsequently storageAdapter.delete was called to compensate!
        verify(storageAdapter, times(1)).store(anyString(), any(), eq((long) validPng.length));
        verify(storageAdapter, times(1)).delete(anyString());
    }
}
