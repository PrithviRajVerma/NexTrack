package com.nextrack.development.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.nextrack.audit.application.service.AuditService;
import com.nextrack.development.api.dto.DevelopmentDtos.*;
import com.nextrack.development.application.service.IssueKeyParser.ParsedIssueKey;
import com.nextrack.development.domain.entity.*;
import com.nextrack.development.domain.repository.*;
import com.nextrack.identity.application.service.AuthService;
import com.nextrack.issue.application.service.IssueService;
import com.nextrack.project.application.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.modulith.NamedInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@NamedInterface
@Service
public class IssueTraceabilityService {

    private static final Logger log = LoggerFactory.getLogger(IssueTraceabilityService.class);

    private final ConnectedRepositoryRepository connectedRepositoryRepository;
    private final ProjectConnectedRepositoryRepository projectConnectedRepositoryRepository;
    private final DevelopmentBranchRepository branchRepository;
    private final DevelopmentCommitRepository commitRepository;
    private final PullRequestRepository pullRequestRepository;
    private final IssueDevelopmentBranchRepository issueBranchRepository;
    private final IssueDevelopmentCommitRepository issueCommitRepository;
    private final IssuePullRequestRepository issuePullRequestRepository;
    private final IssueService issueService;
    private final ProjectService projectService;
    private final AuthService authService;
    private final AuditService auditService;

    public IssueTraceabilityService(
            ConnectedRepositoryRepository connectedRepositoryRepository,
            ProjectConnectedRepositoryRepository projectConnectedRepositoryRepository,
            DevelopmentBranchRepository branchRepository,
            DevelopmentCommitRepository commitRepository,
            PullRequestRepository pullRequestRepository,
            IssueDevelopmentBranchRepository issueBranchRepository,
            IssueDevelopmentCommitRepository issueCommitRepository,
            IssuePullRequestRepository issuePullRequestRepository,
            IssueService issueService,
            ProjectService projectService,
            AuthService authService,
            AuditService auditService
    ) {
        this.connectedRepositoryRepository = connectedRepositoryRepository;
        this.projectConnectedRepositoryRepository = projectConnectedRepositoryRepository;
        this.branchRepository = branchRepository;
        this.commitRepository = commitRepository;
        this.pullRequestRepository = pullRequestRepository;
        this.issueBranchRepository = issueBranchRepository;
        this.issueCommitRepository = issueCommitRepository;
        this.issuePullRequestRepository = issuePullRequestRepository;
        this.issueService = issueService;
        this.projectService = projectService;
        this.authService = authService;
        this.auditService = auditService;
    }

    /**
     * Ingests a GitHub push event payload: updates branch, commits, and auto-links issue keys.
     */
    @Transactional
    public void processPushEvent(JsonNode payload) {
        ConnectedRepository repo = resolveRepository(payload);
        if (repo == null) {
            log.warn("Skipping push processing: repository not recognized in NexTrack");
            return;
        }

        repo.setLastSyncedAt(Instant.now());
        connectedRepositoryRepository.save(repo);

        String ref = payload.path("ref").asText("");
        String branchName = ref.startsWith("refs/heads/") ? ref.substring("refs/heads/".length()) : ref;
        String headSha = payload.path("after").asText(null);

        DevelopmentBranch branch;
        if (!branchName.isBlank()) {
            branch = branchRepository.findByRepositoryIdAndName(repo.getId(), branchName)
                    .map(existing -> {
                        if (headSha != null && !headSha.isBlank()) {
                            existing.updateLastCommitSha(headSha);
                        }
                        return branchRepository.save(existing);
                    })
                    .orElseGet(() -> branchRepository.save(new DevelopmentBranch(repo.getId(), branchName, headSha)));

            // Extract issue keys from branch name and auto-link
            Set<ParsedIssueKey> branchKeys = IssueKeyParser.extractIssueKeys(branchName);
            linkBranchToMatchingIssues(branch, branchKeys);
        }

        // Process commits
        JsonNode commitsNode = payload.path("commits");
        if (commitsNode.isArray()) {
            for (JsonNode commitNode : commitsNode) {
                String sha = commitNode.path("id").asText();
                if (sha.isBlank()) {
                    continue;
                }
                String message = commitNode.path("message").asText("");
                String authorName = commitNode.path("author").path("name").asText("Unknown");
                String authorEmail = commitNode.path("author").path("email").asText("unknown@example.com");
                String commitUrl = commitNode.path("url").asText("");
                Instant timestamp = parseInstant(commitNode.path("timestamp").asText(null));

                DevelopmentCommit commit = commitRepository.findByRepositoryIdAndCommitSha(repo.getId(), sha)
                        .orElseGet(() -> commitRepository.save(new DevelopmentCommit(
                                repo.getId(), sha, message, authorName, authorEmail, commitUrl, timestamp
                        )));

                Set<ParsedIssueKey> commitKeys = IssueKeyParser.extractIssueKeys(message);
                linkCommitToMatchingIssues(commit, commitKeys);
            }
        }
    }

