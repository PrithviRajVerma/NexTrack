package com.nextrack.project.application.service;

import com.nextrack.project.domain.entity.*;
import com.nextrack.project.domain.repository.ProjectIssueCounterRepository;
import com.nextrack.project.domain.repository.ProjectMembershipRepository;
import com.nextrack.project.domain.repository.ProjectRepository;
import com.nextrack.workspace.application.service.WorkspaceService;
import org.springframework.modulith.NamedInterface;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@NamedInterface
@Service
public class ProjectService {

    private static final Pattern PROJECT_KEY_PATTERN = Pattern.compile("^[A-Z][A-Z0-9]{1,9}$");

    private final ProjectRepository projectRepository;
    private final ProjectMembershipRepository membershipRepository;
    private final ProjectIssueCounterRepository counterRepository;
    private final WorkspaceService workspaceService;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectMembershipRepository membershipRepository,
            ProjectIssueCounterRepository counterRepository,
            WorkspaceService workspaceService
    ) {
        this.projectRepository = projectRepository;
        this.membershipRepository = membershipRepository;
        this.counterRepository = counterRepository;
        this.workspaceService = workspaceService;
    }

    @Transactional
    public Project createProject(UUID workspaceId, String name, String rawProjectKey, String description, UUID creatorId) {
        String projectKey = rawProjectKey.toUpperCase().trim();
        if (!PROJECT_KEY_PATTERN.matcher(projectKey).matches()) {
            throw new IllegalArgumentException("Project key must start with an uppercase letter, be alphanumeric, and between 2 and 10 characters (e.g. 'NT', 'CORE')");
        }

        // Creator must hold active membership in enclosing workspace
        workspaceService.assertUserIsMember(workspaceId, creatorId);

        if (projectRepository.existsByWorkspaceIdAndProjectKeyIgnoreCase(workspaceId, projectKey)) {
            throw new IllegalArgumentException("Project key '" + projectKey + "' already exists in this workspace");
        }

        Project project = new Project(workspaceId, name, projectKey, description, creatorId);
        project = projectRepository.save(project);

        // Creator receives PROJECT_ADMIN role
        ProjectMembership adminMembership = new ProjectMembership(
                project.getId(),
                creatorId,
                ProjectRole.PROJECT_ADMIN
        );
        membershipRepository.save(adminMembership);

        // Atomically initialize issue counter for project
        ProjectIssueCounter counter = new ProjectIssueCounter(project.getId());
        counterRepository.save(counter);

        return project;
    }

    @Transactional
    public ProjectMembership addProjectMember(UUID projectId, UUID targetUserId, ProjectRole role, UUID requestingUserId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        assertUserIsProjectAdmin(projectId, requestingUserId);

        // Workspace Membership Inheritance: user MUST belong to the workspace before being assigned to the project
        workspaceService.assertUserIsMember(project.getWorkspaceId(), targetUserId);

        if (membershipRepository.existsByProjectIdAndUserId(projectId, targetUserId)) {
            throw new IllegalArgumentException("User is already a member of this project");
        }

        ProjectMembership membership = new ProjectMembership(projectId, targetUserId, role);
        return membershipRepository.save(membership);
    }

    @Transactional(readOnly = true)
    public List<Project> listProjectsInWorkspace(UUID workspaceId, UUID requestingUserId) {
        workspaceService.assertUserIsMember(workspaceId, requestingUserId);
        return projectRepository.findByWorkspaceId(workspaceId);
    }

    @Transactional(readOnly = true)
    public Project getProject(UUID projectId, UUID requestingUserId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        workspaceService.assertUserIsMember(project.getWorkspaceId(), requestingUserId);
        return project;
    }

    @Transactional(readOnly = true)
    public List<ProjectMembership> listMembers(UUID projectId, UUID requestingUserId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        workspaceService.assertUserIsMember(project.getWorkspaceId(), requestingUserId);
        return membershipRepository.findByProjectId(projectId);
    }

    public boolean isUserProjectMember(UUID projectId, UUID userId) {
        return membershipRepository.existsByProjectIdAndUserId(projectId, userId);
    }

    public void assertUserIsProjectAdmin(UUID projectId, UUID userId) {
        ProjectMembership membership = membershipRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new AccessDeniedException("User is not a member of this project"));
        if (membership.getRole() != ProjectRole.PROJECT_ADMIN) {
            throw new AccessDeniedException("Operation requires PROJECT_ADMIN privileges");
        }
    }
}
