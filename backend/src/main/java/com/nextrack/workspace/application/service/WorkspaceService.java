package com.nextrack.workspace.application.service;

import com.nextrack.workspace.domain.entity.Workspace;
import com.nextrack.workspace.domain.entity.WorkspaceMembership;
import com.nextrack.workspace.domain.entity.WorkspaceRole;
import com.nextrack.workspace.domain.repository.WorkspaceMembershipRepository;
import com.nextrack.workspace.domain.repository.WorkspaceRepository;
import org.springframework.modulith.NamedInterface;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@NamedInterface
@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository membershipRepository;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMembershipRepository membershipRepository
    ) {
        this.workspaceRepository = workspaceRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public Workspace createWorkspace(String name, String slug, UUID ownerUserId) {
        String normalizedSlug = slug.toLowerCase().trim();
        if (workspaceRepository.existsBySlugIgnoreCase(normalizedSlug)) {
            throw new IllegalArgumentException("Workspace with slug '" + slug + "' already exists");
        }

        Workspace workspace = new Workspace(name, normalizedSlug, ownerUserId);
        workspace = workspaceRepository.save(workspace);

        // Creator automatically receives WORKSPACE_ADMIN role
        WorkspaceMembership adminMembership = new WorkspaceMembership(
                workspace.getId(),
                ownerUserId,
                WorkspaceRole.WORKSPACE_ADMIN
        );
        membershipRepository.save(adminMembership);

        return workspace;
    }

    @Transactional
    public WorkspaceMembership addMember(UUID workspaceId, UUID targetUserId, WorkspaceRole role, UUID requestingUserId) {
        assertUserIsWorkspaceAdmin(workspaceId, requestingUserId);

        if (membershipRepository.existsByWorkspaceIdAndUserId(workspaceId, targetUserId)) {
            throw new IllegalArgumentException("User is already a member of this workspace");
        }

        WorkspaceMembership membership = new WorkspaceMembership(workspaceId, targetUserId, role);
        return membershipRepository.save(membership);
    }

    @Transactional(readOnly = true)
    public List<Workspace> listWorkspacesForUser(UUID userId) {
        List<UUID> workspaceIds = membershipRepository.findByUserId(userId).stream()
                .map(WorkspaceMembership::getWorkspaceId)
                .toList();
        return workspaceRepository.findAllById(workspaceIds);
    }

    @Transactional(readOnly = true)
    public Workspace getWorkspace(UUID workspaceId, UUID requestingUserId) {
        assertUserIsMember(workspaceId, requestingUserId);
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMembership> listMembers(UUID workspaceId, UUID requestingUserId) {
        assertUserIsMember(workspaceId, requestingUserId);
        return membershipRepository.findByWorkspaceId(workspaceId);
    }

    public void assertUserIsMember(UUID workspaceId, UUID userId) {
        if (!membershipRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new AccessDeniedException("User is not a member of this workspace");
        }
    }

    public void assertUserIsWorkspaceAdmin(UUID workspaceId, UUID userId) {
        WorkspaceMembership membership = membershipRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new AccessDeniedException("User is not a member of this workspace"));
        if (membership.getRole() != WorkspaceRole.WORKSPACE_ADMIN) {
            throw new AccessDeniedException("Operation requires WORKSPACE_ADMIN privileges");
        }
    }
}