    /**
     * Ingests a GitHub pull_request event payload: updates PR details and auto-links issue keys.
     */
    @Transactional
    public void processPullRequestEvent(JsonNode payload) {
        ConnectedRepository repo = resolveRepository(payload);
        if (repo == null) {
            log.warn("Skipping pull_request processing: repository not recognized in NexTrack");
            return;
        }

        repo.setLastSyncedAt(Instant.now());
        connectedRepositoryRepository.save(repo);

        JsonNode prNode = payload.path("pull_request");
        if (prNode.isMissingNode() || prNode.isNull()) {
            return;
        }

        Long providerPrId = prNode.path("id").asLong();
        Integer prNumber = prNode.path("number").asInt();
        String title = prNode.path("title").asText("");
        String description = prNode.path("body").asText("");
        String sourceBranch = prNode.path("head").path("ref").asText("");
        String targetBranch = prNode.path("base").path("ref").asText("");
        String prUrl = prNode.path("html_url").asText("");
        String state = prNode.path("state").asText("open");
        boolean isDraft = prNode.path("draft").asBoolean(false);
        boolean merged = prNode.path("merged").asBoolean(false);
        Instant mergedAt = parseInstant(prNode.path("merged_at").asText(null));

        String status = merged ? "MERGED" : ("closed".equalsIgnoreCase(state) ? "CLOSED" : "OPEN");
        String reviewStatus = "PENDING";

        PullRequest pr = pullRequestRepository.findByRepositoryIdAndPrNumber(repo.getId(), prNumber)
                .map(existing -> {
                    existing.updateDetails(title, description, sourceBranch, targetBranch, status, reviewStatus, isDraft, mergedAt);
                    return pullRequestRepository.save(existing);
                })
                .orElseGet(() -> pullRequestRepository.save(new PullRequest(
                        repo.getId(), providerPrId, prNumber, title, description,
                        sourceBranch, targetBranch, prUrl, status, reviewStatus, isDraft, mergedAt
                )));

        Set<ParsedIssueKey> prKeys = new LinkedHashSet<>();
        prKeys.addAll(IssueKeyParser.extractIssueKeys(title));
        prKeys.addAll(IssueKeyParser.extractIssueKeys(description));
        prKeys.addAll(IssueKeyParser.extractIssueKeys(sourceBranch));

        linkPullRequestToMatchingIssues(pr, prKeys);
    }

