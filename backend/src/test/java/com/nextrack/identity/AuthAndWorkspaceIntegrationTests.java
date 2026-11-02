package com.nextrack.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextrack.identity.api.dto.request.LoginRequest;
import com.nextrack.identity.api.dto.request.RegisterRequest;
import com.nextrack.identity.api.dto.response.AuthResponse;
import com.nextrack.project.api.dto.request.CreateProjectRequest;
import com.nextrack.project.api.dto.response.ProjectResponse;
import com.nextrack.project.domain.entity.ProjectRole;
import com.nextrack.project.domain.repository.ProjectIssueCounterRepository;
import com.nextrack.workspace.api.dto.request.CreateWorkspaceRequest;
import com.nextrack.workspace.api.dto.response.WorkspaceResponse;
import com.nextrack.workspace.domain.entity.WorkspaceRole;
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

import jakarta.servlet.http.Cookie;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthAndWorkspaceIntegrationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("nextrack_auth_test")
            .withUsername("auth_user")
            .withPassword("auth_pass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectIssueCounterRepository counterRepository;

    @Test
    void shouldRegisterAndAuthenticateUser() throws Exception {
        RegisterRequest registerReq = new RegisterRequest(
                "alice@example.com",
                "Alice Engineer",
                "securePassword123!"
        );

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("alice@example.com"))
                .andReturn();

        Cookie refreshCookie = registerResult.getResponse().getCookie("refresh_token");
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie.getValue()).isNotEmpty();

        // Login with registered credentials
        LoginRequest loginReq = new LoginRequest("alice@example.com", "securePassword123!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void shouldRejectInvalidCredentialsWithoutAccountEnumeration() throws Exception {
        LoginRequest loginReq = new LoginRequest("unknown@example.com", "badPassword123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_FAILED"));
    }

    @Test
    void shouldRotateRefreshTokenAndDetectTokenTheft() throws Exception {
        // Register Bob
        RegisterRequest registerReq = new RegisterRequest(
                "bob@example.com",
                "Bob Developer",
                "passwordForBob123!"
        );

        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Cookie originalCookie = regResult.getResponse().getCookie("refresh_token");
        assertThat(originalCookie).isNotNull();

        // 1. Legitimate Rotation
        MvcResult rotateResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(originalCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        Cookie secondCookie = rotateResult.getResponse().getCookie("refresh_token");
        assertThat(secondCookie).isNotNull();
        assertThat(secondCookie.getValue()).isNotEqualTo(originalCookie.getValue());

        // 2. Token Theft Attempt: Replay of first (already-rotated/revoked) token!
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(originalCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SECURITY_BREACH_DETECTED"));

        // 3. Assert that the second token in the family was also revoked due to breach detection!
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(secondCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SECURITY_BREACH_DETECTED"));
    }

    @Test
    void shouldCreateWorkspaceAndEnforceProjectKeyUniqueness() throws Exception {
        // Register Charlie
        RegisterRequest registerReq = new RegisterRequest(
                "charlie@example.com",
                "Charlie Lead",
                "securePass12345!"
        );

        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse auth = objectMapper.readValue(regResult.getResponse().getContentAsString(), AuthResponse.class);
        String token = "Bearer " + auth.accessToken();

        // Create Workspace
        CreateWorkspaceRequest wsReq = new CreateWorkspaceRequest("Acme Core", "acme-core");
        MvcResult wsResult = mockMvc.perform(post("/api/v1/workspaces")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wsReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("acme-core"))
                .andReturn();

        WorkspaceResponse ws = objectMapper.readValue(wsResult.getResponse().getContentAsString(), WorkspaceResponse.class);

        // Create Project NT in Workspace
        CreateProjectRequest projReq = new CreateProjectRequest("NexTrack Engine", "NT", "Core Engine Project");
        MvcResult projResult = mockMvc.perform(post("/api/v1/workspaces/" + ws.id() + "/projects")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectKey").value("NT"))
                .andReturn();

        ProjectResponse proj = objectMapper.readValue(projResult.getResponse().getContentAsString(), ProjectResponse.class);

        // Verify that ProjectIssueCounter was atomically initialized with last_issue_number = 0
        assertThat(counterRepository.findById(proj.id())).isPresent();
        assertThat(counterRepository.findById(proj.id()).get().getLastIssueNumber()).isEqualTo(0L);

        // Attempting to create duplicate project key 'NT' in same workspace must fail!
        mockMvc.perform(post("/api/v1/workspaces/" + ws.id() + "/projects")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Project key 'NT' already exists in this workspace"));
    }
}
