package com.ecommerce.auth_service.security;

import com.ecommerce.auth_service.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {
    private final AccountService accountService;

    @Override
    public OidcUser loadUser(@NonNull OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        String provider = userRequest.getClientRegistration().getRegistrationId();
        OidcUser oidcUser = super.loadUser(userRequest);
        String email = oidcUser.getEmail();
        String providerId = oidcUser.getName();
        boolean emailVerified = Boolean.TRUE.equals(oidcUser.getEmailVerified());
        accountService.provisionOAuthAccount(provider, providerId, email, emailVerified);
        return oidcUser;
    }
}
