package com.nextrack.attachment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextrack.attachment.api.dto.response.AttachmentResponse;
import com.nextrack.attachment.domain.repository.AttachmentRepository;
import com.nextrack.identity.api.dto.request.RegisterRequest;
import com.nextrack.identity.api.dto.response.AuthResponse;
import com.nextrack.issue.api.dto.request.CreateIssueRequest;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AttachmentIntegrationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("nextrack_attachment_test")
            .withUsername("attach_user")
            .withPassword("attach_pass");

    @DynamicPropertySource
    static void configureProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("nextrack.attachments.storage-dir", () -> "/tmp/nextrack-test-attachments-" + UUID.randomUUID());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Test
    @DisplayName("Upload valid PNG attachment, list it, and stream download successfully")
    void uploadAndDownloadValidAttachment() throws Exception {
        AuthResponse auth = registerUser("owner.attach@example.com", "Attach Owner", "SecretPass123!");
        String token = "Bearer " + auth.accessToken();
        WorkspaceResponse ws = createWorkspace(token, "Attach WS", "attach-ws-" + UUID.randomUUID().toString().substring(0, 6));
        ProjectResponse proj = createProject(token, ws.id(), "Design System", "DS");
        IssueResponse issue = createIssue(token, proj.id(), "Create Design Tokens", "Tokens for Linear dial theme");

        byte[] validPng = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x10, 0x20, 0x30};
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "tokens-spec.png",
                "image/png",
                validPng
        );

        // 1. Upload Attachment
        MvcResult uploadRes = mockMvc.perform(multipart("/api/v1/issues/" + issue.id() + "/attachments")
                        .file(file)
                        .header("Authorization", token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFilename").value("tokens-spec.png"))
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.sizeBytes").value(validPng.length))
                .andReturn();

        AttachmentResponse uploaded = objectMapper.readValue(uploadRes.getResponse().getContentAsString(), AttachmentResponse.class);
        assertThat(uploaded.id()).isNotNull();
        assertThat(uploaded.checksum()).isNotBlank();

        // 2. List Attachments
        mockMvc.perform(get("/api/v1/issues/" + issue.id() + "/attachments")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(uploaded.id().toString()));

        // 3. Download Attachment Stream
        MvcResult downloadRes = mockMvc.perform(get("/api/v1/issues/" + issue.id() + "/attachments/" + uploaded.id())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"tokens-spec.png\""))
                .andReturn();

        assertThat(downloadRes.getResponse().getContentAsByteArray()).isEqualTo(validPng);

        // 4. Verify Board Projection view updates attachment_count
        MvcResult boardRes = mockMvc.perform(get("/api/v1/projects/" + proj.id() + "/board")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn();

        BoardViewResponse board = objectMapper.readValue(boardRes.getResponse().getContentAsString(), BoardViewResponse.class);
        assertThat(board.columns().get("BACKLOG")).hasSize(1);
        assertThat(board.columns().get("BACKLOG").get(0).attachmentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Spoofed executable disguised as .png is rejected before DB persistence")
    void rejectSpoofedExecutableDisguisedAsPng() throws Exception {
        AuthResponse auth = registerUser("sec.user@example.com", "Sec User", "SecretPass123!");
        String token = "Bearer " + auth.accessToken();
        WorkspaceResponse ws = createWorkspace(token, "Sec WS", "sec-ws-" + UUID.randomUUID().toString().substring(0, 6));
        ProjectResponse proj = createProject(token, ws.id(), "Security App", "SEC");
        IssueResponse issue = createIssue(token, proj.id(), "Audit file pipeline", "Check magic bytes");

        byte[] fakeExe = new byte[]{0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "trojan-horse.png",
                "image/png",
                fakeExe
        );

        mockMvc.perform(multipart("/api/v1/issues/" + issue.id() + "/attachments")
                        .file(file)
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("DISALLOWED_FILE_TYPE"));

        assertThat(attachmentRepository.countByIssueIdAndDeletedAtIsNull(issue.id())).isZero();
    }

    @Test
    @DisplayName("Unauthorized user from another workspace cannot download attachment")
    void unauthorizedUserCannotDownloadAttachment() throws Exception {
        // Workspace A
        AuthResponse userA = registerUser("user.a@example.com", "User A", "SecretPass123!");
        String tokenA = "Bearer " + userA.accessToken();
        WorkspaceResponse wsA = createWorkspace(tokenA, "WS A", "ws-a-" + UUID.randomUUID().toString().substring(0, 6));
        ProjectResponse projA = createProject(tokenA, wsA.id(), "Secret Project", "SECPROJ");
        IssueResponse issueA = createIssue(tokenA, projA.id(), "Confidential Specs", "Internal document");

        byte[] pdfBytes = "%PDF-1.4\nConfidential".getBytes(StandardCharsets.US_ASCII);
        MockMultipartFile file = new MockMultipartFile("file", "secret.pdf", "application/pdf", pdfBytes);

        MvcResult uploadRes = mockMvc.perform(multipart("/api/v1/issues/" + issueA.id() + "/attachments")
                        .file(file)
                        .header("Authorization", tokenA))
                .andExpect(status().isCreated())
                .andReturn();

        AttachmentResponse uploaded = objectMapper.readValue(uploadRes.getResponse().getContentAsString(), AttachmentResponse.class);

        // Workspace B (User B tries to access User A's attachment)
        AuthResponse userB = registerUser("user.b@example.com", "User B", "SecretPass123!");
        String tokenB = "Bearer " + userB.accessToken();

        mockMvc.perform(get("/api/v1/issues/" + issueA.id() + "/attachments/" + uploaded.id())
                        .header("Authorization", tokenB))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Soft-delete attachment removes it from active list and projection counts")
    void softDeleteAttachmentRemovesFromList() throws Exception {
        AuthResponse auth = registerUser("del.user@example.com", "Del User", "SecretPass123!");
        String token = "Bearer " + auth.accessToken();
        WorkspaceResponse ws = createWorkspace(token, "Del WS", "del-ws-" + UUID.randomUUID().toString().substring(0, 6));
        ProjectResponse proj = createProject(token, ws.id(), "Cleanup Project", "CLN");
        IssueResponse issue = createIssue(token, proj.id(), "Cleanup task", "Delete temp files");

        byte[] textBytes = "Temporary notes".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", textBytes);

        MvcResult uploadRes = mockMvc.perform(multipart("/api/v1/issues/" + issue.id() + "/attachments")
                        .file(file)
                        .header("Authorization", token))
                .andExpect(status().isCreated())
                .andReturn();

        AttachmentResponse uploaded = objectMapper.readValue(uploadRes.getResponse().getContentAsString(), AttachmentResponse.class);

        // Soft delete
        mockMvc.perform(delete("/api/v1/issues/" + issue.id() + "/attachments/" + uploaded.id())
                        .header("Authorization", token))
                .andExpect(status().isNoContent());

        // Subsequent list is empty
        mockMvc.perform(get("/api/v1/issues/" + issue.id() + "/attachments")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Search & filter issues using Spring Data JPA Specifications")
    void searchAndFilterIssues() throws Exception {
        AuthResponse auth = registerUser("search.user@example.com", "Search User", "SecretPass123!");
        String token = "Bearer " + auth.accessToken();
        WorkspaceResponse ws = createWorkspace(token, "Search WS", "search-ws-" + UUID.randomUUID().toString().substring(0, 6));
        ProjectResponse proj = createProject(token, ws.id(), "Searchable Project", "SRCH");

        createCustomIssue(token, proj.id(), "Fix PostgreSQL HikariCP connection timeout", "Database pooling", IssuePriority.URGENT, IssueType.BUG);
        createCustomIssue(token, proj.id(), "Implement Vite Tailwind dark mode theme", "Frontend styling", IssuePriority.LOW, IssueType.TASK);
        createCustomIssue(token, proj.id(), "Configure Spring Security OAuth2 GitHub", "Authentication provider", IssuePriority.HIGH, IssueType.STORY);

        // 1. Full text search on "HikariCP"
        mockMvc.perform(get("/api/v1/projects/" + proj.id() + "/issues?search=HikariCP")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Fix PostgreSQL HikariCP connection timeout"));

        // 2. Filter by priority "URGENT"
        mockMvc.perform(get("/api/v1/projects/" + proj.id() + "/issues?priority=URGENT")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].priority").value("URGENT"));

        // 3. Filter by type "FEATURE"
        mockMvc.perform(get("/api/v1/projects/" + proj.id() + "/issues?type=FEATURE")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Configure Spring Security OAuth2 GitHub"));
    }

    // --- Helper Methods ---

    private AuthResponse registerUser(String email, String displayName, String password) throws Exception {
        RegisterRequest req = new RegisterRequest(email, displayName, password);
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
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(res.getResponse().getContentAsString(), WorkspaceResponse.class);
    }

    private ProjectResponse createProject(String token, UUID workspaceId, String name, String key) throws Exception {
        CreateProjectRequest req = new CreateProjectRequest(name, key, "Description for " + key);
        MvcResult res = mockMvc.perform(post("/api/v1/workspaces/" + workspaceId + "/projects")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(res.getResponse().getContentAsString(), ProjectResponse.class);
    }

    private IssueResponse createIssue(String token, UUID projectId, String title, String desc) throws Exception {
        return createCustomIssue(token, projectId, title, desc, IssuePriority.MEDIUM, IssueType.TASK);
    }

    private IssueResponse createCustomIssue(String token, UUID projectId, String title, String desc, IssuePriority priority, IssueType type) throws Exception {
        CreateIssueRequest req = new CreateIssueRequest(
                title,
                desc,
                type,
                priority,
                null,
                Instant.now().plusSeconds(86400),
                3
        );
        MvcResult res = mockMvc.perform(post("/api/v1/projects/" + projectId + "/issues")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(res.getResponse().getContentAsString(), IssueResponse.class);
    }
}
