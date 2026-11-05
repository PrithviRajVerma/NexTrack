package com.nextrack.identity.api.controller;

import com.nextrack.identity.api.dto.response.OAuthIdentityResponse;
import com.nextrack.identity.application.service.OAuth2AuthService;
import com.nextrack.identity.domain.entity.OAuthProvider;
import com.nextrack.identity.domain.entity.UserOAuthIdentity;
import com.nextrack.shared.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth/oauth2")
public class OAuth2Controller {

    private final OAuth2AuthService oAuth2AuthService;

    public OAuth2Controller(OAuth2AuthService oAuth2AuthService) {
        this.oAuth2AuthService = oAuth2AuthService;
    }

    @GetMapping("/identities")
    public ResponseEntity<List<OAuthIdentityResponse>> getLinkedIdentities(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<UserOAuthIdentity> identities = oAuth2AuthService.getLinkedIdentities(principal.id());
        List<OAuthIdentityResponse> response = identities.stream()
                .map(i -> new OAuthIdentityResponse(
                        i.getId(),
                        i.getProvider().name(),
                        i.getProviderUserId(),
                        i.getProviderEmail(),
                        i.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/identities/{provider}")
    public ResponseEntity<Void> unlinkIdentity(
            @PathVariable String provider,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        OAuthProvider oAuthProvider = OAuthProvider.valueOf(provider.toUpperCase());
        oAuth2AuthService.unlinkIdentity(principal.id(), oAuthProvider);
        return ResponseEntity.noContent().build();
    }
}
