package com.nextrack.identity.api.controller;

import com.nextrack.identity.api.dto.request.LoginRequest;
import com.nextrack.identity.api.dto.request.RefreshTokenRequest;
import com.nextrack.identity.api.dto.request.RegisterRequest;
import com.nextrack.identity.api.dto.response.AuthResponse;
import com.nextrack.identity.api.mapper.UserMapper;
import com.nextrack.identity.application.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final long accessTokenExpirationSeconds;
    private final long refreshTokenExpirationSeconds;

    public AuthController(
            AuthService authService,
            @Value("${nextrack.security.access-token-expiration-seconds:900}") long accessTokenExpirationSeconds,
            @Value("${nextrack.security.refresh-token-expiration-seconds:604800}") long refreshTokenExpirationSeconds
    ) {
        this.authService = authService;
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response
    ) {
        AuthService.AuthResult result = authService.register(
                request.email(),
                request.displayName(),
                request.password()
        );
        addRefreshTokenCookie(response, result.refreshToken(), refreshTokenExpirationSeconds);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                AuthResponse.of(result.accessToken(), accessTokenExpirationSeconds, UserMapper.toDto(result.user()))
        );
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthService.AuthResult result = authService.login(request.email(), request.password());
        addRefreshTokenCookie(response, result.refreshToken(), refreshTokenExpirationSeconds);

        return ResponseEntity.ok(
                AuthResponse.of(result.accessToken(), accessTokenExpirationSeconds, UserMapper.toDto(result.user()))
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshTokenCookie,
            @RequestBody(required = false) RefreshTokenRequest requestBody,
            HttpServletResponse response
    ) {
        String token = refreshTokenCookie;
        if (token == null && requestBody != null) {
            token = requestBody.refreshToken();
        }

        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AuthService.AuthResult result = authService.refresh(token);
        addRefreshTokenCookie(response, result.refreshToken(), refreshTokenExpirationSeconds);

        return ResponseEntity.ok(
                AuthResponse.of(result.accessToken(), accessTokenExpirationSeconds, UserMapper.toDto(result.user()))
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshTokenCookie,
            HttpServletResponse response
    ) {
        if (refreshTokenCookie != null) {
            authService.logout(refreshTokenCookie);
        }
        deleteRefreshTokenCookie(response);
        return ResponseEntity.noContent().build();
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken, long maxAge) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(false) // Set to true in production over HTTPS
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void deleteRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
