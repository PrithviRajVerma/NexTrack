package com.nextrack.issue.api.controller;

import com.nextrack.audit.api.dto.response.AuditEventResponse;
import com.nextrack.issue.api.dto.request.CreateIssueRequest;
import com.nextrack.issue.api.dto.request.TransitionIssueRequest;
import com.nextrack.issue.api.dto.request.UpdateIssueRequest;
import com.nextrack.issue.api.dto.response.BoardViewResponse;
import com.nextrack.issue.api.dto.response.IssueResponse;
import com.nextrack.issue.api.mapper.IssueMapper;
import com.nextrack.issue.application.service.IssueService;
import com.nextrack.issue.domain.entity.Issue;
import com.nextrack.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class IssueController {

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @PostMapping("/projects/{projectId}/issues")
    public ResponseEntity<IssueResponse> createIssue(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateIssueRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Issue issue = issueService.createIssue(projectId, request, principal.id());
        String projectKey = issueService.getProjectKey(projectId);
        return ResponseEntity.status(HttpStatus.CREATED).body(IssueMapper.toResponse(issue, projectKey));
    }

    @GetMapping("/projects/{projectId}/issues")
    public ResponseEntity<Page<IssueResponse>> listProjectIssues(
            @PathVariable UUID projectId,
            Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Page<Issue> page = issueService.listIssues(projectId, pageable, principal.id());
        String projectKey = issueService.getProjectKey(projectId);
        Page<IssueResponse> responsePage = page.map(issue -> IssueMapper.toResponse(issue, projectKey));
        return ResponseEntity.ok(responsePage);
    }

    @GetMapping("/projects/{projectId}/board")
    public ResponseEntity<BoardViewResponse> getProjectBoard(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        BoardViewResponse board = issueService.getBoardView(projectId, principal.id());
        return ResponseEntity.ok(board);
    }

    @GetMapping("/issues/{issueId}")
    public ResponseEntity<IssueResponse> getIssue(
            @PathVariable UUID issueId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Issue issue = issueService.getIssue(issueId, principal.id());
        String projectKey = issueService.getProjectKey(issue.getProjectId());
        return ResponseEntity.ok(IssueMapper.toResponse(issue, projectKey));
    }

    @GetMapping("/projects/{projectId}/issues/{issueNumber}")
    public ResponseEntity<IssueResponse> getIssueByNumber(
            @PathVariable UUID projectId,
            @PathVariable long issueNumber,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Issue issue = issueService.getIssueByProjectAndNumber(projectId, issueNumber, principal.id());
        String projectKey = issueService.getProjectKey(projectId);
        return ResponseEntity.ok(IssueMapper.toResponse(issue, projectKey));
    }

    @PatchMapping("/issues/{issueId}")
    public ResponseEntity<IssueResponse> updateIssue(
            @PathVariable UUID issueId,
            @Valid @RequestBody UpdateIssueRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Issue issue = issueService.updateIssue(issueId, request, principal.id());
        String projectKey = issueService.getProjectKey(issue.getProjectId());
        return ResponseEntity.ok(IssueMapper.toResponse(issue, projectKey));
    }

    @PostMapping("/issues/{issueId}/transitions")
    public ResponseEntity<IssueResponse> transitionIssue(
            @PathVariable UUID issueId,
            @Valid @RequestBody TransitionIssueRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Issue issue = issueService.transitionIssue(issueId, request, principal.id());
        String projectKey = issueService.getProjectKey(issue.getProjectId());
        return ResponseEntity.ok(IssueMapper.toResponse(issue, projectKey));
    }

    @GetMapping("/issues/{issueId}/history")
    public ResponseEntity<List<AuditEventResponse>> getIssueHistory(
            @PathVariable UUID issueId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<AuditEventResponse> history = issueService.getIssueHistory(issueId, principal.id());
        return ResponseEntity.ok(history);
    }
}
