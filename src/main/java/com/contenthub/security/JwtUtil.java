package com.contenthub.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final Key signingKey;
    private final long userExpirationMs;
    private final long adminExpirationMs;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.user-expiration-ms}") long userExpirationMs,
            @Value("${app.jwt.admin-expiration-ms}") long adminExpirationMs
    ) {
        // Pad the secret if it's short, so HS256 always gets a valid key length
        String padded = secret.length() < 32
                ? String.format("%-32s", secret).replace(' ', '0')
                : secret;
        this.signingKey = Keys.hmacShaKeyFor(padded.getBytes(StandardCharsets.UTF_8));
        this.userExpirationMs = userExpirationMs;
        this.adminExpirationMs = adminExpirationMs;
    }

    public String generateUserToken(Long userId, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + userExpirationMs);
        return Jwts.builder()
                .setSubject(email)
                .claim("role", "USER")
                .claim("userId", userId)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateAdminToken(String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + adminExpirationMs);
        return Jwts.builder()
                .setSubject(email)
                .claim("role", "ADMIN")
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
