package com.nextrack.issue.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextrack.audit.api.dto.response.AuditEventResponse;
import com.nextrack.audit.application.service.AuditService;
import com.nextrack.issue.api.dto.request.CreateIssueRequest;
import com.nextrack.issue.api.dto.request.TransitionIssueRequest;
import com.nextrack.issue.api.dto.request.UpdateIssueRequest;
import com.nextrack.issue.domain.entity.Issue;
import com.nextrack.issue.domain.entity.IssueStatus;
import com.nextrack.issue.domain.repository.IssueRepository;
import com.nextrack.issue.domain.workflow.IssueStatusTransitionRule;
import com.nextrack.project.application.service.ProjectService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.modulith.NamedInterface;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@NamedInterface
@Service
public class IssueService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 25;

    private final IssueRepository issueRepository;
    private final ProjectService projectService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public IssueService(
            IssueRepository issueRepository,
            ProjectService projectService,
            AuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.issueRepository = issueRepository;
        this.projectService = projectService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Issue createIssue(UUID projectId, CreateIssueRequest request, UUID requestingUserId) {
        // 1. Authorize: User must hold CONTRIBUTOR or PROJECT_ADMIN role in project
        projectService.assertUserCanContribute(projectId, requestingUserId);

        // 2. Validate Assignee: Must be an active member of this project
        if (request.assigneeId() != null) {
            assertAssigneeInProject(projectId, request.assigneeId());
        }

        // 3. Atomically allocate next sequential number (UPDATE ... RETURNING)
        long issueNumber = projectService.allocateNextIssueNumber(projectId);

        // 4. Save Issue Aggregate
        Issue issue = new Issue(
                projectId,
                issueNumber,
                request.title(),
                request.description(),
                request.issueType(),
                request.priority(),
                requestingUserId,
                request.assigneeId(),
                request.dueDate(),
                request.storyPoints()
        );

        Issue saved = issueRepository.save(issue);

        // 5. Atomic Audit Logging for issue creation
        try {
            String afterPayload = objectMapper.writeValueAsString(Map.of(
                    "issueNumber", saved.getIssueNumber(),
                    "title", saved.getTitle(),
                    "status", saved.getStatus().name()
            ));
            auditService.recordEvent(
                    requestingUserId,
                    "ISSUE",
                    saved.getId(),
                    "ISSUE_CREATED",
                    null,
                    afterPayload,
                    null
            );
        } catch (JsonProcessingException ignored) {}

        return saved;
    }

    @Transactional(readOnly = true)
    public Issue getIssue(UUID issueId, UUID requestingUserId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue not found"));

        assertUserCanAccessProject(issue.getProjectId(), requestingUserId);
        return issue;
    }

    @Transactional(readOnly = true)
    public Issue getIssueByProjectAndNumber(UUID projectId, long issueNumber, UUID requestingUserId) {
        assertUserCanAccessProject(projectId, requestingUserId);

        return issueRepository.findByProjectIdAndIssueNumber(projectId, issueNumber)
                .orElseThrow(() -> new IllegalArgumentException("Issue " + issueNumber + " not found in project"));
    }

    @Transactional(readOnly = true)
    public Page<Issue> listIssues(UUID projectId, Pageable pageable, UUID requestingUserId) {
        assertUserCanAccessProject(projectId, requestingUserId);

        int pageSize = pageable.getPageSize() <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        int pageNumber = Math.max(pageable.getPageNumber(), 0);
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "createdAt");

        Pageable boundedPageable = PageRequest.of(pageNumber, pageSize, sort);
        return issueRepository.findByProjectId(projectId, boundedPageable);
    }

    @Transactional
    public Issue updateIssue(UUID issueId, UpdateIssueRequest request, UUID requestingUserId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue not found"));

        projectService.assertUserCanContribute(issue.getProjectId(), requestingUserId);

        if (request.assigneeId() != null && !request.assigneeId().equals(issue.getAssigneeId())) {
            assertAssigneeInProject(issue.getProjectId(), request.assigneeId());
        }

        issue.updateDetails(
                request.title(),
                request.description(),
                request.issueType(),
                request.priority(),
                request.assigneeId(),
                request.dueDate(),
                request.storyPoints()
        );

        return issueRepository.save(issue);
    }

    @Transactional
    public Issue transitionIssue(UUID issueId, TransitionIssueRequest request, UUID requestingUserId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalArgumentException("Issue not found"));

        projectService.assertUserCanContribute(issue.getProjectId(), requestingUserId);

        // Optimistic locking version guard
        if (request.version() != null && !request.version().equals(issue.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(Issue.class, issue.getId());
        }

        IssueStatus fromStatus = issue.getStatus();
        IssueStatus toStatus = request.targetStatus();

        // Validate state transition through fixed finite state machine (ADR-07)
        IssueStatusTransitionRule.validateTransition(fromStatus, toStatus);

        if (toStatus == IssueStatus.ARCHIVED) {
            issue.archive();
        } else {
            issue.updateStatus(toStatus);
        }

        Issue saved = issueRepository.saveAndFlush(issue);

        // Atomic audit logging
        try {
            String beforePayload = objectMapper.writeValueAsString(Map.of(
                    "status", fromStatus.name(),
                    "version", request.version()
            ));
            String afterPayload = objectMapper.writeValueAsString(Map.of(
                    "status", saved.getStatus().name(),
                    "version", saved.getVersion()
            ));
            auditService.recordEvent(
                    requestingUserId,
                    "ISSUE",
                    saved.getId(),
                    "STATUS_TRANSITION",
                    beforePayload,
                    afterPayload,
                    null
            );
        } catch (JsonProcessingException ignored) {}

        return saved;
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> getIssueHistory(UUID issueId, UUID requestingUserId) {
        Issue issue = getIssue(issueId, requestingUserId);
        return auditService.getAuditHistory("ISSUE", issue.getId());
    }

    @Transactional(readOnly = true)
    public String getProjectKey(UUID projectId) {
        return projectService.getProjectKey(projectId);
    }

    private void assertAssigneeInProject(UUID projectId, UUID assigneeId) {
        if (!projectService.isUserProjectMember(projectId, assigneeId)) {
            throw new IllegalArgumentException("Assignee must be an active member of this project");
        }
    }

    private void assertUserCanAccessProject(UUID projectId, UUID userId) {
        if (!projectService.isUserProjectMember(projectId, userId)) {
            throw new AccessDeniedException("User is not a member of this project");
        }
    }
}

