package com.nextrack.identity.application.oauth2;

import com.nextrack.identity.application.oauth2.info.OAuth2UserInfo;
import com.nextrack.identity.application.oauth2.info.OAuth2UserInfoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.modulith.NamedInterface;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@NamedInterface
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);
    private static final String GITHUB_EMAILS_URL = "https://api.github.com/user/emails";

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        String verifiedEmail = null;
        if ("github".equalsIgnoreCase(registrationId)) {
            verifiedEmail = fetchPrimaryGitHubEmail(userRequest.getAccessToken().getTokenValue(), oauth2User);
        }

        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                registrationId,
                oauth2User.getAttributes(),
                verifiedEmail
        );

        return new CustomOAuth2User(userInfo, oauth2User.getAttributes());
    }

    private String fetchPrimaryGitHubEmail(String accessToken, OAuth2User oauth2User) {
        Object directEmail = oauth2User.getAttributes().get("email");
        if (directEmail != null && !((String) directEmail).isBlank()) {
            return (String) directEmail;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.set("Accept", "application/vnd.github.v3+json");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    GITHUB_EMAILS_URL,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            List<Map<String, Object>> emails = response.getBody();
            if (emails != null) {
                // Look for primary and verified email
                for (Map<String, Object> emailObj : emails) {
                    Boolean primary = (Boolean) emailObj.get("primary");
                    Boolean verified = (Boolean) emailObj.get("verified");
                    String email = (String) emailObj.get("email");

                    if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified) && email != null) {
                        return email;
                    }
                }
                // Fallback to any verified email
                for (Map<String, Object> emailObj : emails) {
                    Boolean verified = (Boolean) emailObj.get("verified");
                    String email = (String) emailObj.get("email");
                    if (Boolean.TRUE.equals(verified) && email != null) {
                        return email;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch private GitHub emails: {}", e.getMessage());
        }

        return null;
    }
}
