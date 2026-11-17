package com.nextrack.development.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "integration_installations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_integration_installation", columnNames = {"provider", "installation_id"})
        }
)
public class IntegrationInstallation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(nullable = false, length = 32)
    private String provider = "GITHUB";

    @Column(name = "installation_id", nullable = false)
    private Long installationId;

    @Column(name = "account_name", nullable = false)
    private String accountName;

    @Column(name = "repository_selection", length = 32)
    private String repositorySelection;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String permissions;

    @Column(name = "encrypted_webhook_secret", columnDefinition = "text")
    private String encryptedWebhookSecret;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected IntegrationInstallation() {}

    public IntegrationInstallation(
            UUID workspaceId,
            String provider,
            Long installationId,
            String accountName,
            String repositorySelection,
            String permissions,
            String encryptedWebhookSecret
    ) {
        this.workspaceId = workspaceId;
        this.provider = provider != null ? provider : "GITHUB";
        this.installationId = installationId;
        this.accountName = accountName;
        this.repositorySelection = repositorySelection;
        this.permissions = permissions;
        this.encryptedWebhookSecret = encryptedWebhookSecret;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public String getProvider() {
        return provider;
    }

    public Long getInstallationId() {
        return installationId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getRepositorySelection() {
        return repositorySelection;
    }

    public void setRepositorySelection(String repositorySelection) {
        this.repositorySelection = repositorySelection;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public String getEncryptedWebhookSecret() {
        return encryptedWebhookSecret;
    }

    public void setEncryptedWebhookSecret(String encryptedWebhookSecret) {
        this.encryptedWebhookSecret = encryptedWebhookSecret;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
