package com.nextrack.development.api.controller;

import com.nextrack.development.api.dto.DevelopmentDtos.*;
import com.nextrack.development.application.service.DevelopmentService;
import com.nextrack.development.application.service.IssueTraceabilityService;
import com.nextrack.shared.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class DevelopmentController {

    private final DevelopmentService developmentService;
    private final IssueTraceabilityService issueTraceabilityService;

    public DevelopmentController(
            DevelopmentService developmentService,
            IssueTraceabilityService issueTraceabilityService
    ) {
        this.developmentService = developmentService;
        this.issueTraceabilityService = issueTraceabilityService;
    }

    // -------------------------------------------------------------------------
    // GitHub App & Repository Management
    // -------------------------------------------------------------------------

    @PostMapping("/workspaces/{workspaceId}/integrations/github")
    public ResponseEntity<InstallationResponse> registerInstallation(
            @PathVariable UUID workspaceId,
            @RequestBody CreateInstallationRequest request
    ) {
        InstallationResponse response = developmentService.registerInstallation(workspaceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/workspaces/{workspaceId}/integrations/github")
    public ResponseEntity<List<InstallationResponse>> listInstallations(@PathVariable UUID workspaceId) {
        List<InstallationResponse> response = developmentService.listInstallations(workspaceId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/integrations/github/installations/{installationId}/repositories")
    public ResponseEntity<ConnectedRepositoryResponse> connectRepository(
            @PathVariable UUID installationId,
            @RequestBody ConnectRepositoryRequest request
    ) {
        ConnectedRepositoryResponse response = developmentService.connectRepository(installationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/integrations/github/installations/{installationId}/repositories")
    public ResponseEntity<List<ConnectedRepositoryResponse>> listRepositories(@PathVariable UUID installationId) {
        List<ConnectedRepositoryResponse> response = developmentService.listRepositoriesForInstallation(installationId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/projects/{projectId}/repositories/link")
    public ResponseEntity<Void> linkRepository(
            @PathVariable UUID projectId,
            @RequestBody ProjectRepositoryLinkRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        developmentService.linkRepositoryToProject(projectId, request.repositoryId(), principal.id());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/projects/{projectId}/repositories/{repositoryId}")
    public ResponseEntity<Void> unlinkRepository(
            @PathVariable UUID projectId,
            @PathVariable UUID repositoryId
    ) {
        developmentService.unlinkRepositoryFromProject(projectId, repositoryId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/projects/{projectId}/repositories")
    public ResponseEntity<List<ConnectedRepositoryResponse>> listProjectRepositories(@PathVariable UUID projectId) {
        List<ConnectedRepositoryResponse> response = developmentService.listProjectRepositories(projectId);
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // Phase 9: Issue-to-Code Traceability Endpoints
    // -------------------------------------------------------------------------

    @GetMapping("/issues/{issueId}/development")
    public ResponseEntity<IssueDevelopmentResponse> getIssueDevelopment(
            @PathVariable UUID issueId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        IssueDevelopmentResponse response = issueTraceabilityService.getIssueDevelopment(issueId, principal.id());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/issues/{issueId}/development/links")
    public ResponseEntity<Void> linkArtifactToIssue(
            @PathVariable UUID issueId,
            @RequestBody ManualLinkRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        issueTraceabilityService.linkArtifactToIssue(issueId, request, principal.id());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/issues/{issueId}/development/links/{targetId}")
    public ResponseEntity<Void> unlinkArtifactFromIssue(
            @PathVariable UUID issueId,
            @PathVariable UUID targetId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        issueTraceabilityService.unlinkArtifactFromIssue(issueId, targetId, principal.id());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/issues/{issueId}/development/links/{linkType}/{targetId}")
    public ResponseEntity<Void> unlinkArtifactWithTypeFromIssue(
            @PathVariable UUID issueId,
            @PathVariable String linkType,
            @PathVariable UUID targetId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        issueTraceabilityService.unlinkArtifactFromIssue(issueId, linkType, targetId, principal.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/issues/{issueId}/development/candidates")
    public ResponseEntity<List<DevelopmentCandidateResponse>> searchCandidates(
            @PathVariable UUID issueId,
            @RequestParam(required = false, defaultValue = "COMMIT") String type,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<DevelopmentCandidateResponse> candidates = issueTraceabilityService.searchCandidates(
                issueId, type, search, principal.id()
        );
        return ResponseEntity.ok(candidates);
    }

    @GetMapping("/projects/{projectId}/development/overview")
    public ResponseEntity<ProjectDevelopmentOverviewResponse> getProjectDevelopmentOverview(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ProjectDevelopmentOverviewResponse response = issueTraceabilityService.getProjectDevelopmentOverview(
                projectId, principal.id()
        );
        return ResponseEntity.ok(response);
    }
}
