package com.nextrack.development;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextrack.development.api.dto.DevelopmentDtos.*;
import com.nextrack.development.domain.repository.DevelopmentBranchRepository;
import com.nextrack.development.domain.repository.DevelopmentCommitRepository;
import com.nextrack.development.domain.repository.PullRequestRepository;
import com.nextrack.identity.api.dto.request.RegisterRequest;
import com.nextrack.identity.api.dto.response.AuthResponse;
import com.nextrack.issue.api.dto.request.CreateIssueRequest;
import com.nextrack.issue.api.dto.response.IssueResponse;
import com.nextrack.issue.domain.entity.IssuePriority;
import com.nextrack.issue.domain.entity.IssueType;
import com.nextrack.project.api.dto.request.CreateProjectRequest;
import com.nextrack.project.api.dto.response.ProjectResponse;
import com.nextrack.workspace.api.dto.request.CreateWorkspaceRequest;
import com.nextrack.workspace.api.dto.response.WorkspaceResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class IssueTraceabilityIntegrationTests {

    private static final String TEST_SECRET = "traceability_integration_secret_123";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("nextrack_traceability_test")
            .withUsername("trace_user")
            .withPassword("trace_pass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "25");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("nextrack.integrations.github.webhook.secret", () -> TEST_SECRET);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DevelopmentCommitRepository commitRepository;

    @Autowired
    private DevelopmentBranchRepository branchRepository;

    @Autowired
    private PullRequestRepository pullRequestRepository;

    private String computeHmac(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("Should auto-link commits, branches, and PRs from GitHub telemetry to issues")
    void shouldAutoLinkGitTelemetryToIssues() throws Exception {
        // 1. Setup User, Workspace, Project
        AuthResponse auth = registerUser("trace.dev@example.com", "Dev Lead", "Password123!");
        WorkspaceResponse workspace = createWorkspace(auth.accessToken(), "Engineering Corp", "eng-corp");
        ProjectResponse project = createProject(auth.accessToken(), workspace.id(), "Core Platform", "TRACE");

        // 2. Create Issue TRACE-1 and TRACE-2
        IssueResponse issue1 = createIssue(auth.accessToken(), project.id(), "Implement User Auth", "Details here");
        IssueResponse issue2 = createIssue(auth.accessToken(), project.id(), "Optimize DB Indexes", "Details here");
        assertThat(issue1.issueNumber()).isEqualTo(1L);
        assertThat(issue2.issueNumber()).isEqualTo(2L);

        // 3. Connect Repository to Workspace and Link to Project
        InstallationResponse install = registerInstallation(auth.accessToken(), workspace.id(), 998877L, "eng-corp-org");
        ConnectedRepositoryResponse repo = connectRepo(auth.accessToken(), install.id(), "repo-12345", "core-repo", "eng-corp-org/core-repo");
        linkRepoToProject(auth.accessToken(), project.id(), repo.id());

        // 4. Send GitHub Push Webhook with branch "feature/TRACE-1-login" and commits referencing TRACE-1 and TRACE-2
        String commitSha1 = "a1b2c3d4e5f60000000000000000000000000001";
        String commitSha2 = "a1b2c3d4e5f60000000000000000000000000002";
        String pushPayload = """
            {
                "ref": "refs/heads/feature/TRACE-1-login",
                "after": "%s",
                "repository": {
                    "id": "repo-12345",
                    "full_name": "eng-corp-org/core-repo"
                },
                "commits": [
                    {
                        "id": "%s",
                        "message": "TRACE-1: Initial authentication controller scaffolding",
                        "timestamp": "2026-11-19T20:00:00Z",
                        "url": "https://github.com/eng-corp-org/core-repo/commit/%s",
                        "author": { "name": "Alice Dev", "email": "alice@example.com" }
                    },
                    {
                        "id": "%s",
                        "message": "TRACE-1 and TRACE-2: Fix session handling and DB index",
                        "timestamp": "2026-11-19T20:30:00Z",
                        "url": "https://github.com/eng-corp-org/core-repo/commit/%s",
                        "author": { "name": "Bob Dev", "email": "bob@example.com" }
                    }
                ]
            }
            """.formatted(commitSha1, commitSha1, commitSha1, commitSha2, commitSha2);

        mockMvc.perform(post("/api/v1/integrations/github/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-GitHub-Delivery", UUID.randomUUID().toString())
                        .header("X-GitHub-Event", "push")
                        .header("X-Hub-Signature-256", computeHmac(pushPayload))
                        .content(pushPayload))
                .andExpect(status().isAccepted());

        // Process asynchronous webhook inbox
        runInboxWorker();

        // 5. Verify TRACE-1 development artifacts
        MvcResult devResult1 = mockMvc.perform(get("/api/v1/issues/{id}/development", issue1.id())
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andReturn();

        IssueDevelopmentResponse devResponse1 = objectMapper.readValue(
                devResult1.getResponse().getContentAsString(),
                IssueDevelopmentResponse.class
        );

        assertThat(devResponse1.branches()).hasSize(1);
        assertThat(devResponse1.branches().get(0).name()).isEqualTo("feature/TRACE-1-login");
        assertThat(devResponse1.branches().get(0).linkType()).isEqualTo("AUTO_EXTRACTED");

        assertThat(devResponse1.commits()).hasSize(2);
        assertThat(devResponse1.commits()).extracting(LinkedCommitResponse::commitSha)
                .containsExactlyInAnyOrder(commitSha1, commitSha2);

        // 6. Verify TRACE-2 has commit2 auto-linked as M:N association
        MvcResult devResult2 = mockMvc.perform(get("/api/v1/issues/{id}/development", issue2.id())
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andReturn();

        IssueDevelopmentResponse devResponse2 = objectMapper.readValue(
                devResult2.getResponse().getContentAsString(),
                IssueDevelopmentResponse.class
        );

        assertThat(devResponse2.commits()).hasSize(1);
        assertThat(devResponse2.commits().get(0).commitSha()).isEqualTo(commitSha2);

        // 7. Send GitHub Pull Request Webhook
        String prPayload = """
            {
                "action": "opened",
                "repository": {
                    "id": "repo-12345",
                    "full_name": "eng-corp-org/core-repo"
                },
                "pull_request": {
                    "id": 887766,
                    "number": 10,
                    "title": "[TRACE-1] Feature: User Authentication & SSO",
                    "body": "Resolves TRACE-1. Closes PR testing.",
                    "html_url": "https://github.com/eng-corp-org/core-repo/pull/10",
                    "state": "open",
                    "draft": false,
                    "merged": false,
                    "head": { "ref": "feature/TRACE-1-login" },
                    "base": { "ref": "main" }
                }
            }
            """;

        mockMvc.perform(post("/api/v1/integrations/github/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-GitHub-Delivery", UUID.randomUUID().toString())
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-Hub-Signature-256", computeHmac(prPayload))
                        .content(prPayload))
                .andExpect(status().isAccepted());

        runInboxWorker();

        // Verify PR is linked to TRACE-1
        devResult1 = mockMvc.perform(get("/api/v1/issues/{id}/development", issue1.id())
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andReturn();

        devResponse1 = objectMapper.readValue(devResult1.getResponse().getContentAsString(), IssueDevelopmentResponse.class);
        assertThat(devResponse1.pullRequests()).hasSize(1);
        assertThat(devResponse1.pullRequests().get(0).prNumber()).isEqualTo(10);
        assertThat(devResponse1.pullRequests().get(0).linkType()).isEqualTo("AUTO_EXTRACTED");
    }

    @Test
    @DisplayName("Should handle manual linking and unlinking with 409 conflict detection")
    void shouldHandleManualLinkingAndUnlinking() throws Exception {
        // 1. Setup
        AuthResponse auth = registerUser("manual.dev@example.com", "Manual Dev", "Password123!");
        WorkspaceResponse workspace = createWorkspace(auth.accessToken(), "Manual Corp", "manual-corp");
        ProjectResponse project = createProject(auth.accessToken(), workspace.id(), "Manual Project", "MAN");
        IssueResponse issue = createIssue(auth.accessToken(), project.id(), "Support Dark Mode", "Desc");

        InstallationResponse install = registerInstallation(auth.accessToken(), workspace.id(), 112233L, "manual-corp-org");
        ConnectedRepositoryResponse repo = connectRepo(auth.accessToken(), install.id(), "repo-manual-1", "frontend-repo", "manual-corp-org/frontend-repo");
        linkRepoToProject(auth.accessToken(), project.id(), repo.id());

        // 2. Ingest commit without formatted key (unformatted commit)
        String unformattedSha = "c0ffee0000000000000000000000000000000001";
        String pushPayload = """
            {
                "ref": "refs/heads/main",
                "after": "%s",
                "repository": {
                    "id": "repo-manual-1",
                    "full_name": "manual-corp-org/frontend-repo"
                },
                "commits": [
                    {
                        "id": "%s",
                        "message": "Miscellaneous styling updates for dark mode toggle",
                        "timestamp": "2026-11-19T21:00:00Z",
                        "url": "https://github.com/manual-corp-org/frontend-repo/commit/%s",
                        "author": { "name": "Charlie", "email": "charlie@example.com" }
                    }
                ]
            }
            """.formatted(unformattedSha, unformattedSha, unformattedSha);

        mockMvc.perform(post("/api/v1/integrations/github/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-GitHub-Delivery", UUID.randomUUID().toString())
                        .header("X-GitHub-Event", "push")
                        .header("X-Hub-Signature-256", computeHmac(pushPayload))
                        .content(pushPayload))
                .andExpect(status().isAccepted());

        runInboxWorker();

        // Assert zero auto-links for unformatted commit
        MvcResult initialDevResult = mockMvc.perform(get("/api/v1/issues/{id}/development", issue.id())
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andReturn();
        IssueDevelopmentResponse initialDev = objectMapper.readValue(initialDevResult.getResponse().getContentAsString(), IssueDevelopmentResponse.class);
        assertThat(initialDev.commits()).isEmpty();

        // 3. Search candidate commits via search API
        mockMvc.perform(get("/api/v1/issues/{id}/development/candidates", issue.id())
                        .param("type", "COMMIT")
                        .param("search", "styling")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].identifier").value("c0ffee00"));

        // 4. Manually link commit by SHA
        ManualLinkRequest linkRequest = new ManualLinkRequest("COMMIT", null, unformattedSha, null);
        mockMvc.perform(post("/api/v1/issues/{id}/development/links", issue.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .content(objectMapper.writeValueAsString(linkRequest)))
                .andExpect(status().isCreated());

        // Verify commit is now linked with link_type = MANUAL
        MvcResult afterLinkResult = mockMvc.perform(get("/api/v1/issues/{id}/development", issue.id())
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andReturn();
        IssueDevelopmentResponse afterLink = objectMapper.readValue(afterLinkResult.getResponse().getContentAsString(), IssueDevelopmentResponse.class);
        assertThat(afterLink.commits()).hasSize(1);
        assertThat(afterLink.commits().get(0).linkType()).isEqualTo("MANUAL");
        assertThat(afterLink.commits().get(0).linkedByName()).isEqualTo("Manual Dev");

        UUID commitId = afterLink.commits().get(0).id();

        // 5. Linking already-linked commit returns 409 Conflict
        mockMvc.perform(post("/api/v1/issues/{id}/development/links", issue.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .content(objectMapper.writeValueAsString(linkRequest)))
                .andExpect(status().isConflict());

        // 6. Unlink commit
        mockMvc.perform(delete("/api/v1/issues/{id}/development/links/{targetId}", issue.id(), commitId)
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isNoContent());

        // Verify link is removed
        MvcResult finalDevResult = mockMvc.perform(get("/api/v1/issues/{id}/development", issue.id())
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andReturn();
        IssueDevelopmentResponse finalDev = objectMapper.readValue(finalDevResult.getResponse().getContentAsString(), IssueDevelopmentResponse.class);
        assertThat(finalDev.commits()).isEmpty();

        // Underlying commit entity is preserved
        assertThat(commitRepository.findByRepositoryIdAndCommitSha(repo.id(), unformattedSha)).isPresent();
    }

    @Test
    @DisplayName("Should project accurate commit, branch, and PR counts in v_board_cards view")
    void shouldProjectAccurateCountsInBoardView() throws Exception {
        AuthResponse auth = registerUser("board.dev@example.com", "Board Dev", "Password123!");
        WorkspaceResponse workspace = createWorkspace(auth.accessToken(), "Board Corp", "board-corp");
        ProjectResponse project = createProject(auth.accessToken(), workspace.id(), "Kanban Project", "KB");
        IssueResponse issue = createIssue(auth.accessToken(), project.id(), "Interactive Dashboard", "Desc");

        InstallationResponse install = registerInstallation(auth.accessToken(), workspace.id(), 554433L, "board-org");
        ConnectedRepositoryResponse repo = connectRepo(auth.accessToken(), install.id(), "repo-kb", "kb-repo", "board-org/kb-repo");
        linkRepoToProject(auth.accessToken(), project.id(), repo.id());

        // Ingest branch and commit for KB-1
        String sha = "deadbeef00000000000000000000000000000001";
        String pushPayload = """
            {
                "ref": "refs/heads/feature/KB-1-charts",
                "after": "%s",
                "repository": {
                    "id": "repo-kb",
                    "full_name": "board-org/kb-repo"
                },
                "commits": [
                    {
                        "id": "%s",
                        "message": "KB-1: Add chart visualizations",
                        "timestamp": "2026-11-19T22:00:00Z",
                        "url": "https://github.com/board-org/kb-repo/commit/%s",
                        "author": { "name": "Dan", "email": "dan@example.com" }
                    }
                ]
            }
            """.formatted(sha, sha, sha);

        mockMvc.perform(post("/api/v1/integrations/github/webhooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-GitHub-Delivery", UUID.randomUUID().toString())
                        .header("X-GitHub-Event", "push")
                        .header("X-Hub-Signature-256", computeHmac(pushPayload))
                        .content(pushPayload))
                .andExpect(status().isAccepted());

        runInboxWorker();

        // Query Kanban board view and assert commit_count == 1 and branch_count == 1
        mockMvc.perform(get("/api/v1/projects/{projectId}/board", project.id())
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns.BACKLOG[0].commitCount").value(1))
                .andExpect(jsonPath("$.columns.BACKLOG[0].branchCount").value(1));
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    @Autowired
    private com.nextrack.development.application.service.WebhookInboxWorker webhookInboxWorker;

    private void runInboxWorker() {
        int processed;
        do {
            processed = webhookInboxWorker.processBatchOnce();
        } while (processed > 0);
    }

    private AuthResponse registerUser(String email, String name, String password) throws Exception {
        RegisterRequest req = new RegisterRequest(email, name, password);
        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(res.getResponse().getContentAsString(), AuthResponse.class);
    }

    private WorkspaceResponse createWorkspace(String token, String name, String slug) throws Exception {
        CreateWorkspaceRequest req = new CreateWorkspaceRequest(name, slug);
        MvcResult res = mockMvc.perform(post("/api/v1/workspaces")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(res.getResponse().getContentAsString(), WorkspaceResponse.class);
    }

    private ProjectResponse createProject(String token, UUID workspaceId, String name, String key) throws Exception {
        CreateProjectRequest req = new CreateProjectRequest(name, key, "Test Description");
        MvcResult res = mockMvc.perform(post("/api/v1/workspaces/{id}/projects", workspaceId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(res.getResponse().getContentAsString(), ProjectResponse.class);
    }

    private IssueResponse createIssue(String token, UUID projectId, String title, String description) throws Exception {
        CreateIssueRequest req = new CreateIssueRequest(
                title, description, IssueType.TASK, IssuePriority.HIGH, null, null, null
        );
        MvcResult res = mockMvc.perform(post("/api/v1/projects/{projectId}/issues", projectId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(res.getResponse().getContentAsString(), IssueResponse.class);
    }

    private InstallationResponse registerInstallation(String token, UUID workspaceId, Long installId, String account) throws Exception {
        CreateInstallationRequest req = new CreateInstallationRequest(installId, account, "ALL", "{}");
        MvcResult res = mockMvc.perform(post("/api/v1/workspaces/{id}/integrations/github", workspaceId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(res.getResponse().getContentAsString(), InstallationResponse.class);
    }

    private ConnectedRepositoryResponse connectRepo(String token, UUID installId, String extId, String name, String fullName) throws Exception {
        ConnectRepositoryRequest req = new ConnectRepositoryRequest(
                extId, name, fullName, "https://github.com/" + fullName, "main"
        );
        MvcResult res = mockMvc.perform(post("/api/v1/integrations/github/installations/{id}/repositories", installId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(res.getResponse().getContentAsString(), ConnectedRepositoryResponse.class);
    }

    private void linkRepoToProject(String token, UUID projectId, UUID repoId) throws Exception {
        ProjectRepositoryLinkRequest req = new ProjectRepositoryLinkRequest(repoId);
        mockMvc.perform(post("/api/v1/projects/{id}/repositories/link", projectId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());
    }
}
