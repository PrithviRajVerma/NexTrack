package com.nextrack.issue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextrack.identity.api.dto.request.RegisterRequest;
import com.nextrack.identity.api.dto.response.AuthResponse;
import com.nextrack.issue.api.dto.request.CreateIssueRequest;
import com.nextrack.issue.api.dto.request.UpdateIssueRequest;
import com.nextrack.issue.api.dto.response.IssueResponse;
import com.nextrack.issue.domain.entity.Issue;
import com.nextrack.issue.domain.entity.IssuePriority;
import com.nextrack.issue.domain.entity.IssueStatus;
import com.nextrack.issue.domain.entity.IssueType;
import com.nextrack.issue.domain.repository.IssueRepository;
import com.nextrack.project.api.dto.request.CreateProjectRequest;
import com.nextrack.project.api.dto.response.ProjectResponse;
import com.nextrack.project.domain.repository.ProjectIssueCounterRepository;
import com.nextrack.workspace.api.dto.request.CreateWorkspaceRequest;
import com.nextrack.workspace.api.dto.response.WorkspaceResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class IssueIntegrationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("nextrack_issue_test")
            .withUsername("issue_user")
            .withPassword("issue_pass");

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

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private ProjectIssueCounterRepository counterRepository;

    @Test
    void shouldCreateIssuesWithMonotonicSequentialKeysAndQueryThem() throws Exception {
        AuthResponse auth = registerUser("alice.issue@example.com", "Alice Issue", "SecretPass123!");
        String token = "Bearer " + auth.accessToken();
        WorkspaceResponse ws = createWorkspace(token, "Alice Team", "alice-team");
        ProjectResponse proj = createProject(token, ws.id(), "Core Engine", "ENG");

        // 1. Create First Issue (ENG-1)
        CreateIssueRequest req1 = new CreateIssueRequest(
                "Initialize Gradle Multi-Project",
                "Setup Spring Boot backend and React SPA",
                IssueType.TASK,
                IssuePriority.HIGH,
                null,
                Instant.now().plusSeconds(86400),
                5
        );

        MvcResult res1 = mockMvc.perform(post("/api/v1/projects/" + proj.id() + "/issues")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.issueNumber").value(1))
                .andExpect(jsonPath("$.issueKey").value("ENG-1"))
                .andExpect(jsonPath("$.projectKey").value("ENG"))
                .andExpect(jsonPath("$.title").value("Initialize Gradle Multi-Project"))
                .andExpect(jsonPath("$.status").value("BACKLOG"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.version").value(0))
                .andReturn();

        IssueResponse issue1 = objectMapper.readValue(res1.getResponse().getContentAsString(), IssueResponse.class);

        // 2. Create Second Issue (ENG-2)
        CreateIssueRequest req2 = new CreateIssueRequest(
                "Setup Postgres Container",
                "Configure Testcontainers for integration tests",
                IssueType.STORY,
                IssuePriority.MEDIUM,
                null,
                null,
                3
        );

        MvcResult res2 = mockMvc.perform(post("/api/v1/projects/" + proj.id() + "/issues")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.issueNumber").value(2))
                .andExpect(jsonPath("$.issueKey").value("ENG-2"))
                .andExpect(jsonPath("$.projectKey").value("ENG"))
                .andReturn();

        // 3. Query Issue 1 by ID
        mockMvc.perform(get("/api/v1/issues/" + issue1.id())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(issue1.id().toString()))
                .andExpect(jsonPath("$.issueKey").value("ENG-1"));

        // 4. Query Issue 2 by Project ID and Issue Number
        mockMvc.perform(get("/api/v1/projects/" + proj.id() + "/issues/2")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issueNumber").value(2))
                .andExpect(jsonPath("$.issueKey").value("ENG-2"));
    }

    @Test
    void shouldRejectIneligibleAssigneeAndPreserveZeroGap() throws Exception {
        AuthResponse aliceAuth = registerUser("alice.gap@example.com", "Alice Gap", "SecretPass123!");
        String aliceToken = "Bearer " + aliceAuth.accessToken();
        WorkspaceResponse ws = createWorkspace(aliceToken, "Gap Org", "gap-org");
        ProjectResponse proj = createProject(aliceToken, ws.id(), "Zero Gap Project", "GAP");

        // Create Issue 1 (GAP-1)
        CreateIssueRequest req1 = new CreateIssueRequest("First Issue", "Desc 1", IssueType.TASK, IssuePriority.LOW, null, null, 1);
        mockMvc.perform(post("/api/v1/projects/" + proj.id() + "/issues")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.issueKey").value("GAP-1"));

        // Register external user Eve (not a project member)
        AuthResponse eveAuth = registerUser("eve.uninvited@example.com", "Eve Uninvited", "SecretPass123!");
        UUID eveId = eveAuth.user().id();

        // Attempt to create Issue assigning Eve -> Must fail with 400 Bad Request
        CreateIssueRequest invalidAssigneeReq = new CreateIssueRequest(
                "Invalid Assignment",
                "Assignee is not in project",
                IssueType.BUG,
                IssuePriority.HIGH,
                eveId,
                null,
                2
        );

        mockMvc.perform(post("/api/v1/projects/" + proj.id() + "/issues")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidAssigneeReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Assignee must be an active member of this project"));

        // Attempt to create Issue with blank title -> Must fail validation with 400
        CreateIssueRequest invalidValidationReq = new CreateIssueRequest("", "Blank title", IssueType.TASK, IssuePriority.LOW, null, null, null);
        mockMvc.perform(post("/api/v1/projects/" + proj.id() + "/issues")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidValidationReq)))
                .andExpect(status().isBadRequest());

        // Create next valid Issue -> Must be allocated GAP-2 (strictly zero gap!)
        CreateIssueRequest req2 = new CreateIssueRequest("Second Issue", "Desc 2", IssueType.TASK, IssuePriority.LOW, null, null, 2);
        mockMvc.perform(post("/api/v1/projects/" + proj.id() + "/issues")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.issueKey").value("GAP-2"))
                .andExpect(jsonPath("$.issueNumber").value(2));

        assertThat(counterRepository.findById(proj.id()).get().getLastIssueNumber()).isEqualTo(2L);
    }

    @Test
    void shouldDenyAccessToNonProjectMember() throws Exception {
        AuthResponse aliceAuth = registerUser("alice.secure@example.com", "Alice Secure", "SecretPass123!");
        String aliceToken = "Bearer " + aliceAuth.accessToken();
        WorkspaceResponse ws = createWorkspace(aliceToken, "Secure Org", "secure-org");
        ProjectResponse proj = createProject(aliceToken, ws.id(), "Secret Project", "SCRT");

        // Eve is not in this project
        AuthResponse eveAuth = registerUser("eve.hacker@example.com", "Eve Hacker", "SecretPass123!");
        String eveToken = "Bearer " + eveAuth.accessToken();

        CreateIssueRequest req = new CreateIssueRequest("Intrusion Attempt", null, IssueType.BUG, IssuePriority.URGENT, null, null, null);

        mockMvc.perform(post("/api/v1/projects/" + proj.id() + "/issues")
                        .header("Authorization", eveToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void shouldPatchIssueAndIncrementOptimisticLockVersion() throws Exception {
        AuthResponse auth = registerUser("alice.patch@example.com", "Alice Patch", "SecretPass123!");
        String token = "Bearer " + auth.accessToken();
        WorkspaceResponse ws = createWorkspace(token, "Patch Org", "patch-org");
        ProjectResponse proj = createProject(token, ws.id(), "Patch Project", "PTCH");

        CreateIssueRequest createReq = new CreateIssueRequest("Initial Title", "Initial Description", IssueType.TASK, IssuePriority.LOW, null, null, 1);
        MvcResult createResult = mockMvc.perform(post("/api/v1/projects/" + proj.id() + "/issues")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(0))
                .andReturn();

        IssueResponse createdIssue = objectMapper.readValue(createResult.getResponse().getContentAsString(), IssueResponse.class);

        // Update issue via PATCH
        UpdateIssueRequest patchReq = new UpdateIssueRequest("Updated Title", "Updated Description", IssueType.BUG, IssuePriority.URGENT, null, null, 8);
        mockMvc.perform(patch("/api/v1/issues/" + createdIssue.id())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.priority").value("URGENT"))
                .andExpect(jsonPath("$.storyPoints").value(8))
                .andExpect(jsonPath("$.version").value(1));

        // Direct optimistic locking conflict test at JPA aggregate level
        Issue copyA = issueRepository.findById(createdIssue.id()).orElseThrow();
        Issue copyB = issueRepository.findById(createdIssue.id()).orElseThrow();
        assertThat(copyA.getVersion()).isEqualTo(1L);
        assertThat(copyB.getVersion()).isEqualTo(1L);

        // Advance copyA in DB -> version bumps to 2
        copyA.updateDetails("Concurrent Title", "Desc", IssueType.BUG, IssuePriority.HIGH, null, null, 5);
        Issue savedA = issueRepository.saveAndFlush(copyA);
        assertThat(savedA.getVersion()).isEqualTo(2L);

        // Attempting to save copyB (which still holds version 1) must throw ObjectOptimisticLockingFailureException
        copyB.updateDetails("Stale Edit", "Desc", IssueType.BUG, IssuePriority.LOW, null, null, 2);
        assertThatThrownBy(() -> issueRepository.saveAndFlush(copyB))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void shouldListProjectIssuesWithBoundedPagination() throws Exception {
        AuthResponse auth = registerUser("alice.page@example.com", "Alice Page", "SecretPass123!");
        String token = "Bearer " + auth.accessToken();
        WorkspaceResponse ws = createWorkspace(token, "Page Org", "page-org");
        ProjectResponse proj = createProject(token, ws.id(), "Pagination Project", "PAGE");

        // Create 7 issues
        for (int i = 1; i <= 7; i++) {
            CreateIssueRequest req = new CreateIssueRequest("Issue #" + i, "Description " + i, IssueType.TASK, IssuePriority.MEDIUM, null, null, i);
            mockMvc.perform(post("/api/v1/projects/" + proj.id() + "/issues")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());
        }

        // Query page 0, size 3
        mockMvc.perform(get("/api/v1/projects/" + proj.id() + "/issues?page=0&size=3")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(7))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.numberOfElements").value(3))
                .andExpect(jsonPath("$.content[0].projectKey").value("PAGE"));
    }

    @Test
    void shouldAllocateMonotonicSequentialNumbersUnder50ThreadConcurrency() throws Exception {
        AuthResponse auth = registerUser("concurrency.lead@example.com", "Concurrency Lead", "SecretPass123!");
        String token = "Bearer " + auth.accessToken();
        WorkspaceResponse ws = createWorkspace(token, "Scale Org", "scale-org");
        ProjectResponse proj = createProject(token, ws.id(), "High Concurrency Project", "CONC");

        final int CONCURRENT_REQUESTS = 50;
        final int THREAD_POOL_SIZE = 50;

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        CountDownLatch readyLatch = new CountDownLatch(CONCURRENT_REQUESTS);
        CountDownLatch startLatch = new CountDownLatch(1);

        ConcurrentLinkedQueue<Long> allocatedNumbers = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 1; i <= CONCURRENT_REQUESTS; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    // All threads wait here until released simultaneously
                    startLatch.await();

                    CreateIssueRequest req = new CreateIssueRequest(
                            "Concurrent Task " + index,
                            "Fired under concurrent load testing",
                            IssueType.TASK,
                            IssuePriority.MEDIUM,
                            null,
                            null,
                            1
                    );

                    MvcResult result = mockMvc.perform(post("/api/v1/projects/" + proj.id() + "/issues")
                                    .header("Authorization", token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(req)))
                            .andReturn();

                    if (result.getResponse().getStatus() == 201) {
                        IssueResponse res = objectMapper.readValue(result.getResponse().getContentAsString(), IssueResponse.class);
                        allocatedNumbers.add(res.issueNumber());
                    } else {
                        errors.add(new AssertionError("Request " + index + " failed with status " + result.getResponse().getStatus() + ": " + result.getResponse().getContentAsString()));
                    }
                } catch (Throwable t) {
                    errors.add(t);
                }
            }));
        }

        // Wait until all 50 threads are queued and ready
        boolean ready = readyLatch.await(10, TimeUnit.SECONDS);
        assertThat(ready).isTrue();

        // Release all 50 threads simultaneously
        startLatch.countDown();

        // Wait for all tasks to complete
        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }

        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        // Assert 0 failures during concurrent execution
        assertThat(errors).isEmpty();

        // Assert all 50 requests successfully allocated issue numbers
        assertThat(allocatedNumbers).hasSize(CONCURRENT_REQUESTS);

        // Assert that the allocated numbers are strictly 1..50 with ZERO DUPLICATES and ZERO GAPS!
        List<Long> sortedAllocated = allocatedNumbers.stream().sorted().toList();
        List<Long> expectedNumbers = LongStream.rangeClosed(1, CONCURRENT_REQUESTS).boxed().toList();

        assertThat(sortedAllocated)
                .as("All 50 issues must receive monotonically increasing sequential numbers 1..50")
                .isEqualTo(expectedNumbers);

        // Verify the database row-counter reached exactly 50
        assertThat(counterRepository.findById(proj.id()).get().getLastIssueNumber())
                .isEqualTo((long) CONCURRENT_REQUESTS);

        // Verify the issue table has exactly 50 rows for this project
        assertThat(issueRepository.findByProjectId(proj.id(), org.springframework.data.domain.Pageable.unpaged()).getTotalElements())
                .isEqualTo((long) CONCURRENT_REQUESTS);
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
