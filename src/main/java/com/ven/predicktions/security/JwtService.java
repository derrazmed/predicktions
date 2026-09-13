package com.ven.predicktions.security;

import com.ven.predicktions.config.JwtProperties;
import com.ven.predicktions.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(
                jwtProperties.secret().getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateToken(UUID userId) {
        return generateToken(userId, Role.USER);
    }

    public String generateToken(UUID userId, Role role) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(jwtProperties.expiration());

        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(signingKey)
                .compact();
    }

    public UUID extractUserId(String token) {
        Claims claims = parseToken(token);

        return UUID.fromString(claims.getSubject());
    }

    public Role extractRole(String token) {
        Claims claims = parseToken(token);
        String role = claims.get("role", String.class);

        return role == null ? Role.USER : Role.valueOf(role);
    }

    public boolean isValid(String token) {
        try {
            parseToken(token);
            extractUserId(token);
            extractRole(token);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpiration() {
        return jwtProperties.expiration();
    }
}