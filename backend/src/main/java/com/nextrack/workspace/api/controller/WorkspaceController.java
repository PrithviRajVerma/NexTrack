package com.nextrack.workspace.api.controller;

import com.nextrack.shared.security.UserPrincipal;
import com.nextrack.workspace.api.dto.request.AddMemberRequest;
import com.nextrack.workspace.api.dto.request.CreateWorkspaceRequest;
import com.nextrack.workspace.api.dto.response.WorkspaceMemberResponse;
import com.nextrack.workspace.api.dto.response.WorkspaceResponse;
import com.nextrack.workspace.api.mapper.WorkspaceMapper;
import com.nextrack.workspace.application.service.WorkspaceService;
import com.nextrack.workspace.domain.entity.Workspace;
import com.nextrack.workspace.domain.entity.WorkspaceMembership;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping
    public ResponseEntity<WorkspaceResponse> createWorkspace(
            @Valid @RequestBody CreateWorkspaceRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Workspace workspace = workspaceService.createWorkspace(
                request.name(),
                request.slug(),
                principal.id()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(WorkspaceMapper.toResponse(workspace));
    }

    @GetMapping
    public ResponseEntity<List<WorkspaceResponse>> listWorkspaces(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<WorkspaceResponse> workspaces = workspaceService.listWorkspacesForUser(principal.id()).stream()
                .map(WorkspaceMapper::toResponse)
                .toList();
        return ResponseEntity.ok(workspaces);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkspaceResponse> getWorkspace(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Workspace workspace = workspaceService.getWorkspace(id, principal.id());
        return ResponseEntity.ok(WorkspaceMapper.toResponse(workspace));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<WorkspaceMemberResponse>> listMembers(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<WorkspaceMemberResponse> members = workspaceService.listMembers(id, principal.id()).stream()
                .map(WorkspaceMapper::toMemberResponse)
                .toList();
        return ResponseEntity.ok(members);
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<WorkspaceMemberResponse> addMember(
            @PathVariable UUID id,
            @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        WorkspaceMembership membership = workspaceService.addMember(
                id,
                request.userId(),
                request.role(),
                principal.id()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(WorkspaceMapper.toMemberResponse(membership));
    }
}
