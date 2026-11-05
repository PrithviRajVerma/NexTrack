package com.nextrack.identity.application.oauth2.info;

import com.nextrack.identity.domain.entity.OAuthProvider;

import java.util.Map;

public class GithubOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;
    private final String verifiedEmail;

    public GithubOAuth2UserInfo(Map<String, Object> attributes, String verifiedEmail) {
        this.attributes = attributes;
        this.verifiedEmail = verifiedEmail;
    }

    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.GITHUB;
    }

    @Override
    public String getProviderUserId() {
        Object id = attributes.get("id");
        return id != null ? String.valueOf(id) : null;
    }

    @Override
    public String getEmail() {
        if (verifiedEmail != null && !verifiedEmail.isBlank()) {
            return verifiedEmail;
        }
        return (String) attributes.get("email");
    }

    @Override
    public boolean isEmailVerified() {
        // If retrieved via /user/emails or direct attribute with confirmed presence
        return verifiedEmail != null || attributes.get("email") != null;
    }

    @Override
    public String getDisplayName() {
        String name = (String) attributes.get("name");
        if (name != null && !name.isBlank()) {
            return name;
        }
        String login = (String) attributes.get("login");
        return login != null && !login.isBlank() ? login : "GitHub User";
    }

    @Override
    public String getAvatarUrl() {
        return (String) attributes.get("avatar_url");
    }
}
