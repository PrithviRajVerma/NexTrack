package com.nextrack.issue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextrack.identity.api.dto.request.RegisterRequest;
import com.nextrack.identity.api.dto.response.AuthResponse;
import com.nextrack.issue.api.dto.request.CreateIssueRequest;
import com.nextrack.issue.api.dto.request.CreateCommentRequest;
import com.nextrack.issue.api.dto.request.TransitionIssueRequest;
import com.nextrack.issue.api.dto.response.CommentResponse;
import com.nextrack.issue.api.dto.response.IssueResponse;
import com.nextrack.issue.domain.entity.IssuePriority;
import com.nextrack.issue.domain.entity.IssueStatus;
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
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class WorkflowAndTransitionIntegrationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("nextrack_workflow_test")
            .withUsername("workflow_user")
            .withPassword("workflow_pass");

    @DynamicPropertySource
    static void configureProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "25");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should traverse valid issue workflow lifecycle and record atomic audit events")
    void shouldTraverseValidWorkflowLifecycle() throws Exception {
        AuthResponse auth = registerUser("alice.workflow@example.com", "Alice Workflow", "SecretPass123!");
        String token = "Bearer " + auth.accessToken();
        WorkspaceResponse ws = createWorkspace(token, "Workflow Org", "wf-org");
        ProjectResponse proj = createProject(token, ws.id(), "Workflow Project", "WF");

        // 1. Create issue (initial status BACKLOG, version 0)
        CreateIssueRequest createReq = new CreateIssueRequest(
                "Build FSM Engine",
                "Implement finite state machine",
                IssueType.TASK,
                IssuePriority.HIGH,
                null,
                null,
                3
        );
        MvcResult createResult = mockMvc.perform(post("/api/v1/projects/" + proj.id() + "/issues")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BACKLOG"))
                .andExpect(jsonPath("$.version").value(0))
                .andReturn();

        IssueResponse issue = objectMapper.readValue(createResult.getResponse().getContentAsString(), IssueResponse.class);
        UUID issueId = issue.id();

        // 2. BACKLOG -> TO_DO (version 0 -> 1)
        issue = executeTransition(token, issueId, IssueStatus.TO_DO, 0L);
        assertThat(issue.status()).isEqualTo(IssueStatus.TO_DO);
        assertThat(issue.version()).isEqualTo(1L);

        // 3. TO_DO -> IN_PROGRESS (version 1 -> 2)
        issue = executeTransition(token, issueId, IssueStatus.IN_PROGRESS, 1L);
        assertThat(issue.status()).isEqualTo(IssueStatus.IN_PROGRESS);
        assertThat(issue.version()).isEqualTo(2L);

        // 4. IN_PROGRESS -> IN_REVIEW (version 2 -> 3)
        issue = executeTransition(token, issueId, IssueStatus.IN_REVIEW, 2L);
        assertThat(issue.status()).isEqualTo(IssueStatus.IN_REVIEW);
        assertThat(issue.version()).isEqualTo(3L);

        // 5. IN_REVIEW -> DONE (version 3 -> 4)
        issue = executeTransition(token, issueId, IssueStatus.DONE, 3L);
        assertThat(issue.status()).isEqualTo(IssueStatus.DONE);
        assertThat(issue.version()).isEqualTo(4L);

        // 6. Defect Reopen: DONE -> IN_PROGRESS (version 4 -> 5)
        issue = executeTransition(token, issueId, IssueStatus.IN_PROGRESS, 4L);
        assertThat(issue.status()).isEqualTo(IssueStatus.IN_PROGRESS);
        assertThat(issue.version()).isEqualTo(5L);

        // 7. IN_PROGRESS -> IN_REVIEW (version 5 -> 6)
        issue = executeTransition(token, issueId, IssueStatus.IN_REVIEW, 5L);
        assertThat(issue.status()).isEqualTo(IssueStatus.IN_REVIEW);
        assertThat(issue.version()).isEqualTo(6L);

        // 8. IN_REVIEW -> DONE (version 6 -> 7)
        issue = executeTransition(token, issueId, IssueStatus.DONE, 6L);
        assertThat(issue.status()).isEqualTo(IssueStatus.DONE);
        assertThat(issue.version()).isEqualTo(7L);

        // 9. DONE -> ARCHIVED (version 7 -> 8)
        issue = executeTransition(token, issueId, IssueStatus.ARCHIVED, 7L);
        assertThat(issue.status()).isEqualTo(IssueStatus.ARCHIVED);
        assertThat(issue.version()).isEqualTo(8L);

        // 10. Verify audit history records all transitions
        MvcResult historyResult = mockMvc.perform(get("/api/v1/issues/" + issueId + "/history")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode historyArray = objectMapper.readTree(historyResult.getResponse().getContentAsString());
        assertThat(historyArray.isArray()).isTrue();
        // 1 creation event + 8 transition events = 9 total
        assertThat(historyArray.size()).isEqualTo(9);

        // Top event should be the latest transition (DONE -> ARCHIVED)
        JsonNode latestEvent = historyArray.get(0);
        assertThat(latestEvent.get("action").asText()).isEqualTo("STATUS_TRANSITION");
        assertThat(latestEvent.get("entityType").asText()).isEqualTo("ISSUE");
        assertThat(latestEvent.get("entityId").asText()).isEqualTo(issueId.toString());
        assertThat(latestEvent.get("actorId").asText()).isEqualTo(auth.user().id().toString());
    }

    @Test
    @DisplayName("Should reject disallowed status transitions with 400 Bad Request and WORKFLOW_TRANSITION_DENIED")
    void shouldRejectDisallowedTransitions() throws Exception {
        AuthResponse auth = registerUser("bob.fsm@example.com", "Bob FSM", "SecretPass123!");
        String token = "Bearer " + auth.accessToken();
        WorkspaceResponse ws = createWorkspace(token, "Bob Org", "bob-org");
        ProjectResponse proj = createProject(token, ws.id(), "FSM Guard Project", "FSM");

        CreateIssueRequest createReq = new CreateIssueRequest(
                "Direct Jump Test",
                "Try jumping from BACKLOG directly to DONE",
                IssueType.TASK,
                IssuePriority.MEDIUM,
                null,
                null,
                1
        );
        MvcResult createResult = mockMvc.perform(post("/api/v1/projects/" + proj.id() + "/issues")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        IssueResponse issue = objectMapper.readValue(createResult.getResponse().getContentAsString(), IssueResponse.class);

        // Disallowed: BACKLOG -> DONE
        TransitionIssueRequest badTransition = new TransitionIssueRequest(IssueStatus.DONE, 0L);

        mockMvc.perform(post("/api/v1/issues/" + issue.id() + "/transitions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badTransition)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WORKFLOW_TRANSITION_DENIED"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Transition from BACKLOG to DONE is not permitted by the workflow state machine"));

        // Verify status remains BACKLOG and version remains 0
        mockMvc.perform(get("/api/v1/issues/" + issue.id())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BACKLOG"))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    @DisplayName("Should detect optimistic locking conflicts under concurrent transition requests")
    void shouldDetectConcurrentOptimisticLockConflict() throws Exception {
        AuthResponse auth = registerUser("concurrency.lock@example.com", "Concurrency Lock", "SecretPass123!");
        String token = "Bearer " + auth.accessToken();
        WorkspaceResponse ws = createWorkspace(token, "Lock Org", "lock-org");
        ProjectResponse proj = createProject(token, ws.id(), "Lock Project", "LOCK");

        CreateIssueRequest createReq = new CreateIssueRequest(
                "Race Transition Issue",
                "Concurrent transitions on same version",
                IssueType.TASK,
                IssuePriority.HIGH,
                null,
                null,
                5
        );
        MvcResult createResult = mockMvc.perform(post("/api/v1/projects/" + proj.id() + "/issues")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        IssueResponse issue = objectMapper.readValue(createResult.getResponse().getContentAsString(), IssueResponse.class);
        UUID issueId = issue.id();

        // Two concurrent threads attempting to transition BACKLOG -> TO_DO using the same initial version 0
        final int THREADS = 2;
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch readyLatch = new CountDownLatch(THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<Integer>> futures = new ArrayList<>();
        List<String> responses = new CopyOnWriteArrayList<>();

        for (int i = 0; i < THREADS; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();

                TransitionIssueRequest req = new TransitionIssueRequest(IssueStatus.TO_DO, 0L);
                MvcResult res = mockMvc.perform(post("/api/v1/issues/" + issueId + "/transitions")
                                .header("Authorization", token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andReturn();

                responses.add(res.getResponse().getContentAsString());
                return res.getResponse().getStatus();
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        List<Integer> statusCodes = new ArrayList<>();
        for (Future<Integer> f : futures) {
            statusCodes.add(f.get(10, TimeUnit.SECONDS));
        }

        executor.shutdown();

        // Exactly one request must succeed (200 OK) and one must receive 409 Conflict
        assertThat(statusCodes).containsExactlyInAnyOrder(200, 409);

        // Verify the 409 Conflict response contains ISSUE_VERSION_CONFLICT
        String conflictResponse = responses.stream()
                .filter(r -> r.contains("ISSUE_VERSION_CONFLICT"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected ISSUE_VERSION_CONFLICT in one of: " + responses));

        JsonNode conflictJson = objectMapper.readTree(conflictResponse);
        assertThat(conflictJson.get("code").asText()).isEqualTo("ISSUE_VERSION_CONFLICT");
        assertThat(conflictJson.get("status").asInt()).isEqualTo(409);
    }

    @Test
    @DisplayName("Should reject stale expected version with 409 Conflict")
    void shouldRejectStaleVersion() throws Exception {
        AuthResponse auth = registerUser("stale.ver@example.com", "Stale Ver", "SecretPass123!");
        String token = "Bearer " + auth.accessToken();
        WorkspaceResponse ws = createWorkspace(token, "Stale Org", "stale-org");
        ProjectResponse proj = createProject(token, ws.id(), "Stale Project", "STALE");

        CreateIssueRequest createReq = new CreateIssueRequest("Stale Test", "Desc", IssueType.TASK, IssuePriority.LOW, null, null, 1);
        MvcResult createResult = mockMvc.perform(post("/api/v1/projects/" + proj.id() + "/issues")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        IssueResponse issue = objectMapper.readValue(createResult.getResponse().getContentAsString(), IssueResponse.class);

        // Advance to TO_DO (version 0 -> 1)
        executeTransition(token, issue.id(), IssueStatus.TO_DO, 0L);

        // Attempt transition to IN_PROGRESS using stale version 0L (current is 1L)
        TransitionIssueRequest staleReq = new TransitionIssueRequest(IssueStatus.IN_PROGRESS, 0L);
        mockMvc.perform(post("/api/v1/issues/" + issue.id() + "/transitions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staleReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ISSUE_VERSION_CONFLICT"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("Should add, list, and soft-delete issue comments with audit logging")
    void shouldSupportCommentLifecycle() throws Exception {
        AuthResponse auth = registerUser("commenter@example.com", "Comment User", "SecretPass123!");
        String token = "Bearer " + auth.accessToken();
        WorkspaceResponse ws = createWorkspace(token, "Comment Org", "comment-org");
        ProjectResponse proj = createProject(token, ws.id(), "Comment Project", "COMM");

        CreateIssueRequest createReq = new CreateIssueRequest("Commentable Issue", "Desc", IssueType.TASK, IssuePriority.MEDIUM, null, null, 2);
        MvcResult createResult = mockMvc.perform(post("/api/v1/projects/" + proj.id() + "/issues")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        IssueResponse issue = objectMapper.readValue(createResult.getResponse().getContentAsString(), IssueResponse.class);

        // 1. Add comment
        CreateCommentRequest commentReq = new CreateCommentRequest("This is a root cause analysis comment.");
        MvcResult commentResult = mockMvc.perform(post("/api/v1/issues/" + issue.id() + "/comments")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("This is a root cause analysis comment."))
                .andExpect(jsonPath("$.issueId").value(issue.id().toString()))
                .andExpect(jsonPath("$.authorId").value(auth.user().id().toString()))
                .andReturn();

        CommentResponse comment = objectMapper.readValue(commentResult.getResponse().getContentAsString(), CommentResponse.class);

        // 2. List comments
        mockMvc.perform(get("/api/v1/issues/" + issue.id() + "/comments")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(comment.id().toString()));

        // 3. Delete comment (soft delete)
        mockMvc.perform(delete("/api/v1/issues/" + issue.id() + "/comments/" + comment.id())
                        .header("Authorization", token))
                .andExpect(status().isNoContent());

        // 4. List comments again -> empty
        mockMvc.perform(get("/api/v1/issues/" + issue.id() + "/comments")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private IssueResponse executeTransition(String token, UUID issueId, IssueStatus targetStatus, Long version) throws Exception {
        TransitionIssueRequest req = new TransitionIssueRequest(targetStatus, version);
        MvcResult res = mockMvc.perform(post("/api/v1/issues/" + issueId + "/transitions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readValue(res.getResponse().getContentAsString(), IssueResponse.class);
    }

    private AuthResponse registerUser(String email, String name, String password) throws Exception {
        RegisterRequest registerReq = new RegisterRequest(email, name, password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
    }

    private WorkspaceResponse createWorkspace(String token, String name, String slug) throws Exception {
        CreateWorkspaceRequest wsReq = new CreateWorkspaceRequest(name, slug);
        MvcResult wsResult = mockMvc.perform(post("/api/v1/workspaces")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wsReq)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(wsResult.getResponse().getContentAsString(), WorkspaceResponse.class);
    }

    private ProjectResponse createProject(String token, UUID workspaceId, String name, String key) throws Exception {
        CreateProjectRequest projReq = new CreateProjectRequest(name, key, "Description for " + key);
        MvcResult projResult = mockMvc.perform(post("/api/v1/workspaces/" + workspaceId + "/projects")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projReq)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(projResult.getResponse().getContentAsString(), ProjectResponse.class);
    }
}
