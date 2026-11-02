package com.nextrack.identity.application.service;

import com.nextrack.identity.domain.entity.RefreshToken;
import com.nextrack.identity.domain.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenExpirationSeconds;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${nextrack.security.refresh-token-expiration-seconds:604800}") long refreshTokenExpirationSeconds
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }

    @Transactional
    public RotationResult createRefreshToken(UUID userId) {
        String rawToken = generateSecureToken();
        String tokenFamily = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawToken);

        Instant expiresAt = Instant.now().plusSeconds(refreshTokenExpirationSeconds);
        RefreshToken entity = new RefreshToken(userId, tokenHash, tokenFamily, expiresAt);
        refreshTokenRepository.save(entity);

        return new RotationResult(rawToken, userId);
    }

    @Transactional(noRollbackFor = SecurityException.class)
    public RotationResult rotateRefreshToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        RefreshToken currentToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        // Token Family Theft Detection
        if (currentToken.isRevoked()) {
            refreshTokenRepository.revokeFamily(currentToken.getTokenFamily());
            throw new SecurityException("Security violation: Attempted reuse of revoked refresh token. Entire token family invalidated.");
        }

        if (currentToken.isExpired()) {
            currentToken.revoke();
            throw new IllegalArgumentException("Refresh token has expired. Please re-authenticate.");
        }

        // Revoke current token
        currentToken.revoke();

        // Issue new token in same family
        String newRawToken = generateSecureToken();
        String newTokenHash = hashToken(newRawToken);
        Instant expiresAt = Instant.now().plusSeconds(refreshTokenExpirationSeconds);

        RefreshToken nextToken = new RefreshToken(
                currentToken.getUserId(),
                newTokenHash,
                currentToken.getTokenFamily(),
                expiresAt
        );
        refreshTokenRepository.save(nextToken);

        return new RotationResult(newRawToken, currentToken.getUserId());
    }

    @Transactional
    public void revokeToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(RefreshToken::revoke);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public record RotationResult(String rawToken, UUID userId) {}
}
