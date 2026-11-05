package com.ecommerce.auth_service.security;

import com.ecommerce.auth_service.dto.GitHubEmail;
import com.ecommerce.auth_service.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final AccountService accountService;
    private final RestClient restClient;

    @Override
    public @NonNull OAuth2User loadUser(@NonNull OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        String token = userRequest.getAccessToken().getTokenValue();

        Map<String, Object> attributes = fetchUserInfo(token);

        String providerId = String.valueOf(Objects.requireNonNull(attributes.get("id")));
        String email = (String) attributes.get("email");
        if (email == null) {
            email = fetchPrimaryEmail(token)
                    .orElseThrow(() -> {
                        log.debug("GitHub email is not available");
                        return new OAuth2AuthenticationException("GitHub email is not available");
                    });
        }
        accountService.provisionOAuthAccount("github", providerId, email, true);
        Map<String, Object> safeAttrs = new HashMap<>(attributes);
        safeAttrs.put("id", providerId);

        log.info("CustomOAuth2UserService: returning GitHubOAuth2User with id={}", safeAttrs.get("id")); //delete
        return new GitHubOAuth2User(safeAttrs);
    }

    private Map<String, Object> fetchUserInfo(String token) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri("https://api.github.com/user")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header(HttpHeaders.USER_AGENT, "auth-service")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (response == null) {
                throw new OAuth2AuthenticationException("GitHub user info is empty");
            }
            return response;
        } catch (RestClientResponseException e) {
            log.warn("GitHub /user failed: status {}", e.getStatusCode());
            throw new OAuth2AuthenticationException("Failed to fetch GitHub user info");
        } catch (RestClientException e) {
            log.warn("GitHub /user failed: {}", e.getMessage());
            throw new OAuth2AuthenticationException("Failed to fetch GitHub user info");
        }
    }

    private Optional<String> fetchPrimaryEmail(String accessToken) {
        try {
            GitHubEmail[] emails = restClient.get()
                    .uri("https://api.github.com/user/emails")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.USER_AGENT, "auth-service")
                    .retrieve()
                    .body(GitHubEmail[].class);
            if (emails == null) {
                return Optional.empty();
            }

            return Arrays.stream(emails)
                    .filter(e -> e.primary() && e.verified())
                    .map(GitHubEmail::email)
                    .findFirst();

        } catch (RestClientResponseException e) {
            log.warn("GitHub /user/emails failed: status {}", e.getStatusCode());
            throw new OAuth2AuthenticationException("Failed to fetch GitHub emails");
        } catch (RestClientException e) {
            log.warn("GitHub /user/emails failed: {}", e.getMessage());
            throw new OAuth2AuthenticationException("Failed to fetch GitHub emails");
        }
    }
}
