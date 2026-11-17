package com.nextrack.development.api.controller;

import com.nextrack.development.api.dto.DevelopmentDtos.*;
import com.nextrack.development.application.service.DevelopmentService;
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

    public DevelopmentController(DevelopmentService developmentService) {
        this.developmentService = developmentService;
    }

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
}
