package com.nextrack.development;

import com.nextrack.development.infrastructure.github.GitHubAppProperties;
import com.nextrack.development.infrastructure.security.CachedBodyHttpServletRequest;
import com.nextrack.development.infrastructure.security.WebhookSignatureFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookSignatureFilterTests {

    private static final String SECRET = "super_secret_test_key_123";
    private static final String WEBHOOK_URI = "/api/v1/integrations/github/webhooks";

    private WebhookSignatureFilter filter;

    @BeforeEach
    void setUp() {
        GitHubAppProperties properties = new GitHubAppProperties();
        properties.getWebhook().setSecret(SECRET);
        properties.getWebhook().setMaxBodyBytes(2 * 1024 * 1024); // 2 MB
        filter = new WebhookSignatureFilter(properties);
    }

    private String computeHmacSignature(byte[] body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
    }

    @Test
    @DisplayName("Should pass valid HMAC signature and wrap request in CachedBodyHttpServletRequest")
    void shouldPassValidHmacSignature() throws Exception {
        byte[] body = "{\"action\":\"ping\",\"zen\":\"Keep it simple.\"}".getBytes(StandardCharsets.UTF_8);
        String validSignature = computeHmacSignature(body, SECRET);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", WEBHOOK_URI);
        request.setContent(body);
        request.addHeader("X-Hub-Signature-256", validSignature);
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean filterChainInvoked = new AtomicBoolean(false);
        FilterChain filterChain = (req, res) -> {
            filterChainInvoked.set(true);
            assertThat(req).isInstanceOf(CachedBodyHttpServletRequest.class);
            // Verify body is replayable
            try (ServletInputStream in = req.getInputStream()) {
                byte[] readBytes = in.readAllBytes();
                assertThat(readBytes).isEqualTo(body);
            }
        };

        filter.doFilter(request, response, filterChain);

        assertThat(filterChainInvoked.get()).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Should return 401 Unauthorized for forged or invalid HMAC signature")
    void shouldRejectForgedHmacSignature() throws Exception {
        byte[] body = "{\"action\":\"push\",\"ref\":\"refs/heads/main\"}".getBytes(StandardCharsets.UTF_8);
        String forgedSignature = "sha256=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

        MockHttpServletRequest request = new MockHttpServletRequest("POST", WEBHOOK_URI);
        request.setContent(body);
        request.addHeader("X-Hub-Signature-256", forgedSignature);
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean filterChainInvoked = new AtomicBoolean(false);
        filter.doFilter(request, response, (req, res) -> filterChainInvoked.set(true));

        assertThat(filterChainInvoked.get()).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("INVALID_WEBHOOK_SIGNATURE");
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when X-Hub-Signature-256 header is missing")
    void shouldRejectMissingSignatureHeader() throws Exception {
        byte[] body = "{\"action\":\"ping\"}".getBytes(StandardCharsets.UTF_8);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", WEBHOOK_URI);
        request.setContent(body);
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean filterChainInvoked = new AtomicBoolean(false);
        filter.doFilter(request, response, (req, res) -> filterChainInvoked.set(true));

        assertThat(filterChainInvoked.get()).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("MISSING_WEBHOOK_SIGNATURE");
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when signature does not start with sha256=")
    void shouldRejectMalformedSignaturePrefix() throws Exception {
        byte[] body = "{\"action\":\"ping\"}".getBytes(StandardCharsets.UTF_8);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", WEBHOOK_URI);
        request.setContent(body);
        request.addHeader("X-Hub-Signature-256", "md5=1234567890abcdef");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean filterChainInvoked = new AtomicBoolean(false);
        filter.doFilter(request, response, (req, res) -> filterChainInvoked.set(true));

        assertThat(filterChainInvoked.get()).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("INVALID_WEBHOOK_SIGNATURE");
    }

    @Test
    @DisplayName("Should return 413 Payload Too Large when payload exceeds 2 MB")
    void shouldRejectPayloadExceedingMaxBytes() throws Exception {
        // Exceed 2 MB
        byte[] largeBody = new byte[2 * 1024 * 1024 + 100];
        String signature = computeHmacSignature(largeBody, SECRET);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", WEBHOOK_URI);
        request.setContent(largeBody);
        request.addHeader("X-Hub-Signature-256", signature);
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean filterChainInvoked = new AtomicBoolean(false);
        filter.doFilter(request, response, (req, res) -> filterChainInvoked.set(true));

        assertThat(filterChainInvoked.get()).isFalse();
        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentAsString()).contains("PAYLOAD_TOO_LARGE");
    }

    @Test
    @DisplayName("Should bypass filter for non-webhook URIs")
    void shouldBypassFilterForOtherUris() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/issues");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicBoolean filterChainInvoked = new AtomicBoolean(false);
        filter.doFilter(request, response, (req, res) -> filterChainInvoked.set(true));

        assertThat(filterChainInvoked.get()).isTrue();
    }
}
