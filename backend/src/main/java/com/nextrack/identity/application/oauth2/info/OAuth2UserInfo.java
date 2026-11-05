package com.nextrack.identity.application.oauth2.info;

import com.nextrack.identity.domain.entity.OAuthProvider;

public interface OAuth2UserInfo {

    OAuthProvider getProvider();

    String getProviderUserId();

    String getEmail();

    boolean isEmailVerified();

    String getDisplayName();

    String getAvatarUrl();
}
