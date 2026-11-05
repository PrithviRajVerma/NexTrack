package com.nextrack.identity.application.service;

import com.nextrack.identity.application.oauth2.info.OAuth2UserInfo;
import com.nextrack.identity.domain.entity.OAuthProvider;
import com.nextrack.identity.domain.entity.User;
import com.nextrack.identity.domain.entity.UserOAuthIdentity;
import com.nextrack.identity.domain.repository.UserOAuthIdentityRepository;
import com.nextrack.identity.domain.repository.UserRepository;
import com.nextrack.workspace.application.service.WorkspaceService;
import org.springframework.modulith.NamedInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@NamedInterface
@Service
public class OAuth2AuthService {

    private final UserRepository userRepository;
    private final UserOAuthIdentityRepository oauthIdentityRepository;
    private final WorkspaceService workspaceService;

    public OAuth2AuthService(
            UserRepository userRepository,
            UserOAuthIdentityRepository oauthIdentityRepository,
            WorkspaceService workspaceService
    ) {
        this.userRepository = userRepository;
        this.oauthIdentityRepository = oauthIdentityRepository;
        this.workspaceService = workspaceService;
    }

    @Transactional
    public User processOAuth2Login(OAuth2UserInfo userInfo) {
        if (userInfo.getEmail() == null || userInfo.getEmail().isBlank()) {
            throw new IllegalArgumentException("OAuth2 provider did not supply a valid email address");
        }

        // 1. Check if identity already linked to a user
        Optional<UserOAuthIdentity> existingIdentity = oauthIdentityRepository
                .findByProviderAndProviderUserId(userInfo.getProvider(), userInfo.getProviderUserId());

        if (existingIdentity.isPresent()) {
            User user = userRepository.findById(existingIdentity.get().getUserId())
                    .orElseThrow(() -> new IllegalStateException("Associated user not found for identity"));

            // Sync latest metadata
            if (userInfo.getAvatarUrl() != null && !userInfo.getAvatarUrl().equals(user.getAvatarUrl())) {
                user.updateAvatarUrl(userInfo.getAvatarUrl());
            }
            if (userInfo.getDisplayName() != null && !userInfo.getDisplayName().equals(user.getDisplayName())) {
                user.updateDisplayName(userInfo.getDisplayName());
            }
            return userRepository.save(user);
        }

        // 2. Check if a user with this email already exists
        Optional<User> existingUser = userRepository.findByEmailIgnoreCase(userInfo.getEmail());

        if (existingUser.isPresent()) {
            User user = existingUser.get();

            // Account Takeover Defense: Only link if provider verified the email
            if (!userInfo.isEmailVerified()) {
                throw new IllegalStateException(
                        "Cannot auto-link account: Email '" + userInfo.getEmail() +
                        "' is not verified by " + userInfo.getProvider()
                );
            }

            UserOAuthIdentity newIdentity = new UserOAuthIdentity(
                    user.getId(),
                    userInfo.getProvider(),
                    userInfo.getProviderUserId(),
                    userInfo.getEmail(),
                    true
            );
            oauthIdentityRepository.save(newIdentity);

            if (user.getAvatarUrl() == null && userInfo.getAvatarUrl() != null) {
                user.updateAvatarUrl(userInfo.getAvatarUrl());
            }
            return userRepository.save(user);
        }

        // 3. JIT Provision new User and default Personal Workspace
        User newUser = new User(
                userInfo.getEmail(),
                userInfo.getDisplayName(),
                null,
                userInfo.getAvatarUrl()
        );
        newUser = userRepository.save(newUser);

        UserOAuthIdentity newIdentity = new UserOAuthIdentity(
                newUser.getId(),
                userInfo.getProvider(),
                userInfo.getProviderUserId(),
                userInfo.getEmail(),
                userInfo.isEmailVerified()
        );
        oauthIdentityRepository.save(newIdentity);

        // Auto-provision personal workspace
        String baseSlug = sanitizeSlug(newUser.getDisplayName());
        String uniqueSlug = baseSlug + "-" + UUID.randomUUID().toString().substring(0, 4);
        workspaceService.createWorkspace(newUser.getDisplayName() + "'s Team", uniqueSlug, newUser.getId());

        return newUser;
    }

    @Transactional(readOnly = true)
    public List<UserOAuthIdentity> getLinkedIdentities(UUID userId) {
        return oauthIdentityRepository.findByUserId(userId);
    }

    @Transactional
    public void unlinkIdentity(UUID userId, OAuthProvider provider) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        long identityCount = oauthIdentityRepository.countByUserId(userId);
        if (!user.hasPassword() && identityCount <= 1) {
            throw new IllegalStateException(
                    "Cannot unlink sole authentication identity. Set a password first or link another provider."
            );
        }

        oauthIdentityRepository.deleteByUserIdAndProvider(userId, provider);
    }

    private String sanitizeSlug(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return "user-workspace";
        }
        String cleaned = displayName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return cleaned.isBlank() ? "workspace" : cleaned;
    }
}
