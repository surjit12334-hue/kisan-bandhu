package com.kisanbandhu.security;

import com.kisanbandhu.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlMillis;

    public JwtService(@Value("${kisan.jwt.secret}") String secret,
                      @Value("${kisan.jwt.ttl-hours}") long ttlHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMillis = ttlHours * 3600_000L;
    }

    public String issue(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getPhone())
                .claims(Map.of(
                        "uid", user.getId(),
                        "name", user.getName(),
                        "role", user.getRole().name(),
                        "lang", user.getLanguage()
                ))
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMillis))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }
}
