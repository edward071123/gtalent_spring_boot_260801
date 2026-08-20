package student.ed.gtalent_spring_boot_260801.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    private final SecretKey secretKey;
    private final long accessTokenSeconds;
    private final long refreshTokenSeconds;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-seconds}") long accessTokenSeconds,
            @Value("${jwt.refresh-token-seconds}") long refreshTokenSeconds) {
        // JJWT 的 HMAC key 長度要足夠；正式環境請用 JWT_SECRET 覆蓋預設值。
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenSeconds = accessTokenSeconds;
        this.refreshTokenSeconds = refreshTokenSeconds;
    }

    public String generateAccessToken(String ownerType, Long ownerId) {
        return generateToken(ownerType, ownerId, "access", getAccessExpiresAt());
    }

    public String generateAccessToken(String ownerType, Long ownerId, LocalDateTime expiresAt) {
        return generateToken(ownerType, ownerId, "access", expiresAt);
    }

    public String generateRefreshToken(String ownerType, Long ownerId) {
        return generateToken(ownerType, ownerId, "refresh", getRefreshExpiresAt());
    }

    public String generateRefreshToken(String ownerType, Long ownerId, LocalDateTime expiresAt) {
        return generateToken(ownerType, ownerId, "refresh", expiresAt);
    }

    public Claims parse(String token) {
        // 解析時會同時驗證簽章與 exp，token 過期會丟 ExpiredJwtException。
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String hashToken(String token) {
        // DB 只存 token hash，不存原始 JWT，降低 token 外洩風險。
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();

            for (byte value : encoded) {
                hex.append(String.format("%02x", value));
            }

            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public LocalDateTime getAccessExpiresAt() {
        return LocalDateTime.now().plusSeconds(accessTokenSeconds);
    }

    public LocalDateTime getRefreshExpiresAt() {
        return LocalDateTime.now().plusSeconds(refreshTokenSeconds);
    }

    public long getAccessTokenSeconds() {
        return accessTokenSeconds;
    }

    public long getRefreshTokenSeconds() {
        return refreshTokenSeconds;
    }

    public LocalDateTime getExpiresAt(Claims claims) {
        return LocalDateTime.ofInstant(claims.getExpiration().toInstant(), ZONE_ID);
    }

    private String generateToken(String ownerType, Long ownerId, String tokenType, LocalDateTime expiresAt) {
        // ownerType 用來支援 MEMBER / ADMIN 各自登入；tokenType 用來區分 access / refresh。
        return Jwts.builder()
                .subject(String.valueOf(ownerId))
                .claim("ownerType", ownerType)
                .claim("tokenType", tokenType)
                .issuedAt(new Date())
                .expiration(toDate(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZONE_ID).toInstant());
    }
}