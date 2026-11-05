package com.nextrack.identity.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_oauth_identities")
public class UserOAuthIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OAuthProvider provider;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    @Column(name = "provider_email")
    private String providerEmail;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected UserOAuthIdentity() {}

    public UserOAuthIdentity(
            UUID userId,
            OAuthProvider provider,
            String providerUserId,
            String providerEmail,
            boolean emailVerified
    ) {
        this.userId = userId;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.providerEmail = providerEmail != null ? providerEmail.toLowerCase().trim() : null;
        this.emailVerified = emailVerified;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public OAuthProvider getProvider() { return provider; }
    public String getProviderUserId() { return providerUserId; }
    public String getProviderEmail() { return providerEmail; }
    public boolean isEmailVerified() { return emailVerified; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void updateEmail(String providerEmail, boolean emailVerified) {
        this.providerEmail = providerEmail != null ? providerEmail.toLowerCase().trim() : null;
        this.emailVerified = emailVerified;
        this.updatedAt = Instant.now();
    }
}
