package com.nextrack.issue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextrack.identity.api.dto.request.RegisterRequest;
import com.nextrack.identity.api.dto.response.AuthResponse;
import com.nextrack.issue.api.dto.request.CreateCommentRequest;
import com.nextrack.issue.api.dto.request.CreateIssueRequest;
import com.nextrack.issue.api.dto.request.TransitionIssueRequest;
import com.nextrack.issue.api.dto.response.BoardViewResponse;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class BoardViewIntegrationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("nextrack_board_test")
            .withUsername("board_user")
            .withPassword("board_pass");

    @DynamicPropertySource
    static void configureProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "10");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should return read-optimized Kanban board with grouped columns and lateral counts")
    void shouldReturnGroupedKanbanBoardWithLateralCounts() throws Exception {
        // 1. Setup workspace & project
        AuthResponse auth = registerUser("board.lead@example.com", "Board Lead", "StrongPass123!");
        String token = "Bearer " + auth.accessToken();
        WorkspaceResponse ws = createWorkspace(token, "Kanban Workspace", "kanban-ws");
        ProjectResponse project = createProject(token, ws.id(), "Kanban Project", "KB");

        // 2. Create Issue 1 (starts in BACKLOG)
        CreateIssueRequest issue1Req = new CreateIssueRequest(
                "Task 1: Setup Architecture",
                "Scaffold backend project",
                IssueType.TASK,
                IssuePriority.HIGH,
                null,
                null,
                5
        );
        IssueResponse issue1 = createIssue(token, project.id(), issue1Req);

        // 3. Create Issue 2 and transition to TO_DO, then IN_PROGRESS
        CreateIssueRequest issue2Req = new CreateIssueRequest(
                "Task 2: Build Database View",
                "Create v_board_cards projection",
                IssueType.STORY,
                IssuePriority.URGENT,
                null,
                null,
                8
        );
        IssueResponse issue2 = createIssue(token, project.id(), issue2Req);
        transitionIssue(token, issue2.id(), IssueStatus.TO_DO, 0L);
        transitionIssue(token, issue2.id(), IssueStatus.IN_PROGRESS, 1L);

        // 4. Add comment to Issue 2 to verify comment count projection in view
        mockMvc.perform(post("/api/v1/issues/" + issue2.id() + "/comments")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest("Great progress on this view!"))))
                .andExpect(status().isCreated());

        // 5. Fetch Board View
        MvcResult boardResult = mockMvc.perform(get("/api/v1/projects/" + project.id() + "/board")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(project.id().toString()))
                .andExpect(jsonPath("$.projectKey").value("KB"))
                .andExpect(jsonPath("$.totalIssues").value(2))
                .andExpect(jsonPath("$.columns.BACKLOG").isArray())
                .andExpect(jsonPath("$.columns.TO_DO").isArray())
                .andExpect(jsonPath("$.columns.IN_PROGRESS").isArray())
                .andExpect(jsonPath("$.columns.IN_REVIEW").isArray())
                .andExpect(jsonPath("$.columns.DONE").isArray())
                .andReturn();

        BoardViewResponse board = objectMapper.readValue(boardResult.getResponse().getContentAsString(), BoardViewResponse.class);

        assertThat(board.columns().get(IssueStatus.BACKLOG)).hasSize(1);
        assertThat(board.columns().get(IssueStatus.BACKLOG).get(0).formattedKey()).isEqualTo("KB-1");
        assertThat(board.columns().get(IssueStatus.BACKLOG).get(0).commentCount()).isEqualTo(0);

        assertThat(board.columns().get(IssueStatus.IN_PROGRESS)).hasSize(1);
        assertThat(board.columns().get(IssueStatus.IN_PROGRESS).get(0).formattedKey()).isEqualTo("KB-2");
        assertThat(board.columns().get(IssueStatus.IN_PROGRESS).get(0).commentCount()).isEqualTo(1);
        assertThat(board.columns().get(IssueStatus.IN_PROGRESS).get(0).priority()).isEqualTo(IssuePriority.URGENT);

        assertThat(board.columns().get(IssueStatus.TO_DO)).isEmpty();
        assertThat(board.columns().get(IssueStatus.DONE)).isEmpty();
    }

    @Test
    @DisplayName("Should reject unauthorized board access for non-project member")
    void shouldRejectUnauthorizedBoardAccess() throws Exception {
        AuthResponse userA = registerUser("alice.board@example.com", "Alice Board", "AlicePass123!");
        String tokenA = "Bearer " + userA.accessToken();
        WorkspaceResponse ws = createWorkspace(tokenA, "Private Workspace", "priv-ws");
        ProjectResponse project = createProject(tokenA, ws.id(), "Private Project", "PRV");

        AuthResponse userB = registerUser("bob.board@example.com", "Bob Board", "BobPass123!");
        String tokenB = "Bearer " + userB.accessToken();

        mockMvc.perform(get("/api/v1/projects/" + project.id() + "/board")
                        .header("Authorization", tokenB))
                .andExpect(status().isForbidden());
    }

    private AuthResponse registerUser(String email, String name, String password) throws Exception {
        RegisterRequest req = new RegisterRequest(email, password, name);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
    }

    private WorkspaceResponse createWorkspace(String token, String name, String slug) throws Exception {
        CreateWorkspaceRequest req = new CreateWorkspaceRequest(name, slug);
        MvcResult result = mockMvc.perform(post("/api/v1/workspaces")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), WorkspaceResponse.class);
    }

    private ProjectResponse createProject(String token, UUID workspaceId, String name, String key) throws Exception {
        CreateProjectRequest req = new CreateProjectRequest(name, key, "Description for " + name);
        MvcResult result = mockMvc.perform(post("/api/v1/workspaces/" + workspaceId + "/projects")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), ProjectResponse.class);
    }

    private IssueResponse createIssue(String token, UUID projectId, CreateIssueRequest req) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/projects/" + projectId + "/issues")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), IssueResponse.class);
    }

    private void transitionIssue(String token, UUID issueId, IssueStatus targetStatus, long version) throws Exception {
        TransitionIssueRequest req = new TransitionIssueRequest(targetStatus, version);
        mockMvc.perform(post("/api/v1/issues/" + issueId + "/transitions")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }
}
