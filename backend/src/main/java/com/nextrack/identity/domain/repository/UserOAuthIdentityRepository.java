package com.nextrack.identity.domain.repository;

import com.nextrack.identity.domain.entity.OAuthProvider;
import com.nextrack.identity.domain.entity.UserOAuthIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserOAuthIdentityRepository extends JpaRepository<UserOAuthIdentity, UUID> {

    Optional<UserOAuthIdentity> findByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);

    List<UserOAuthIdentity> findByUserId(UUID userId);

    boolean existsByProviderAndProviderUserId(OAuthProvider provider, String providerUserId);

    void deleteByUserIdAndProvider(UUID userId, OAuthProvider provider);

    long countByUserId(UUID userId);
}
