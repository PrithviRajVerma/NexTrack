package com.nextrack.attachment.api.controller;

import com.nextrack.attachment.api.dto.response.AttachmentResponse;
import com.nextrack.attachment.application.service.AttachmentService;
import com.nextrack.shared.security.UserPrincipal;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/issues/{issueId}/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> uploadAttachment(
            @PathVariable UUID issueId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AttachmentResponse response = attachmentService.uploadAttachment(issueId, file, principal.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AttachmentResponse>> listAttachments(
            @PathVariable UUID issueId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<AttachmentResponse> attachments = attachmentService.listAttachments(issueId, principal.id());
        return ResponseEntity.ok(attachments);
    }

    @GetMapping("/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable UUID issueId,
            @PathVariable UUID attachmentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AttachmentService.AttachmentDownload download = attachmentService.downloadAttachment(
                issueId,
                attachmentId,
                principal.id()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(download.contentType()));
        headers.setContentLength(download.sizeBytes());
        headers.setContentDispositionFormData("attachment", download.filename());

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(download.stream()));
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable UUID issueId,
            @PathVariable UUID attachmentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        attachmentService.deleteAttachment(issueId, attachmentId, principal.id());
        return ResponseEntity.noContent().build();
    }
}
