package com.ecommerce.auth_service.config;

import com.ecommerce.auth_service.security.*;
import com.ecommerce.auth_service.service.JwtService;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class SecurityConfig {

    private final String[] publicEndpoints = {
            "/auth/register", "/auth/login", "/auth/refresh",
            "/auth/logout", "/auth/verify-email",
            "/auth/forgot-password", "/auth/reset-password", "/oauth2/**",
            "/login/oauth2/**", "/auth/resend-verification"
    };

    private final String[] systemEndpoints = {"/actuator/health", "/swagger-ui/**", "/swagger-ui.html",
            "/v3/api-docs.yaml", "/v3/api-docs/**", "/error"};

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtService jwtService,
                                           JsonAuthenticationEntryPoint authenticationEntryPoint,
                                           JsonAccessDeniedHandler accessDeniedHandler,
                                           CustomOidcUserService customOidcUserService,
                                           CustomOAuth2UserService customOAuth2UserService,
                                           OAuth2AuthenticationSuccessHandler successHandler,
                                           ProxyManager<String> proxyManager,
                                           ObjectMapper objectMapper) {
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService);
        RateLimitFilter rateLimitFilter = new RateLimitFilter(objectMapper, proxyManager);

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint).accessDeniedHandler(accessDeniedHandler))
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(successHandler)
                        .userInfoEndpoint(u -> u
                                .oidcUserService(customOidcUserService)
                                .userService(customOAuth2UserService)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(publicEndpoints).permitAll()
                        .requestMatchers(systemEndpoints).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, JwtAuthenticationFilter.class)
                .build();
    }
}
