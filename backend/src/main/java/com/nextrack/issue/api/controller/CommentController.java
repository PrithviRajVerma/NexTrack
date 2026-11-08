package com.nextrack.issue.api.controller;

import com.nextrack.issue.api.dto.request.CreateCommentRequest;
import com.nextrack.issue.api.dto.response.CommentResponse;
import com.nextrack.issue.application.service.CommentService;
import com.nextrack.issue.domain.entity.Comment;
import com.nextrack.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/issues/{issueId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable UUID issueId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Comment comment = commentService.addComment(issueId, request.body(), principal.id());
        CommentResponse response = new CommentResponse(
                comment.getId(),
                comment.getIssueId(),
                comment.getAuthorId(),
                comment.getBody(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CommentResponse>> listComments(
            @PathVariable UUID issueId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<CommentResponse> comments = commentService.listComments(issueId, principal.id());
        return ResponseEntity.ok(comments);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID issueId,
            @PathVariable UUID commentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        commentService.deleteComment(issueId, commentId, principal.id());
        return ResponseEntity.noContent().build();
    }
}
