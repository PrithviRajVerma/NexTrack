package com.nextrack.identity.application;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.modulith.NamedInterface;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@NamedInterface
@Service
public class JwtTokenService {

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final long expirationSeconds;

    public JwtTokenService(
            RsaKeyProvider rsaKeyProvider,
            @Value("${nextrack.security.access-token-expiration-seconds:900}") long expirationSeconds
    ) {
        this.algorithm = Algorithm.RSA256(rsaKeyProvider.getPublicKey(), rsaKeyProvider.getPrivateKey());
        this.verifier = JWT.require(this.algorithm)
                .withIssuer("nextrack-auth")
                .build();
        this.expirationSeconds = expirationSeconds;
    }

    public String generateAccessToken(UUID userId, String email, String displayName) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationSeconds);

        return JWT.create()
                .withIssuer("nextrack-auth")
                .withSubject(userId.toString())
                .withClaim("email", email)
                .withClaim("name", displayName)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(expiresAt))
                .sign(algorithm);
    }

    public Optional<DecodedJWT> verifyToken(String token) {
        try {
            return Optional.of(verifier.verify(token));
        } catch (JWTVerificationException e) {
            return Optional.empty();
        }
    }
}
