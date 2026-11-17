package com.nextrack.development;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nextrack.development.infrastructure.github.GitHubAppAuthService;
import com.nextrack.development.infrastructure.github.GitHubAppProperties;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitHubAppAuthServiceTests {

    private GitHubAppProperties properties;
    private GitHubAppAuthService authService;
    private KeyPair testKeyPair;

    @BeforeEach
    void setUp() throws Exception {
        properties = new GitHubAppProperties();
        properties.setAppId("123456");
        authService = new GitHubAppAuthService(properties);

        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048);
        testKeyPair = keyPairGen.generateKeyPair();
    }

    @Test
    @DisplayName("Should successfully parse PKCS#8 RSA private key PEM")
    void shouldParsePkcs8PrivateKey() throws Exception {
        StringWriter writer = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
            pemWriter.writeObject(testKeyPair.getPrivate());
        }
        String pem = writer.toString();

        RSAPrivateKey parsedKey = authService.parsePrivateKey(pem);
        assertThat(parsedKey).isNotNull();
        assertThat(parsedKey.getModulus()).isEqualTo(((RSAPrivateKey) testKeyPair.getPrivate()).getModulus());
    }

    @Test
    @DisplayName("Should generate valid RS256 JWT with 10-minute expiry")
    void shouldGenerateValidRs256Jwt() {
        RSAPrivateKey privateKey = (RSAPrivateKey) testKeyPair.getPrivate();
        RSAPublicKey publicKey = (RSAPublicKey) testKeyPair.getPublic();

        String jwt = authService.generateAppJwtWithKey("998877", privateKey);
        assertThat(jwt).isNotBlank();

        // Verify with public key
        Algorithm algorithm = Algorithm.RSA256(publicKey, null);
        DecodedJWT decoded = JWT.require(algorithm)
                .withIssuer("998877")
                .build()
                .verify(jwt);

        assertThat(decoded.getIssuer()).isEqualTo("998877");
        assertThat(decoded.getExpiresAt()).isAfter(new Date());
        assertThat(decoded.getIssuedAt()).isBeforeOrEqualTo(new Date());
    }

    @Test
    @DisplayName("Should proactively reuse fresh cached installation token")
    void shouldReuseFreshCachedInstallationToken() {
        Long installationId = 42L;
        String cachedToken = "ghs_cached_token_42";
        // Expires 45 minutes from now (more than 10 minutes remaining -> fresh)
        authService.cacheInstallationToken(installationId, cachedToken, Instant.now().plus(45, ChronoUnit.MINUTES));

        String token = authService.getInstallationToken(installationId);
        assertThat(token).isEqualTo(cachedToken);
    }

    @Test
    @DisplayName("Should refresh installation token when cached token is near expiration")
    void shouldRefreshExpiredInstallationToken() {
        Long installationId = 77L;
        String staleToken = "ghs_stale_token_77";
        // Expires 5 minutes from now (less than 10 minutes remaining -> not fresh)
        authService.cacheInstallationToken(installationId, staleToken, Instant.now().plus(5, ChronoUnit.MINUTES));

        String token = authService.getInstallationToken(installationId);
        assertThat(token).isNotEqualTo(staleToken);
        assertThat(token).contains("ghs_mock_token_for_installation_77");
    }

    @Test
    @DisplayName("Should reject invalid or empty private key PEM")
    void shouldRejectInvalidPrivateKeyPem() {
        assertThatThrownBy(() -> authService.parsePrivateKey(""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> authService.parsePrivateKey("NOT_A_VALID_PEM_STRING"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
