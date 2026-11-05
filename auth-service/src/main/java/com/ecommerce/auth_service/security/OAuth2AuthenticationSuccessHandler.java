package com.ecommerce.auth_service.security;

import com.ecommerce.auth_service.dto.AccountDto;
import com.ecommerce.auth_service.dto.AuthResponse;
import com.ecommerce.auth_service.service.AccountService;
import com.ecommerce.auth_service.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final ObjectMapper objectMapper;
    private final AccountService accountService;
    private final AuthService authService;

    @Override
    public void onAuthenticationSuccess(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, Authentication authentication) throws IOException {
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            handleLogin(oidcUser.getEmail(), response);
        } else if (authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
            Object idObj = oAuth2User.getAttribute("id");

            log.info("Handler: principal class = {}, id = {}", oAuth2User.getClass().getName(), oAuth2User.getAttribute("id")); //delete

            String providerId = idObj.toString();
            handleLoginByProviderId("github", providerId, response);
        } else {
            log.warn("Unsupported OAuth2 principal");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getWriter(), Map.of("message", "OAuth2 provider not supported"));
        }
    }

    private void handleLogin(String email, HttpServletResponse response) throws IOException {
        if (email == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getWriter(), Map.of("message", "email is null"));
            return;
        }
        AccountDto accountDto = accountService.findAccountByEmail(email).orElseThrow(() -> {
            log.debug("Account not found for email");
            return new OAuth2AuthenticationException("Account not found");
        });
        buildAuthResponse(response, accountDto);
    }

    private void handleLoginByProviderId(String provider, String providerId, HttpServletResponse response) throws IOException {
        AccountDto accountDto = accountService.findByOauth2ProviderAndOauth2ProviderId(provider, providerId)
                .orElseThrow(() -> new OAuth2AuthenticationException("Account not found"));
        buildAuthResponse(response, accountDto);
    }

    private void buildAuthResponse(HttpServletResponse response, AccountDto accountDto) throws IOException {
        if (accountDto.blocked()) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getWriter(), Map.of("message", "Account is blocked"));
            return;
        }
        AuthResponse authResponse = authService.oauthLogin(accountDto);
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(response.getWriter(), authResponse);
    }
}
