package com.nextrack.identity.application.oauth2;

import com.nextrack.identity.application.oauth2.info.OAuth2UserInfo;
import com.nextrack.identity.application.oauth2.info.OAuth2UserInfoFactory;
import com.nextrack.identity.application.service.OAuth2AuthService;
import com.nextrack.identity.application.service.RefreshTokenService;
import com.nextrack.identity.domain.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.modulith.NamedInterface;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@NamedInterface
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final OAuth2AuthService oAuth2AuthService;
    private final RefreshTokenService refreshTokenService;
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieRepository;
    private final String redirectUri;
    private final long refreshTokenExpirationSeconds;

    public OAuth2AuthenticationSuccessHandler(
            OAuth2AuthService oAuth2AuthService,
            RefreshTokenService refreshTokenService,
            HttpCookieOAuth2AuthorizationRequestRepository cookieRepository,
            @Value("${nextrack.security.oauth2.redirect-uri:http://localhost:5173/auth/callback}") String redirectUri,
            @Value("${nextrack.security.refresh-token-expiration-seconds:604800}") long refreshTokenExpirationSeconds
    ) {
        this.oAuth2AuthService = oAuth2AuthService;
        this.refreshTokenService = refreshTokenService;
        this.cookieRepository = cookieRepository;
        this.redirectUri = redirectUri;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        OAuth2UserInfo userInfo = extractUserInfo(authentication);
        User user = oAuth2AuthService.processOAuth2Login(userInfo);

        // Issue refresh token inside token family
        RefreshTokenService.RotationResult rotationResult = refreshTokenService.createRefreshToken(user.getId());

        // Attach secure HttpOnly refresh token cookie
        ResponseCookie cookie = ResponseCookie.from("refresh_token", rotationResult.rawToken())
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/api/v1/auth")
                .maxAge(refreshTokenExpirationSeconds)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // Clean up temporary OAuth2 state cookie
        cookieRepository.removeAuthorizationRequest(request, response);

        // 302 Redirect to SPA callback with ZERO tokens in URL
        getRedirectStrategy().sendRedirect(request, response, redirectUri);
    }

    private OAuth2UserInfo extractUserInfo(Authentication authentication) {
        if (authentication.getPrincipal() instanceof CustomOAuth2User customUser) {
            return customUser.getUserInfo();
        }

        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            String registrationId = oauthToken.getAuthorizedClientRegistrationId();
            OAuth2User oauth2User = oauthToken.getPrincipal();
            return OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oauth2User.getAttributes(), null);
        }

        throw new IllegalArgumentException("Unsupported authentication principal type: " + authentication.getPrincipal().getClass());
    }
}
