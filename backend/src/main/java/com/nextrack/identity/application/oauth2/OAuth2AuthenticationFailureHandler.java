package com.nextrack.identity.application.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.modulith.NamedInterface;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@NamedInterface
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final HttpCookieOAuth2AuthorizationRequestRepository cookieRepository;
    private final String loginErrorUri;

    public OAuth2AuthenticationFailureHandler(
            HttpCookieOAuth2AuthorizationRequestRepository cookieRepository,
            @Value("${nextrack.security.oauth2.login-error-uri:http://localhost:5173/login?error=oauth2_failure}") String loginErrorUri
    ) {
        this.cookieRepository = cookieRepository;
        this.loginErrorUri = loginErrorUri;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        cookieRepository.removeAuthorizationRequest(request, response);

        String message = exception.getMessage() != null ? exception.getMessage() : "OAuth2 authentication failed";
        String separator = loginErrorUri.contains("?") ? "&" : "?";
        String targetUrl = loginErrorUri + separator + "message=" + URLEncoder.encode(message, StandardCharsets.UTF_8);

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
