package student.ed.gtalent_spring_boot_260801.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import student.ed.gtalent_spring_boot_260801.constant.ResponseMessages;
import student.ed.gtalent_spring_boot_260801.exception.BusinessException;

@Service
public class JwtService {

    private final SecretKey secretKey;

    private final long expirationMinutes;

    // 簡易登出黑名單。
    // key 是 token，value 是 token 過期秒數。
    // 注意：這是記憶體資料，應用程式重啟後會消失；正式環境通常會放 Redis 或資料庫。
    private final Map<String, Long> revokedTokens = new ConcurrentHashMap<>();

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-minutes}") long expirationMinutes) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public JwtToken createToken(Long memberId) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(expirationMinutes * 60);

        String token = Jwts.builder()
                .subject(String.valueOf(memberId))
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();

        return new JwtToken(token, toLocalDateTime(expiresAt));
    }

    public Long getMemberId(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        validateTokenNotRevoked(token);

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Long.valueOf(claims.getSubject());
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BusinessException(ResponseMessages.UNAUTHORIZED);
        }
    }

    public void revoke(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            revokedTokens.put(token, claims.getExpiration().toInstant().getEpochSecond());
            cleanupExpiredRevokedTokens();
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BusinessException(ResponseMessages.UNAUTHORIZED);
        }
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new BusinessException(ResponseMessages.UNAUTHORIZED);
        }

        return authorizationHeader.substring("Bearer ".length()).trim();
    }

    private void validateTokenNotRevoked(String token) {
        cleanupExpiredRevokedTokens();

        if (revokedTokens.containsKey(token)) {
            throw new BusinessException(ResponseMessages.UNAUTHORIZED);
        }
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private void cleanupExpiredRevokedTokens() {
        long now = Instant.now().getEpochSecond();
        revokedTokens.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    public static class JwtToken {

        private final String token;

        private final LocalDateTime expiresAt;

        public JwtToken(String token, LocalDateTime expiresAt) {
            this.token = token;
            this.expiresAt = expiresAt;
        }

        public String getToken() {
            return this.token;
        }

        public LocalDateTime getExpiresAt() {
            return this.expiresAt;
        }

    }

}
