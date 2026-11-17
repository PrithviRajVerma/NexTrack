package com.nextrack.development.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextrack.development.infrastructure.github.GitHubAppProperties;
import com.nextrack.shared.api.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Pre-Parsing Cryptographic Filter that authenticates incoming GitHub webhooks
 * over raw HTTP request bytes before any JSON deserialization occurs.
 */
@Component
public class WebhookSignatureFilter extends OncePerRequestFilter {

    public static final String GITHUB_WEBHOOK_URI = "/api/v1/integrations/github/webhooks";
    public static final String SIGNATURE_HEADER = "X-Hub-Signature-256";

    private final GitHubAppProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public WebhookSignatureFilter(GitHubAppProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (!uri.equals(GITHUB_WEBHOOK_URI) && !uri.endsWith(GITHUB_WEBHOOK_URI)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        int maxBytes = properties.getWebhook().getMaxBodyBytes();
        long contentLength = request.getContentLengthLong();
        if (contentLength > maxBytes) {
            sendProblemResponse(
                    response,
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "Payload Too Large",
                    "Webhook payload exceeds maximum permitted size of " + (maxBytes / (1024 * 1024)) + " MB.",
                    "PAYLOAD_TOO_LARGE",
                    request.getRequestURI()
            );
            return;
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int bytesRead;
        int totalRead = 0;
        InputStream inputStream = request.getInputStream();

        while ((bytesRead = inputStream.read(chunk)) != -1) {
            totalRead += bytesRead;
            if (totalRead > maxBytes) {
                sendProblemResponse(
                        response,
                        HttpStatus.PAYLOAD_TOO_LARGE,
                        "Payload Too Large",
                        "Webhook payload stream exceeded maximum permitted size of " + (maxBytes / (1024 * 1024)) + " MB.",
                        "PAYLOAD_TOO_LARGE",
                        request.getRequestURI()
                );
                return;
            }
            buffer.write(chunk, 0, bytesRead);
        }

        byte[] rawBytes = buffer.toByteArray();

        String signatureHeader = request.getHeader(SIGNATURE_HEADER);
        if (signatureHeader == null || signatureHeader.isBlank()) {
            sendProblemResponse(
                    response,
                    HttpStatus.UNAUTHORIZED,
                    "Missing Webhook Signature",
                    "Header " + SIGNATURE_HEADER + " is required for webhook authentication.",
                    "MISSING_WEBHOOK_SIGNATURE",
                    request.getRequestURI()
            );
            return;
        }

        if (!signatureHeader.startsWith("sha256=")) {
            sendProblemResponse(
                    response,
                    HttpStatus.UNAUTHORIZED,
                    "Invalid Webhook Signature",
                    "Header " + SIGNATURE_HEADER + " must start with 'sha256=' prefix.",
                    "INVALID_WEBHOOK_SIGNATURE",
                    request.getRequestURI()
            );
            return;
        }

        String secret = properties.getWebhook().getSecret();
        if (secret == null || secret.isBlank()) {
            secret = "mock_webhook_secret";
        }

        boolean isValid = verifyHmacSignature(rawBytes, secret, signatureHeader);
        if (!isValid) {
            sendProblemResponse(
                    response,
                    HttpStatus.UNAUTHORIZED,
                    "Invalid Webhook Signature",
                    "Computed HMAC-SHA256 signature does not match header " + SIGNATURE_HEADER + ".",
                    "INVALID_WEBHOOK_SIGNATURE",
                    request.getRequestURI()
            );
            return;
        }

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request, rawBytes);
        filterChain.doFilter(cachedRequest, response);
    }

    public static boolean verifyHmacSignature(byte[] payload, String secret, String headerSignature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(payload);
            String computedSignature = "sha256=" + HexFormat.of().formatHex(hmacBytes);

            return MessageDigest.isEqual(
                    computedSignature.getBytes(StandardCharsets.UTF_8),
                    headerSignature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception ex) {
            return false;
        }
    }

    private void sendProblemResponse(
            HttpServletResponse response,
            HttpStatus status,
            String title,
            String detail,
            String code,
            String uri
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        ApiErrorResponse error = ApiErrorResponse.of(status.value(), title, detail, code, uri);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
