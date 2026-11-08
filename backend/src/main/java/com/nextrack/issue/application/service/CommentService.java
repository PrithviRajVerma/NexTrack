package com.nextrack.issue.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextrack.audit.application.service.AuditService;
import com.nextrack.issue.api.dto.response.CommentResponse;
import com.nextrack.issue.domain.entity.Comment;
import com.nextrack.issue.domain.entity.Issue;
import com.nextrack.issue.domain.repository.CommentRepository;
import com.nextrack.project.application.service.ProjectService;
import org.springframework.modulith.NamedInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@NamedInterface
@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final IssueService issueService;
    private final ProjectService projectService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public CommentService(
            CommentRepository commentRepository,
            IssueService issueService,
            ProjectService projectService,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.commentRepository = commentRepository;
        this.issueService = issueService;
        this.projectService = projectService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Comment addComment(UUID issueId, String body, UUID authorId) {
        Issue issue = issueService.getIssue(issueId, authorId);
        projectService.assertUserCanContribute(issue.getProjectId(), authorId);

        Comment comment = new Comment(issueId, authorId, body);
        Comment saved = commentRepository.save(comment);

        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "commentId", saved.getId().toString(),
                    "bodyLength", saved.getBody().length()
            ));
            auditService.recordEvent(
                    authorId,
                    "ISSUE",
                    issueId,
                    "COMMENT_ADDED",
                    null,
                    payload,
                    null
            );
        } catch (JsonProcessingException ignored) {}

        return saved;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(UUID issueId, UUID userId) {
        issueService.getIssue(issueId, userId); // Asserts user has access to project

        return commentRepository.findByIssueIdAndDeletedAtIsNullOrderByCreatedAtAsc(issueId)
                .stream()
                .map(c -> new CommentResponse(
                        c.getId(),
                        c.getIssueId(),
                        c.getAuthorId(),
                        c.getBody(),
                        c.getCreatedAt(),
                        c.getUpdatedAt()
                ))
                .toList();
    }

    @Transactional
    public void deleteComment(UUID issueId, UUID commentId, UUID userId) {
        Issue issue = issueService.getIssue(issueId, userId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        if (!comment.getIssueId().equals(issueId)) {
            throw new IllegalArgumentException("Comment does not belong to the specified issue");
        }

        if (comment.isDeleted()) {
            return;
        }

        // Author or Project Admin can delete comment
        boolean isAuthor = comment.getAuthorId().equals(userId);
        if (!isAuthor) {
            projectService.assertUserIsProjectAdmin(issue.getProjectId(), userId);
        }

        comment.softDelete();
        commentRepository.save(comment);

        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "commentId", comment.getId().toString()
            ));
            auditService.recordEvent(
                    userId,
                    "ISSUE",
                    issueId,
                    "COMMENT_DELETED",
                    payload,
                    null,
                    null
            );
        } catch (JsonProcessingException ignored) {}
    }
}
