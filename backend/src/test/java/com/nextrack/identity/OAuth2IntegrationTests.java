package com.nextrack.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextrack.identity.api.dto.request.RegisterRequest;
import com.nextrack.identity.api.dto.response.AuthResponse;
import com.nextrack.identity.application.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.nextrack.identity.application.oauth2.info.GithubOAuth2UserInfo;
import com.nextrack.identity.application.oauth2.info.GoogleOAuth2UserInfo;
import com.nextrack.identity.application.service.OAuth2AuthService;
import com.nextrack.identity.domain.entity.OAuthProvider;
import com.nextrack.identity.domain.entity.User;
import com.nextrack.identity.domain.entity.UserOAuthIdentity;
import com.nextrack.identity.domain.repository.UserOAuthIdentityRepository;
import com.nextrack.identity.domain.repository.UserRepository;
import com.nextrack.workspace.domain.entity.Workspace;
import com.nextrack.workspace.domain.repository.WorkspaceMembershipRepository;
import com.nextrack.workspace.domain.repository.WorkspaceRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class OAuth2IntegrationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("nextrack_oauth_test")
            .withUsername("oauth_user")
            .withPassword("oauth_pass");

    @DynamicPropertySource
    static void configureProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OAuth2AuthService oAuth2AuthService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserOAuthIdentityRepository oauthIdentityRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMembershipRepository workspaceMembershipRepository;

    @Autowired
    private HttpCookieOAuth2AuthorizationRequestRepository cookieRepository;

    @Test
    void shouldEncryptAndDecryptOAuth2StateInStatelessHttpOnlyCookie() {
        OAuth2AuthorizationRequest originalRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://github.com/login/oauth/authorize")
                .clientId("test-github-client-id")
                .redirectUri("http://localhost:8080/login/oauth2/code/github")
                .state("secureRandomAntiCsrfStateToken123")
                .attributes(Map.of("registration_id", "github"))
                .build();

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // 1. Save authorization request into cookie
        cookieRepository.saveAuthorizationRequest(originalRequest, request, response);

        Cookie cookie = response.getCookie(HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getValue()).isNotEmpty();
        // Value must be encrypted and Base64-encoded, not raw plaintext state
        assertThat(cookie.getValue()).doesNotContain("secureRandomAntiCsrfStateToken123");

        // 2. Load authorization request back using simulated incoming request with cookie
        MockHttpServletRequest incomingRequest = new MockHttpServletRequest();
        incomingRequest.setCookies(cookie);

        OAuth2AuthorizationRequest decryptedRequest = cookieRepository.loadAuthorizationRequest(incomingRequest);
        assertThat(decryptedRequest).isNotNull();
        assertThat(decryptedRequest.getState()).isEqualTo("secureRandomAntiCsrfStateToken123");
        assertThat(decryptedRequest.getClientId()).isEqualTo("test-github-client-id");
        assertThat(decryptedRequest.getRedirectUri()).isEqualTo("http://localhost:8080/login/oauth2/code/github");

        // 3. Remove authorization request
        MockHttpServletResponse removeResponse = new MockHttpServletResponse();
        OAuth2AuthorizationRequest removedRequest = cookieRepository.removeAuthorizationRequest(incomingRequest, removeResponse);
        assertThat(removedRequest).isNotNull();
        assertThat(removedRequest.getState()).isEqualTo("secureRandomAntiCsrfStateToken123");

        Cookie deleteCookie = removeResponse.getCookie(HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        assertThat(deleteCookie).isNotNull();
        assertThat(deleteCookie.getMaxAge()).isEqualTo(0);
    }

    @Test
    void shouldJitProvisionUserAndPersonalWorkspaceOnFirstGoogleLogin() {
        Map<String, Object> googleAttributes = new HashMap<>();
        googleAttributes.put("sub", "google-sub-987654321");
        googleAttributes.put("email", "grace.hopper@example.com");
        googleAttributes.put("name", "Grace Hopper");
        googleAttributes.put("picture", "https://lh3.googleusercontent.com/photo.jpg");
        googleAttributes.put("email_verified", true);

        GoogleOAuth2UserInfo googleUserInfo = new GoogleOAuth2UserInfo(googleAttributes);

        // Process JIT Login
        User provisionedUser = oAuth2AuthService.processOAuth2Login(googleUserInfo);

        assertThat(provisionedUser).isNotNull();
        assertThat(provisionedUser.getEmail()).isEqualTo("grace.hopper@example.com");
        assertThat(provisionedUser.getDisplayName()).isEqualTo("Grace Hopper");
        assertThat(provisionedUser.getAvatarUrl()).isEqualTo("https://lh3.googleusercontent.com/photo.jpg");
        assertThat(provisionedUser.getPasswordHash()).isNull(); // OAuth users have null password
        assertThat(provisionedUser.hasPassword()).isFalse();

        // Verify user_oauth_identities link
        Optional<UserOAuthIdentity> identity = oauthIdentityRepository
                .findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "google-sub-987654321");
        assertThat(identity).isPresent();
        assertThat(identity.get().getUserId()).isEqualTo(provisionedUser.getId());
        assertThat(identity.get().getProviderEmail()).isEqualTo("grace.hopper@example.com");

        // Verify Personal Workspace auto-creation
        List<Workspace> workspaces = workspaceRepository.findAll().stream()
                .filter(w -> provisionedUser.getId().equals(w.getOwnerUserId()))
                .toList();

        assertThat(workspaces).hasSize(1);
        Workspace personalWs = workspaces.getFirst();
        assertThat(personalWs.getName()).isEqualTo("Grace Hopper's Team");
        assertThat(personalWs.getSlug()).startsWith("grace-hopper-");
        assertThat(workspaceMembershipRepository.existsByWorkspaceIdAndUserId(personalWs.getId(), provisionedUser.getId())).isTrue();
    }

    @Test
    void shouldAutoLinkVerifiedGitHubIdentityToExistingPasswordUser() throws Exception {
        // 1. Register traditional email/password user
        RegisterRequest registerReq = new RegisterRequest(
                "linus.torvalds@example.com",
                "Linus Torvalds",
                "GitSecretPassword123!"
        );

        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse auth = objectMapper.readValue(regResult.getResponse().getContentAsString(), AuthResponse.class);
        String token = "Bearer " + auth.accessToken();

        // 2. Simulate GitHub login with matching verified email
        Map<String, Object> githubAttributes = new HashMap<>();
        githubAttributes.put("id", 1024);
        githubAttributes.put("login", "torvalds");
        githubAttributes.put("name", "Linus B. Torvalds");
        githubAttributes.put("avatar_url", "https://avatars.githubusercontent.com/u/1024");
        githubAttributes.put("email", null); // Simulate private email on profile

        GithubOAuth2UserInfo githubUserInfo = new GithubOAuth2UserInfo(githubAttributes, "linus.torvalds@example.com");

        User linkedUser = oAuth2AuthService.processOAuth2Login(githubUserInfo);

        assertThat(linkedUser.getId()).isEqualTo(auth.user().id());
        assertThat(linkedUser.hasPassword()).isTrue(); // Retains existing password
        assertThat(linkedUser.getAvatarUrl()).isEqualTo("https://avatars.githubusercontent.com/u/1024");

        // 3. Query linked identities endpoint
        mockMvc.perform(get("/api/v1/auth/oauth2/identities")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].provider").value("GITHUB"))
                .andExpect(jsonPath("$[0].providerUserId").value("1024"));

        // 4. Unlink GitHub identity
        mockMvc.perform(delete("/api/v1/auth/oauth2/identities/GITHUB")
                        .header("Authorization", token))
                .andExpect(status().isNoContent());

        assertThat(oauthIdentityRepository.findByUserId(auth.user().id())).isEmpty();
    }

    @Test
    void shouldRejectAccountTakeoverWhenExternalEmailIsUnverified() {
        // Create legitimate user
        User legitimateUser = new User("ceo@enterprise.com", "CEO", "SecurePass123!");
        userRepository.save(legitimateUser);

        // Attacker attempts to login with matching unverified email on Google
        Map<String, Object> spoofedAttributes = new HashMap<>();
        spoofedAttributes.put("sub", "attacker-sub-111");
        spoofedAttributes.put("email", "ceo@enterprise.com");
        spoofedAttributes.put("name", "Attacker");
        spoofedAttributes.put("email_verified", false); // Unverified!

        GoogleOAuth2UserInfo unverifiedUserInfo = new GoogleOAuth2UserInfo(spoofedAttributes);

        // Must throw IllegalStateException preventing account hijacking
        assertThatThrownBy(() -> oAuth2AuthService.processOAuth2Login(unverifiedUserInfo))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot auto-link account: Email 'ceo@enterprise.com' is not verified");

        // Verify attacker identity was NOT linked
        assertThat(oauthIdentityRepository.findByProviderAndProviderUserId(OAuthProvider.GOOGLE, "attacker-sub-111"))
                .isEmpty();
    }

    @Test
    void shouldPreventUnlinkingSoleIdentityForPasswordlessUser() {
        // Create user with Google OAuth only (no password)
        Map<String, Object> googleAttributes = new HashMap<>();
        googleAttributes.put("sub", "solo-google-user-123");
        googleAttributes.put("email", "solo.dev@example.com");
        googleAttributes.put("name", "Solo Dev");
        googleAttributes.put("email_verified", true);

        User soloUser = oAuth2AuthService.processOAuth2Login(new GoogleOAuth2UserInfo(googleAttributes));
        assertThat(soloUser.hasPassword()).isFalse();

        // Attempting to unlink sole identity must fail
        assertThatThrownBy(() -> oAuth2AuthService.unlinkIdentity(soloUser.getId(), OAuthProvider.GOOGLE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot unlink sole authentication identity");
    }
}
