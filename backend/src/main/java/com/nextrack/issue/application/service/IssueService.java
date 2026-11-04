package com.nextrack.issue.application.service;

import com.nextrack.issue.api.dto.request.CreateIssueRequest;
import com.nextrack.issue.api.dto.request.UpdateIssueRequest;
import com.nextrack.issue.domain.entity.Issue;
import com.nextrack.issue.domain.repository.IssueRepository;
import com.nextrack.project.application.service.ProjectService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.modulith.NamedInterface;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@NamedInterface
@Service
public class IssueService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 25;

    private final IssueRepository issueRepository;
    private final ProjectService projectService;

    public IssueService(
            IssueRepository issueRepository,
            ProjectService projectService
    ) {
        this.issueRepository = issueRepository;
        this.projectService = projectService;
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

        return issueRepository.save(issue);
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
