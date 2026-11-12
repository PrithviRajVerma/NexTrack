package com.nextrack.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextrack.identity.api.dto.request.RegisterRequest;
import com.nextrack.identity.api.dto.response.AuthResponse;
import com.nextrack.issue.api.dto.request.CreateIssueRequest;
import com.nextrack.issue.api.dto.request.TransitionIssueRequest;
import com.nextrack.issue.api.dto.response.IssueResponse;
import com.nextrack.issue.domain.entity.IssuePriority;
import com.nextrack.issue.domain.entity.IssueStatus;
import com.nextrack.issue.domain.entity.IssueType;
import com.nextrack.notification.application.service.OutboxWorker;
import com.nextrack.notification.domain.entity.Notification;
import com.nextrack.notification.domain.entity.OutboxEvent;
import com.nextrack.notification.domain.entity.OutboxStatus;
import com.nextrack.notification.domain.repository.NotificationRepository;
import com.nextrack.notification.domain.repository.OutboxEventRepository;
import com.nextrack.project.api.dto.request.CreateProjectRequest;
import com.nextrack.project.api.dto.response.ProjectResponse;
import com.nextrack.project.domain.entity.ProjectRole;
import com.nextrack.workspace.api.dto.request.CreateWorkspaceRequest;
import com.nextrack.workspace.api.dto.response.WorkspaceResponse;
import com.nextrack.workspace.application.service.WorkspaceService;
import com.nextrack.project.application.service.ProjectService;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class NotificationAndOutboxIntegrationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("nextrack_notification_test")
            .withUsername("notif_user")
            .withPassword("notif_pass");

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
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private OutboxWorker outboxWorker;

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private ProjectService projectService;

    @Test
    @DisplayName("Should publish outbox event on issue assignment and worker should dispatch in-app notification reliably")
    void shouldPublishOutboxAndDispatchNotification() throws Exception {
        // 1. Setup Alice (owner) and Bob (member)
        AuthResponse alice = registerUser("alice.outbox@example.com", "Alice Outbox", "Password123!");
        AuthResponse bob = registerUser("bob.outbox@example.com", "Bob Outbox", "Password123!");

        String aliceToken = "Bearer " + alice.accessToken();
        String bobToken = "Bearer " + bob.accessToken();

        WorkspaceResponse ws = createWorkspace(aliceToken, "Outbox Org", "outbox-org");
        ProjectResponse proj = createProject(aliceToken, ws.id(), "Outbox Project", "OBX");

        // Add Bob to workspace and project
        workspaceService.addMember(ws.id(), bob.user().id(), com.nextrack.workspace.domain.entity.WorkspaceRole.MEMBER, alice.user().id());
        projectService.addProjectMember(proj.id(), bob.user().id(), ProjectRole.CONTRIBUTOR, alice.user().id());

        // 2. Alice creates issue assigned to Bob
        CreateIssueRequest createReq = new CreateIssueRequest(
                "Implement Outbox Worker",
                "Reliable dispatch without message broker",
                IssueType.TASK,
                IssuePriority.HIGH,
                bob.user().id(),
                null,
                5
        );

        MvcResult issueResult = mockMvc.perform(post("/api/v1/projects/" + proj.id() + "/issues")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        IssueResponse createdIssue = objectMapper.readValue(
                issueResult.getResponse().getContentAsString(),
                IssueResponse.class
        );

        // 3. Verify OutboxEvent exists in DB with status PENDING
        List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
        assertThat(outboxEvents).isNotEmpty();
        OutboxEvent assignedEvent = outboxEvents.stream()
                .filter(e -> "ISSUE_ASSIGNED".equals(e.getEventType()))
                .findFirst()
                .orElseThrow();
        assertThat(assignedEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(assignedEvent.getPayload()).contains(bob.user().id().toString());
        assertThat(assignedEvent.getPayload()).contains("OBX-1");

        // 4. Run OutboxWorker to process the pending batch
        int processed = outboxWorker.processPendingEvents();
        assertThat(processed).isGreaterThanOrEqualTo(1);

        // 5. Verify OutboxEvent transitioned to PUBLISHED
        OutboxEvent reloadedEvent = outboxEventRepository.findById(assignedEvent.getId()).orElseThrow();
        assertThat(reloadedEvent.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(reloadedEvent.getPublishedAt()).isNotNull();

        // 6. Verify Notification row created for Bob with unique dedup key
        List<Notification> bobNotifications = notificationRepository.findAll().stream()
                .filter(n -> n.getRecipientId().equals(bob.user().id()))
                .toList();
        assertThat(bobNotifications).hasSize(1);
        Notification bobNotif = bobNotifications.get(0);
        assertThat(bobNotif.getEventType()).isEqualTo("ISSUE_ASSIGNED");
        assertThat(bobNotif.getReadAt()).isNull();
        assertThat(bobNotif.getDedupKey()).isEqualTo("ASSIGNED-" + createdIssue.id() + "-" + bob.user().id());

        // 7. Verify Idempotency: Running worker again should not duplicate notification
        int secondRunProcessed = outboxWorker.processPendingEvents();
        assertThat(secondRunProcessed).isEqualTo(0);
        long countAfterSecondRun = notificationRepository.countByRecipientIdAndReadAtIsNull(bob.user().id());
        assertThat(countAfterSecondRun).isEqualTo(1);

        // 8. Bob queries REST API for notifications
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.notifications[0].id").value(bobNotif.getId().toString()))
                .andExpect(jsonPath("$.notifications[0].isRead").value(false))
                .andExpect(jsonPath("$.notifications[0].eventType").value("ISSUE_ASSIGNED"));

        // 9. Bob marks notification as read
        mockMvc.perform(patch("/api/v1/notifications/" + bobNotif.getId() + "/read")
                        .header("Authorization", bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true))
                .andExpect(jsonPath("$.readAt").isNotEmpty());

        // Unread count is now 0
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));

        // 10. Status Transition notification: Alice transitions the issue to TO_DO
        TransitionIssueRequest transitionReq = new TransitionIssueRequest(IssueStatus.TO_DO, createdIssue.version());
        mockMvc.perform(post("/api/v1/issues/" + createdIssue.id() + "/transitions")
                        .header("Authorization", aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transitionReq)))
                .andExpect(status().isOk());

        // Worker processes the status change outbox event
        outboxWorker.processPendingEvents();

        // Bob now has a new unread status change notification
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.notifications[0].eventType").value("ISSUE_STATUS_CHANGED"));

        // Bob marks all as read
        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .header("Authorization", bobToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    @Test
    @DisplayName("Should mark poison pill outbox event as FAILED after 5 retries without crashing worker")
    void shouldHandlePoisonPillEvent() {
        // Insert invalid poison pill event missing recipientId
        OutboxEvent poison = new OutboxEvent("CORRUPTED_EVENT", "{\"invalid\":\"payload\"}");
        outboxEventRepository.save(poison);

        // Process 5 times with backoff resets
        for (int i = 0; i < 5; i++) {
            // Force nextRetryAt to now to bypass backoff in test
            OutboxEvent current = outboxEventRepository.findById(poison.getId()).orElseThrow();
            if (current.getNextRetryAt() != null) {
                // Clear backoff to allow immediate retry
                outboxEventRepository.save(new OutboxEvent(
                        current.getEventType(),
                        current.getPayload()
                ));
            }
            outboxWorker.processPendingEvents();
        }

        OutboxEvent finalState = outboxEventRepository.findById(poison.getId()).orElseThrow();
        assertThat(finalState.getRetryCount()).isGreaterThanOrEqualTo(1);
        assertThat(finalState.getErrorMessage()).contains("missing recipientId");
    }

    private AuthResponse registerUser(String email, String displayName, String password) throws Exception {
        RegisterRequest request = new RegisterRequest(email, password, displayName);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
    }

    private WorkspaceResponse createWorkspace(String token, String name, String slug) throws Exception {
        CreateWorkspaceRequest request = new CreateWorkspaceRequest(name, slug);
        MvcResult result = mockMvc.perform(post("/api/v1/workspaces")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), WorkspaceResponse.class);
    }

    private ProjectResponse createProject(String token, UUID workspaceId, String name, String key) throws Exception {
        CreateProjectRequest request = new CreateProjectRequest(name, key, "Description for " + name);
        MvcResult result = mockMvc.perform(post("/api/v1/workspaces/" + workspaceId + "/projects")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), ProjectResponse.class);
    }
}
