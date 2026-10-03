package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.dto.AccountDto;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;

@Slf4j
@Service
public class JwtService {

    private final SecretKey jwtAccessSecret;
    private final Duration accessExpiration;

    public JwtService(
            @Value("${jwt.secret}") String jwtAccessSecret,
            @Value("${jwt.access.expiration}") Duration accessExpiration) {
        this.jwtAccessSecret = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtAccessSecret));
        this.accessExpiration = accessExpiration;
    }

    public String generateToken(AccountDto account) {
        final Date expiration = new Date(System.currentTimeMillis() + accessExpiration.toMillis());
        return Jwts.builder()
                .subject(account.email())
                .expiration(expiration)
                .signWith(jwtAccessSecret)
                .claim("role", account.role())
                .compact();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException expEx) {
            log.debug("Token expired: {}", expEx.getMessage());
        } catch (JwtException jwtEx) {
            log.warn("Invalid JWT: {}", jwtEx.getMessage());
        } catch (IllegalArgumentException e) {
            log.debug("Token is null or empty: {}", e.getMessage());
        }
        return false;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(jwtAccessSecret)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