    /**
     * Retrieves all development artifacts linked to an issue (branches, commits, and pull requests).
     */
    @Transactional(readOnly = true)
    public IssueDevelopmentResponse getIssueDevelopment(UUID issueId, UUID requestingUserId) {
        UUID projectId = issueService.getIssueProjectId(issueId, requestingUserId);
        if (!projectService.isUserProjectMember(projectId, requestingUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to project issues");
        }

        Map<UUID, ConnectedRepository> repoCache = new HashMap<>();

        List<LinkedBranchResponse> branches = issueBranchRepository.findAllByIdIssueId(issueId).stream()
                .map(link -> {
                    DevelopmentBranch b = branchRepository.findById(link.getBranchId()).orElse(null);
                    if (b == null) return null;
                    ConnectedRepository r = getCachedRepo(b.getRepositoryId(), repoCache);
                    String linkedByName = authService.getUserDisplayName(link.getLinkedBy());
                    return new LinkedBranchResponse(
                            b.getId(), b.getId(), b.getName(), b.getLastCommitSha(),
                            b.getRepositoryId(), r != null ? r.getFullName() : "Repository",
                            link.getLinkType(), link.getLinkedAt(), linkedByName
                    );
                })
                .filter(Objects::nonNull)
                .toList();

        List<LinkedCommitResponse> commits = issueCommitRepository.findAllByIdIssueId(issueId).stream()
                .map(link -> {
                    DevelopmentCommit c = commitRepository.findById(link.getCommitId()).orElse(null);
                    if (c == null) return null;
                    ConnectedRepository r = getCachedRepo(c.getRepositoryId(), repoCache);
                    String linkedByName = authService.getUserDisplayName(link.getLinkedBy());
                    return new LinkedCommitResponse(
                            c.getId(), c.getId(), c.getCommitSha(), c.getMessage(),
                            c.getAuthorName(), c.getAuthorEmail(), c.getCommitUrl(), c.getCommittedAt(),
                            c.getRepositoryId(), r != null ? r.getFullName() : "Repository",
                            link.getLinkType(), link.getLinkedAt(), linkedByName
                    );
                })
                .filter(Objects::nonNull)
                .toList();

        List<LinkedPullRequestResponse> pullRequests = issuePullRequestRepository.findAllByIdIssueId(issueId).stream()
                .map(link -> {
                    PullRequest pr = pullRequestRepository.findById(link.getPullRequestId()).orElse(null);
                    if (pr == null) return null;
                    ConnectedRepository r = getCachedRepo(pr.getRepositoryId(), repoCache);
                    String linkedByName = authService.getUserDisplayName(link.getLinkedBy());
                    return new LinkedPullRequestResponse(
                            pr.getId(), pr.getId(), pr.getProviderPrId(), pr.getPrNumber(),
                            pr.getTitle(), pr.getDescription(), pr.getSourceBranch(), pr.getTargetBranch(),
                            pr.getPrUrl(), pr.getStatus(), pr.getReviewStatus(), pr.isDraft(), pr.getMergedAt(),
                            pr.getRepositoryId(), r != null ? r.getFullName() : "Repository",
                            link.getLinkType(), link.getLinkedAt(), linkedByName
                    );
                })
                .filter(Objects::nonNull)
                .toList();

        int total = branches.size() + commits.size() + pullRequests.size();
        return new IssueDevelopmentResponse(issueId, branches, commits, pullRequests, total);
    }

    /**
     * Manually links a branch, commit, or pull request to an issue.
     */
    @Transactional
    public void linkArtifactToIssue(UUID issueId, ManualLinkRequest request, UUID requestingUserId) {
        UUID projectId = issueService.getIssueProjectId(issueId, requestingUserId);
        projectService.assertUserCanContribute(projectId, requestingUserId);

        String type = request.artifactType() != null ? request.artifactType().toUpperCase() : "";
        List<UUID> connectedRepoIds = getProjectConnectedRepoIds(projectId);

        switch (type) {
            case "BRANCH" -> {
                DevelopmentBranch branch = resolveBranch(request, connectedRepoIds);
                if (issueBranchRepository.existsByIdIssueIdAndIdBranchId(issueId, branch.getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Branch is already linked to this issue");
                }
                issueBranchRepository.save(new IssueDevelopmentBranch(issueId, branch.getId(), "MANUAL", requestingUserId));
            }
            case "COMMIT" -> {
                DevelopmentCommit commit = resolveCommit(request, connectedRepoIds);
                if (issueCommitRepository.existsByIdIssueIdAndIdCommitId(issueId, commit.getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Commit is already linked to this issue");
                }
                issueCommitRepository.save(new IssueDevelopmentCommit(issueId, commit.getId(), "MANUAL", requestingUserId));
            }
            case "PULL_REQUEST", "PR" -> {
                PullRequest pr = resolvePullRequest(request, connectedRepoIds);
                if (issuePullRequestRepository.existsByIdIssueIdAndIdPullRequestId(issueId, pr.getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Pull Request is already linked to this issue");
                }
                issuePullRequestRepository.save(new IssuePullRequest(issueId, pr.getId(), "MANUAL", requestingUserId));
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid artifactType: " + request.artifactType());
        }

        auditService.recordEvent(
                requestingUserId,
                "ISSUE",
                issueId,
                "DEVELOPMENT_ARTIFACT_LINKED",
                null,
                "{\"artifactType\":\"" + type + "\",\"issueId\":\"" + issueId + "\"}",
                null
        );
    }

    /**
     * Unlinks a branch, commit, or pull request from an issue without deleting the underlying record.
     */
    @Transactional
    public void unlinkArtifactFromIssue(UUID issueId, UUID targetId, UUID requestingUserId) {
        UUID projectId = issueService.getIssueProjectId(issueId, requestingUserId);
        projectService.assertUserCanContribute(projectId, requestingUserId);

        boolean removed = false;
        if (issueBranchRepository.existsByIdIssueIdAndIdBranchId(issueId, targetId)) {
            issueBranchRepository.deleteByIdIssueIdAndIdBranchId(issueId, targetId);
            removed = true;
        } else if (issueCommitRepository.existsByIdIssueIdAndIdCommitId(issueId, targetId)) {
            issueCommitRepository.deleteByIdIssueIdAndIdCommitId(issueId, targetId);
            removed = true;
        } else if (issuePullRequestRepository.existsByIdIssueIdAndIdPullRequestId(issueId, targetId)) {
            issuePullRequestRepository.deleteByIdIssueIdAndIdPullRequestId(issueId, targetId);
            removed = true;
        }

        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Development link not found for this issue");
        }

        auditService.recordEvent(
                requestingUserId,
                "ISSUE",
                issueId,
                "DEVELOPMENT_ARTIFACT_UNLINKED",
                null,
                "{\"targetId\":\"" + targetId + "\",\"issueId\":\"" + issueId + "\"}",
                null
        );
    }

    /**
     * Unlinks an artifact with explicit type name.
     */
    @Transactional
    public void unlinkArtifactFromIssue(UUID issueId, String linkType, UUID targetId, UUID requestingUserId) {
        UUID projectId = issueService.getIssueProjectId(issueId, requestingUserId);
        projectService.assertUserCanContribute(projectId, requestingUserId);

        String type = linkType.toUpperCase();
        switch (type) {
            case "BRANCH", "BRANCHES" -> {
                if (!issueBranchRepository.existsByIdIssueIdAndIdBranchId(issueId, targetId)) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch link not found");
                }
                issueBranchRepository.deleteByIdIssueIdAndIdBranchId(issueId, targetId);
            }
            case "COMMIT", "COMMITS" -> {
                if (!issueCommitRepository.existsByIdIssueIdAndIdCommitId(issueId, targetId)) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Commit link not found");
                }
                issueCommitRepository.deleteByIdIssueIdAndIdCommitId(issueId, targetId);
            }
            case "PULL_REQUEST", "PULL_REQUESTS", "PRS" -> {
                if (!issuePullRequestRepository.existsByIdIssueIdAndIdPullRequestId(issueId, targetId)) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pull request link not found");
                }
                issuePullRequestRepository.deleteByIdIssueIdAndIdPullRequestId(issueId, targetId);
            }
            default -> unlinkArtifactFromIssue(issueId, targetId, requestingUserId);
        }

        auditService.recordEvent(
                requestingUserId,
                "ISSUE",
                issueId,
                "DEVELOPMENT_ARTIFACT_UNLINKED",
                null,
                "{\"targetId\":\"" + targetId + "\",\"artifactType\":\"" + type + "\",\"issueId\":\"" + issueId + "\"}",
                null
        );
    }

    /**
     * Retrieves high-level development telemetry for an entire project.
     */
    @Transactional(readOnly = true)
    public ProjectDevelopmentOverviewResponse getProjectDevelopmentOverview(UUID projectId, UUID requestingUserId) {
        if (!projectService.isUserProjectMember(projectId, requestingUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to project");
        }

        List<UUID> repoIds = getProjectConnectedRepoIds(projectId);
        if (repoIds.isEmpty()) {
            return new ProjectDevelopmentOverviewResponse(
                    List.of(), List.of(), List.of(), List.of(),
                    new DevelopmentSummary(0, 0, 0, 0, 0, 0)
            );
        }

        List<ConnectedRepository> repos = connectedRepositoryRepository.findAllById(repoIds);
        Map<UUID, String> repoNames = repos.stream()
                .collect(Collectors.toMap(ConnectedRepository::getId, ConnectedRepository::getFullName));

        List<ConnectedRepositoryResponse> repoResponses = repos.stream()
                .map(r -> new ConnectedRepositoryResponse(
                        r.getId(), r.getInstallationId(), r.getProvider(), r.getExternalRepoId(),
                        r.getName(), r.getFullName(), r.getHtmlUrl(), r.getDefaultBranch(),
                        r.isActive(), r.getLastSyncedAt()
                ))
                .toList();

        List<LinkedCommitResponse> recentCommits = commitRepository
                .findAllByRepositoryIdInOrderByCommittedAtDesc(repoIds, PageRequest.of(0, 20))
                .stream()
                .map(c -> new LinkedCommitResponse(
                        c.getId(), c.getId(), c.getCommitSha(), c.getMessage(),
                        c.getAuthorName(), c.getAuthorEmail(), c.getCommitUrl(), c.getCommittedAt(),
                        c.getRepositoryId(), repoNames.getOrDefault(c.getRepositoryId(), "Repository"),
                        "CONNECTED", c.getCreatedAt(), null
                ))
                .toList();

        List<LinkedPullRequestResponse> recentPRs = pullRequestRepository
                .findAllByRepositoryIdInOrderByCreatedAtDesc(repoIds, PageRequest.of(0, 20))
                .stream()
                .map(pr -> new LinkedPullRequestResponse(
                        pr.getId(), pr.getId(), pr.getProviderPrId(), pr.getPrNumber(),
                        pr.getTitle(), pr.getDescription(), pr.getSourceBranch(), pr.getTargetBranch(),
                        pr.getPrUrl(), pr.getStatus(), pr.getReviewStatus(), pr.isDraft(), pr.getMergedAt(),
                        pr.getRepositoryId(), repoNames.getOrDefault(pr.getRepositoryId(), "Repository"),
                        "CONNECTED", pr.getCreatedAt(), null
                ))
                .toList();

        List<LinkedBranchResponse> recentBranches = branchRepository
                .findAllByRepositoryIdIn(repoIds)
                .stream()
                .limit(20)
                .map(b -> new LinkedBranchResponse(
                        b.getId(), b.getId(), b.getName(), b.getLastCommitSha(),
                        b.getRepositoryId(), repoNames.getOrDefault(b.getRepositoryId(), "Repository"),
                        "CONNECTED", b.getCreatedAt(), null
                ))
                .toList();

        long openPRs = pullRequestRepository.countByRepositoryIdInAndStatus(repoIds, "OPEN");
        long mergedPRs = pullRequestRepository.countByRepositoryIdInAndStatus(repoIds, "MERGED");

        DevelopmentSummary summary = new DevelopmentSummary(
                repos.size(),
                recentBranches.size(),
                recentCommits.size(),
                recentPRs.size(),
                (int) openPRs,
                (int) mergedPRs
        );

        return new ProjectDevelopmentOverviewResponse(repoResponses, recentCommits, recentPRs, recentBranches, summary);
    }

    /**
     * Searches unlinked candidates in connected repositories for the manual linking modal.
     */
    @Transactional(readOnly = true)
    public List<DevelopmentCandidateResponse> searchCandidates(
            UUID issueId,
            String artifactType,
            String query,
            UUID requestingUserId
    ) {
        UUID projectId = issueService.getIssueProjectId(issueId, requestingUserId);
        if (!projectService.isUserProjectMember(projectId, requestingUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to project");
        }

        List<UUID> repoIds = getProjectConnectedRepoIds(projectId);
        if (repoIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, String> repoNames = connectedRepositoryRepository.findAllById(repoIds).stream()
                .collect(Collectors.toMap(ConnectedRepository::getId, ConnectedRepository::getFullName));

        String q = query != null ? query.trim() : "";
        String type = artifactType != null ? artifactType.toUpperCase() : "COMMIT";

        return switch (type) {
            case "COMMIT" -> {
                Set<UUID> linkedCommitIds = issueCommitRepository.findAllByIdIssueId(issueId).stream()
                        .map(IssueDevelopmentCommit::getCommitId)
                        .collect(Collectors.toSet());

                yield commitRepository.findAllByRepositoryIdInOrderByCommittedAtDesc(repoIds, PageRequest.of(0, 50))
                        .stream()
                        .filter(c -> !linkedCommitIds.contains(c.getId()))
                        .filter(c -> q.isEmpty() || c.getCommitSha().contains(q) || c.getMessage().toLowerCase().contains(q.toLowerCase()))
                        .map(c -> new DevelopmentCandidateResponse(
                                c.getId(), "COMMIT", c.getCommitSha().substring(0, Math.min(8, c.getCommitSha().length())),
                                c.getMessage(), c.getAuthorName(), c.getCommitUrl(), c.getCommittedAt(),
                                c.getRepositoryId(), repoNames.getOrDefault(c.getRepositoryId(), "Repository")
                        ))
                        .limit(20)
                        .toList();
            }
            case "PULL_REQUEST", "PR" -> {
                Set<UUID> linkedPrIds = issuePullRequestRepository.findAllByIdIssueId(issueId).stream()
                        .map(IssuePullRequest::getPullRequestId)
                        .collect(Collectors.toSet());

                yield pullRequestRepository.findAllByRepositoryIdInOrderByCreatedAtDesc(repoIds, PageRequest.of(0, 50))
                        .stream()
                        .filter(pr -> !linkedPrIds.contains(pr.getId()))
                        .filter(pr -> q.isEmpty() || String.valueOf(pr.getPrNumber()).contains(q) || pr.getTitle().toLowerCase().contains(q.toLowerCase()))
                        .map(pr -> new DevelopmentCandidateResponse(
                                pr.getId(), "PULL_REQUEST", "#" + pr.getPrNumber(),
                                pr.getTitle(), pr.getStatus() + " • " + pr.getSourceBranch(), pr.getPrUrl(), pr.getCreatedAt(),
                                pr.getRepositoryId(), repoNames.getOrDefault(pr.getRepositoryId(), "Repository")
                        ))
                        .limit(20)
                        .toList();
            }
            case "BRANCH" -> {
                Set<UUID> linkedBranchIds = issueBranchRepository.findAllByIdIssueId(issueId).stream()
                        .map(IssueDevelopmentBranch::getBranchId)
                        .collect(Collectors.toSet());

                yield branchRepository.findAllByRepositoryIdIn(repoIds)
                        .stream()
                        .filter(b -> !linkedBranchIds.contains(b.getId()))
                        .filter(b -> q.isEmpty() || b.getName().toLowerCase().contains(q.toLowerCase()))
                        .map(b -> new DevelopmentCandidateResponse(
                                b.getId(), "BRANCH", b.getName(),
                                b.getName(), b.getLastCommitSha() != null ? b.getLastCommitSha().substring(0, Math.min(8, b.getLastCommitSha().length())) : "Head",
                                "", b.getCreatedAt(),
                                b.getRepositoryId(), repoNames.getOrDefault(b.getRepositoryId(), "Repository")
                        ))
                        .limit(20)
                        .toList();
            }
            default -> List.of();
        };
    }

    // -------------------------------------------------------------------------
    // Private Helpers
    // -------------------------------------------------------------------------

    private void linkBranchToMatchingIssues(DevelopmentBranch branch, Set<ParsedIssueKey> keys) {
        if (keys.isEmpty()) return;
        List<UUID> projectIds = getRepoConnectedProjectIds(branch.getRepositoryId());

        for (UUID projectId : projectIds) {
            String projectKey = projectService.getProjectKey(projectId);
            if (projectKey == null) continue;

            for (ParsedIssueKey key : keys) {
                if (projectKey.equalsIgnoreCase(key.projectKey())) {
                    issueService.findIssueIdByProjectAndNumber(projectId, key.issueNumber())
                            .ifPresent(matchedIssueId -> {
                                if (!issueBranchRepository.existsByIdIssueIdAndIdBranchId(matchedIssueId, branch.getId())) {
                                    issueBranchRepository.save(new IssueDevelopmentBranch(matchedIssueId, branch.getId(), "AUTO_EXTRACTED", null));
                                    log.info("Auto-linked branch '{}' to issue '{}'", branch.getName(), key.formattedKey());
                                }
                            });
                }
            }
        }
    }

    private void linkCommitToMatchingIssues(DevelopmentCommit commit, Set<ParsedIssueKey> keys) {
        if (keys.isEmpty()) return;
        List<UUID> projectIds = getRepoConnectedProjectIds(commit.getRepositoryId());

        for (UUID projectId : projectIds) {
            String projectKey = projectService.getProjectKey(projectId);
            if (projectKey == null) continue;

            for (ParsedIssueKey key : keys) {
                if (projectKey.equalsIgnoreCase(key.projectKey())) {
                    issueService.findIssueIdByProjectAndNumber(projectId, key.issueNumber())
                            .ifPresent(matchedIssueId -> {
                                if (!issueCommitRepository.existsByIdIssueIdAndIdCommitId(matchedIssueId, commit.getId())) {
                                    issueCommitRepository.save(new IssueDevelopmentCommit(matchedIssueId, commit.getId(), "AUTO_EXTRACTED", null));
                                    log.info("Auto-linked commit '{}' to issue '{}'", commit.getCommitSha(), key.formattedKey());
                                }
                            });
                }
            }
        }
    }

    private void linkPullRequestToMatchingIssues(PullRequest pr, Set<ParsedIssueKey> keys) {
        if (keys.isEmpty()) return;
        List<UUID> projectIds = getRepoConnectedProjectIds(pr.getRepositoryId());

        for (UUID projectId : projectIds) {
            String projectKey = projectService.getProjectKey(projectId);
            if (projectKey == null) continue;

            for (ParsedIssueKey key : keys) {
                if (projectKey.equalsIgnoreCase(key.projectKey())) {
                    issueService.findIssueIdByProjectAndNumber(projectId, key.issueNumber())
                            .ifPresent(matchedIssueId -> {
                                if (!issuePullRequestRepository.existsByIdIssueIdAndIdPullRequestId(matchedIssueId, pr.getId())) {
                                    issuePullRequestRepository.save(new IssuePullRequest(matchedIssueId, pr.getId(), "AUTO_EXTRACTED", null));
                                    log.info("Auto-linked PR '#{}' to issue '{}'", pr.getPrNumber(), key.formattedKey());
                                }
                            });
                }
            }
        }
    }

    private ConnectedRepository resolveRepository(JsonNode payload) {
        JsonNode repoNode = payload.path("repository");
        if (repoNode.isMissingNode() || repoNode.isNull()) {
            return null;
        }

        String externalId = repoNode.path("id").asText(null);
        String fullName = repoNode.path("full_name").asText(null);

        if (externalId != null && !externalId.isBlank()) {
            Optional<ConnectedRepository> opt = connectedRepositoryRepository.findFirstByExternalRepoId(externalId);
            if (opt.isPresent()) return opt.get();
        }

        if (fullName != null && !fullName.isBlank()) {
            return connectedRepositoryRepository.findFirstByFullName(fullName).orElse(null);
        }

        return null;
    }

    private List<UUID> getProjectConnectedRepoIds(UUID projectId) {
        return projectConnectedRepositoryRepository.findAllByProjectId(projectId).stream()
                .map(ProjectConnectedRepository::getRepositoryId)
                .toList();
    }

    private List<UUID> getRepoConnectedProjectIds(UUID repoId) {
        return projectConnectedRepositoryRepository.findAllByRepositoryId(repoId).stream()
                .map(ProjectConnectedRepository::getProjectId)
                .toList();
    }

    private DevelopmentBranch resolveBranch(ManualLinkRequest request, List<UUID> connectedRepoIds) {
        if (request.artifactId() != null) {
            return branchRepository.findById(request.artifactId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
        }
        if (request.identifier() != null && !request.identifier().isBlank()) {
            return branchRepository.findAllByRepositoryIdIn(connectedRepoIds).stream()
                    .filter(b -> b.getName().equalsIgnoreCase(request.identifier().trim()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found matching '" + request.identifier() + "'"));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Must provide artifactId or branch name identifier");
    }

    private DevelopmentCommit resolveCommit(ManualLinkRequest request, List<UUID> connectedRepoIds) {
        if (request.artifactId() != null) {
            return commitRepository.findById(request.artifactId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commit not found"));
        }
        if (request.identifier() != null && !request.identifier().isBlank()) {
            String sha = request.identifier().trim().toLowerCase();
            return commitRepository.findAllByRepositoryIdInOrderByCommittedAtDesc(connectedRepoIds, PageRequest.of(0, 100)).stream()
                    .filter(c -> c.getCommitSha().toLowerCase().startsWith(sha))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commit not found matching SHA '" + request.identifier() + "'"));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Must provide artifactId or commitSha identifier");
    }

    private PullRequest resolvePullRequest(ManualLinkRequest request, List<UUID> connectedRepoIds) {
        if (request.artifactId() != null) {
            return pullRequestRepository.findById(request.artifactId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pull request not found"));
        }
        if (request.identifier() != null && !request.identifier().isBlank()) {
            String idStr = request.identifier().trim().replace("#", "");
            try {
                int prNum = Integer.parseInt(idStr);
                return pullRequestRepository.findAllByRepositoryIdInOrderByCreatedAtDesc(connectedRepoIds, PageRequest.of(0, 100)).stream()
                        .filter(pr -> pr.getPrNumber() == prNum)
                        .findFirst()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pull request #" + prNum + " not found"));
            } catch (NumberFormatException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid PR number: " + request.identifier());
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Must provide artifactId or prNumber identifier");
    }

    private ConnectedRepository getCachedRepo(UUID repoId, Map<UUID, ConnectedRepository> cache) {
        if (repoId == null) return null;
        return cache.computeIfAbsent(repoId, id -> connectedRepositoryRepository.findById(id).orElse(null));
    }

    private Instant parseInstant(String text) {
        if (text == null || text.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(text);
        } catch (Exception ex) {
            return Instant.now();
        }
    }
}
