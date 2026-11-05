package com.nextrack.identity.application.oauth2.info;

import com.nextrack.identity.domain.entity.OAuthProvider;

import java.util.Map;

public class GoogleOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public String getProviderUserId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public boolean isEmailVerified() {
        Object verified = attributes.get("email_verified");
        if (verified instanceof Boolean b) {
            return b;
        }
        if (verified instanceof String s) {
            return Boolean.parseBoolean(s);
        }
        return false;
    }

    @Override
    public String getDisplayName() {
        String name = (String) attributes.get("name");
        return name != null && !name.isBlank() ? name : getEmail();
    }

    @Override
    public String getAvatarUrl() {
        return (String) attributes.get("picture");
    }
}
