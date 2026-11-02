package com.ecommerce.auth_service;

import com.ecommerce.auth_service.dto.AccountDto;
import com.ecommerce.auth_service.model.Role;
import com.ecommerce.auth_service.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class JwtServiceTest {
    private static final String SECRET = "3LcrJLahvC/6nBTaIm8YSONJmSw4lRX6X7/mc6aBbHg=";
    private static final Duration EXPIRATION = Duration.ofMinutes(15);
    private JwtService jwtService;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION);
        secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
    }

    private AccountDto account() {

        return new AccountDto(1L, "test@mail.com", Role.ROLE_USER, false, true);
    }

    @Test
    void generateTokenTest() {
        String token = jwtService.generateToken(account());

        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo("test@mail.com");
        assertThat(claims.get("id", Long.class)).isEqualTo(1L);
        assertThat(claims.get("role", String.class)).isEqualTo("ROLE_USER");
        assertThat(claims.getExpiration()).isAfter(new Date());
    }

    @Test
    void extractEmail_shouldReturnEmail() {
        String token = jwtService.generateToken(account());
        assertThat(jwtService.extractEmail(token)).isEqualTo("test@mail.com");
    }

    @Test
    void extractRole_shouldReturnRole() {
        String token = jwtService.generateToken(account());
        assertThat(jwtService.extractRole(token)).isEqualTo("ROLE_USER");

    }

    @Test
    void extractId_shouldReturnId() {
        String token = jwtService.generateToken(account());
        assertThat(jwtService.extractId(token)).isEqualTo(1L);
    }

    @Test
    void isTokenValid_shouldReturnFalseForMalformedToken() {
        assertThat(jwtService.isTokenValid("garbage")).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalseForTokenSignedWithOtherSecret() {
        final String otherSecret = "d7XxKVvw1EWdQ5F96GGhQtkhglnfL88CbBGL2BhdJWY=";
        JwtService otherService = new JwtService(otherSecret, EXPIRATION);
        String token = otherService.generateToken(account());
        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void extractEmail_shouldThrowOnInvalidToken() {
        assertThatThrownBy(() -> jwtService.extractEmail("garbage"))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }


}
