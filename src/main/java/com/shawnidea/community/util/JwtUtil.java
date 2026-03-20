package com.shawnidea.community.util;

import com.shawnidea.community.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class JwtUtil {

    private static final String REDIS_REFRESH_PREFIX = "refresh_token:";

    private final SecretKey secretKey;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;
    private final RedisTemplate<String, Object> redisTemplate;

    public JwtUtil(
            @Value("${community.jwt.secret:default-dev-secret-key-change-in-production-at-least-32-chars}") String secret,
            @Value("${community.jwt.access-expiration-ms:900000}") long accessExpirationMs,
            @Value("${community.jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs,
            RedisTemplate<String, Object> redisTemplate) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
        this.redisTemplate = redisTemplate;
    }

    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessExpirationMs);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("type", user.getType())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(User user) {
        String token = UUID.randomUUID().toString();
        String redisKey = REDIS_REFRESH_PREFIX + token;
        redisTemplate.opsForValue().set(redisKey, user.getId(), refreshExpirationMs, TimeUnit.MILLISECONDS);
        return token;
    }

    public Claims validateAccessToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public Integer validateRefreshToken(String refreshToken) {
        String redisKey = REDIS_REFRESH_PREFIX + refreshToken;
        Object userId = redisTemplate.opsForValue().get(redisKey);
        return userId != null ? (Integer) userId : null;
    }

    public void revokeRefreshToken(String refreshToken) {
        String redisKey = REDIS_REFRESH_PREFIX + refreshToken;
        redisTemplate.delete(redisKey);
    }

}
