package com.nextrack.identity.application.oauth2.info;

import com.nextrack.identity.domain.entity.OAuthProvider;

import java.util.Map;

public final class OAuth2UserInfoFactory {

    private OAuth2UserInfoFactory() {}

    public static OAuth2UserInfo getOAuth2UserInfo(
            String registrationId,
            Map<String, Object> attributes,
            String verifiedEmail
    ) {
        if (OAuthProvider.GOOGLE.name().equalsIgnoreCase(registrationId)) {
            return new GoogleOAuth2UserInfo(attributes);
        } else if (OAuthProvider.GITHUB.name().equalsIgnoreCase(registrationId)) {
            return new GithubOAuth2UserInfo(attributes, verifiedEmail);
        } else {
            throw new IllegalArgumentException("Unsupported OAuth2 identity provider: " + registrationId);
        }
    }
}
