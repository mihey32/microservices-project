package com.ecommerce.auth_service.security;

import com.ecommerce.auth_service.dto.ErrorResponseDto;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {
    private final ObjectMapper objectMapper;
    private final ProxyManager<String> proxyManager;
    private final BucketConfiguration loginBucketConfig;
    private final BucketConfiguration defaultBucketConfig;

    public RateLimitFilter(ObjectMapper objectMapper, ProxyManager<String> proxyManager) {
        this.objectMapper = objectMapper;
        this.proxyManager = proxyManager;

        this.loginBucketConfig = BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(5).refillIntervally(5, Duration.ofMinutes(2)))
                .build();

        this.defaultBucketConfig = BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(60).refillIntervally(60, Duration.ofMinutes(1)))
                .build();
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        HttpServletRequest effectiveRequest = request;
        String key;
        BucketConfiguration config;

        if (isLoginAttempt(request)) {
            CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
            effectiveRequest = cachedRequest;

            String email = extractEmail(cachedRequest);
            key = "rl:login" + request.getRemoteAddr() + ":" + (email != null ? email : "unknown");
            config = loginBucketConfig;
        } else {
            key = "rl:default:" + request.getRemoteAddr() + ":" + request.getRequestURI();
            config = defaultBucketConfig;
        }

        try {
            Bucket bucket = proxyManager.builder().build(key, () -> config);
            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit exceeded: key={}", key);
                writeRateLimitResponse(response);
                return;
            }
        } catch (Exception e) {
            log.error("Rate limit unavailable (Redis issue), allowing request. key={}", key, e);
        }

        filterChain.doFilter(effectiveRequest, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    private boolean isLoginAttempt(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && "/auth/login".equals(request.getRequestURI());
    }

    private String extractEmail(CachedBodyHttpServletRequest request) {
        try {
            JsonNode root = objectMapper.readTree(request.getInputStream());
            JsonNode emailNode = root.get("email");
            return emailNode != null ? emailNode.asText(null) : null;
        } catch (Exception e) {
            log.debug("Failed to extract email from login request for rate limiting", e);
            return null;
        }
    }

    private void writeRateLimitResponse(HttpServletResponse response) throws IOException {
        ErrorResponseDto dto = new ErrorResponseDto(
                "Too many requests",
                "Rate limit exceeded. Try again later.",
                Instant.now()
        );
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), dto);
    }
}
