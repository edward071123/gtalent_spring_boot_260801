package student.ed.gtalent_spring_boot_260801.interceptor;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.HandlerInterceptor;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import student.ed.gtalent_spring_boot_260801.constant.AuthOwnerTypes;
import student.ed.gtalent_spring_boot_260801.constant.ResponseMessages;
import student.ed.gtalent_spring_boot_260801.entity.AuthToken;
import student.ed.gtalent_spring_boot_260801.exception.AuthException;
import student.ed.gtalent_spring_boot_260801.repository.AuthTokenRepository;
import student.ed.gtalent_spring_boot_260801.response.TokenResponse;
import student.ed.gtalent_spring_boot_260801.service.JwtService;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    // 目前先保護 /members/{id} 類型的 API，避免拿自己的 token 操作別人的會員資料。
    private static final Pattern MEMBER_ID_PATH_PATTERN = Pattern.compile("^/members/(\\d+)(/.*)?$");
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String REFRESH_TOKEN_HEADER = "X-Refresh-Token";
    private static final Byte TOKEN_REVOKED = 1;

    private final JwtService jwtService;
    private final AuthTokenRepository authTokenRepository;

    public AuthInterceptor(JwtService jwtService, AuthTokenRepository authTokenRepository) {
        this.jwtService = jwtService;
        this.authTokenRepository = authTokenRepository;
    }

    @Override
    @Transactional
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {
        String accessToken = getBearerToken(request);

        try {
            // access token 有效就直接放行。
            Claims accessClaims = jwtService.parse(accessToken);
            validateAccessToken(accessToken, accessClaims, request);
            return true;
        } catch (ExpiredJwtException exception) {
            // access token 過期時，改用 X-Refresh-Token 自動換新 token。
            refreshTokenAndSetHeaders(request, response, exception.getClaims());
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new AuthException("token", ResponseMessages.TOKEN_INVALID);
        }
    }

    private String getBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new AuthException("token", ResponseMessages.TOKEN_REQUIRED);
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            throw new AuthException("token", ResponseMessages.TOKEN_REQUIRED);
        }

        return token;
    }

    private void validateAccessToken(String accessToken, Claims claims, HttpServletRequest request) {
        String ownerType = claims.get("ownerType", String.class);
        String tokenType = claims.get("tokenType", String.class);
        Long ownerId = Long.valueOf(claims.getSubject());

        if (!AuthOwnerTypes.MEMBER.equals(ownerType) || !"access".equals(tokenType)) {
            throw new AuthException("token", ResponseMessages.TOKEN_INVALID);
        }

        // 除了 JWT 本身有效，也要確認 MySQL 中這顆 token 沒被 logout 或 refresh rotation 撤銷。
        Optional<AuthToken> authToken = authTokenRepository.findActiveByAccessTokenHashAndOwnerType(
                jwtService.hashToken(accessToken),
                AuthOwnerTypes.MEMBER
        );

        if (authToken.isEmpty() || authToken.get().getAccessExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AuthException("token", ResponseMessages.TOKEN_EXPIRED);
        }

        validateMemberPathOwner(request, ownerId);
    }

    private void refreshTokenAndSetHeaders(
            HttpServletRequest request,
            HttpServletResponse response,
            Claims expiredAccessClaims) {
        // 自動 refresh 需要前端同時帶 X-Refresh-Token；如果 refresh 也失效，就要求重新登入。
        String refreshToken = getRefreshToken(request);
        if (refreshToken == null) {
            throw new AuthException("token", ResponseMessages.TOKEN_EXPIRED);
        }

        Long ownerId = Long.valueOf(expiredAccessClaims.getSubject());
        validateMemberPathOwner(request, ownerId);

        Claims refreshClaims = parseRefreshToken(refreshToken);
        String ownerType = refreshClaims.get("ownerType", String.class);
        String tokenType = refreshClaims.get("tokenType", String.class);
        Long refreshOwnerId = Long.valueOf(refreshClaims.getSubject());

        if (!AuthOwnerTypes.MEMBER.equals(ownerType)
                || !"refresh".equals(tokenType)
                || !ownerId.equals(refreshOwnerId)) {
            throw new AuthException("refreshToken", ResponseMessages.TOKEN_INVALID);
        }

        AuthToken oldToken = authTokenRepository
                .findActiveByRefreshTokenHashAndOwnerType(jwtService.hashToken(refreshToken), AuthOwnerTypes.MEMBER)
                .orElseThrow(() -> new AuthException("refreshToken", ResponseMessages.TOKEN_INVALID));

        // refresh token rotation：舊 token 用過後立即撤銷，只能使用新 token。
        if (oldToken.getRefreshExpiresAt().isBefore(LocalDateTime.now())) {
            oldToken.setRevoked(TOKEN_REVOKED);
            throw new AuthException("refreshToken", ResponseMessages.TOKEN_EXPIRED);
        }

        oldToken.setRevoked(TOKEN_REVOKED);
        oldToken.setDeletedAt(LocalDateTime.now());

        TokenResponse tokenResponse = createAndSaveToken(AuthOwnerTypes.MEMBER, ownerId);
        // 新 token 放在 response header，前端收到後要更新本地保存的雙令牌。
        response.setHeader("X-New-Access-Token", tokenResponse.getAccessToken());
        response.setHeader("X-New-Refresh-Token", tokenResponse.getRefreshToken());
        response.setHeader("X-New-Access-Expires-At", tokenResponse.getAccessExpiresAt().toString());
        response.setHeader("X-New-Refresh-Expires-At", tokenResponse.getRefreshExpiresAt().toString());
    }

    private Claims parseRefreshToken(String refreshToken) {
        try {
            return jwtService.parse(refreshToken);
        } catch (ExpiredJwtException exception) {
            throw new AuthException("refreshToken", ResponseMessages.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new AuthException("refreshToken", ResponseMessages.TOKEN_INVALID);
        }
    }

    private TokenResponse createAndSaveToken(String ownerType, Long ownerId) {
        LocalDateTime accessExpiresAt = jwtService.getAccessExpiresAt();
        LocalDateTime refreshExpiresAt = jwtService.getRefreshExpiresAt();
        String accessToken = jwtService.generateAccessToken(ownerType, ownerId, accessExpiresAt);
        String refreshToken = jwtService.generateRefreshToken(ownerType, ownerId, refreshExpiresAt);

        AuthToken authToken = new AuthToken(
                ownerType,
                ownerId,
                jwtService.hashToken(accessToken),
                jwtService.hashToken(refreshToken),
                accessExpiresAt,
                refreshExpiresAt
        );

        authTokenRepository.save(authToken);
        return new TokenResponse(accessToken, refreshToken, accessExpiresAt, refreshExpiresAt);
    }

    private String getRefreshToken(HttpServletRequest request) {
        String refreshToken = request.getHeader(REFRESH_TOKEN_HEADER);
        if (refreshToken == null || refreshToken.isBlank()) {
            return null;
        }

        return refreshToken;
    }

    private void validateMemberPathOwner(HttpServletRequest request, Long ownerId) {
        Matcher matcher = MEMBER_ID_PATH_PATTERN.matcher(request.getRequestURI());

        if (matcher.matches() && !ownerId.equals(Long.valueOf(matcher.group(1)))) {
            throw new AuthException("token", ResponseMessages.TOKEN_INVALID);
        }
    }

}