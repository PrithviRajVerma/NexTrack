package com.nextrack.project.api;

import com.nextrack.project.application.ProjectService;
import com.nextrack.project.domain.Project;
import com.nextrack.project.domain.ProjectMembership;
import com.nextrack.project.domain.ProjectRole;
import com.nextrack.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping("/workspaces/{workspaceId}/projects")
    public ResponseEntity<ProjectResponse> createProject(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Project project = projectService.createProject(
                workspaceId,
                request.name(),
                request.projectKey(),
                request.description(),
                principal.id()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectResponse.from(project));
    }

    @GetMapping("/workspaces/{workspaceId}/projects")
    public ResponseEntity<List<ProjectResponse>> listProjectsInWorkspace(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<ProjectResponse> projects = projectService.listProjectsInWorkspace(workspaceId, principal.id()).stream()
                .map(ProjectResponse::from)
                .toList();
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/projects/{id}")
    public ResponseEntity<ProjectResponse> getProject(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Project project = projectService.getProject(id, principal.id());
        return ResponseEntity.ok(ProjectResponse.from(project));
    }

    @GetMapping("/projects/{id}/members")
    public ResponseEntity<List<ProjectMemberResponse>> listMembers(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<ProjectMemberResponse> members = projectService.listMembers(id, principal.id()).stream()
                .map(ProjectMemberResponse::from)
                .toList();
        return ResponseEntity.ok(members);
    }

    @PostMapping("/projects/{id}/members")
    public ResponseEntity<ProjectMemberResponse> addMember(
            @PathVariable UUID id,
            @RequestBody AddProjectMemberRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ProjectMembership membership = projectService.addProjectMember(
                id,
                request.userId(),
                request.role(),
                principal.id()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectMemberResponse.from(membership));
    }

    public record AddProjectMemberRequest(UUID userId, ProjectRole role) {}
}
