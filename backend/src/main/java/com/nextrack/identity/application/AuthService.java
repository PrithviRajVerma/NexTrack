package com.nextrack.identity.application;

import com.nextrack.identity.domain.User;
import com.nextrack.identity.domain.UserRepository;
import com.nextrack.identity.domain.UserStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthResult register(String email, String displayName, String rawPassword) {
        String normalizedEmail = email.toLowerCase().trim();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        String passwordHash = passwordEncoder.encode(rawPassword);
        User user = new User(normalizedEmail, displayName, passwordHash);
        user = userRepository.save(user);

        String accessToken = jwtTokenService.generateAccessToken(user.getId(), user.getEmail(), user.getDisplayName());
        RefreshTokenService.RotationResult refresh = refreshTokenService.createRefreshToken(user.getId());

        return new AuthResult(accessToken, refresh.rawToken(), user);
    }

    @Transactional
    public AuthResult login(String email, String rawPassword) {
        String normalizedEmail = email.toLowerCase().trim();
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Account is disabled or pending verification");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String accessToken = jwtTokenService.generateAccessToken(user.getId(), user.getEmail(), user.getDisplayName());
        RefreshTokenService.RotationResult refresh = refreshTokenService.createRefreshToken(user.getId());

        return new AuthResult(accessToken, refresh.rawToken(), user);
    }

    @Transactional(noRollbackFor = SecurityException.class)
    public AuthResult refresh(String rawRefreshToken) {
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotateRefreshToken(rawRefreshToken);
        User user = userRepository.findById(rotation.userId())
                .orElseThrow(() -> new IllegalStateException("Associated user no longer exists"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Account is disabled");
        }

        String newAccessToken = jwtTokenService.generateAccessToken(user.getId(), user.getEmail(), user.getDisplayName());
        return new AuthResult(newAccessToken, rotation.rawToken(), user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revokeToken(rawRefreshToken);
        }
    }

    public record AuthResult(String accessToken, String refreshToken, User user) {}
}
