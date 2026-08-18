package student.ed.gtalent_spring_boot_260801.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import student.ed.gtalent_spring_boot_260801.constant.AuthOwnerTypes;
import student.ed.gtalent_spring_boot_260801.constant.ResponseMessages;
import student.ed.gtalent_spring_boot_260801.entity.AuthToken;
import student.ed.gtalent_spring_boot_260801.entity.Member;
import student.ed.gtalent_spring_boot_260801.exception.MemberAccountExcption;
import student.ed.gtalent_spring_boot_260801.exception.ResourceNotFoundException;
import student.ed.gtalent_spring_boot_260801.repository.AuthTokenRepository;
import student.ed.gtalent_spring_boot_260801.repository.MemberRepository;
import student.ed.gtalent_spring_boot_260801.request.MemberLoginRequest;
import student.ed.gtalent_spring_boot_260801.request.MemberPasswordUpdateRequest;
import student.ed.gtalent_spring_boot_260801.request.MemberProfileUpdateRequest;
import student.ed.gtalent_spring_boot_260801.request.MemberRegisterRequest;
import student.ed.gtalent_spring_boot_260801.response.TokenResponse;

@Service
public class MemberService {

    private static final Byte STATUS_ACTIVE = 1;
    private static final Byte STATUS_DELETED = 0;
    private static final Byte TOKEN_REVOKED = 1;
    private static final DateTimeFormatter DELETED_ACCOUNT_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final MemberRepository repository;
    private final AuthTokenRepository authTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public MemberService(
            MemberRepository repository,
            AuthTokenRepository authTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.repository = repository;
        this.authTokenRepository = authTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public Member findOneById(Long id) {
        return findActiveMember(id);
    }

    @Transactional
    public Member register(MemberRegisterRequest request) {
        validateConfirmPassword(request.getPassword(), request.getConfirmPassword());

        String account = request.getAccount().trim();
        if (repository.countByAccount(account) > 0) {
            throw new MemberAccountExcption("account", ResponseMessages.MEMBER_ACCOUNT_EXISTS);
        }

        Member member = new Member(
                request.getName().trim(),
                request.getGender(),
                account,
                normalizeEmail(request.getEmail()),
                passwordEncoder.encode(request.getPassword())
        );

        try {
            return repository.save(member);
        } catch (RuntimeException exception) {
            throw new DataIntegrityViolationException(
                    ResponseMessages.getMessage(ResponseMessages.DATABASE_WRITE_FAILED),
                    exception
            );
        }
    }

    @Transactional
    public TokenResponse login(MemberLoginRequest request) {
        String account = request.getAccount().trim();
        Member member = repository.findOneByAccountAndStatus(account, STATUS_ACTIVE)
                .orElseThrow(() -> new MemberAccountExcption("account", ResponseMessages.MEMBER_LOGIN_FAILED));

        // BCrypt 每次 encode 都會產生不同 hash，所以登入時必須用 matches 比對。
        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new MemberAccountExcption("password", ResponseMessages.MEMBER_LOGIN_FAILED);
        }

        return createAndSaveToken(AuthOwnerTypes.MEMBER, member.getId());
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        // refresh token 還有效時，撤銷舊紀錄並建立一組新 access/refresh token。
        Claims claims = parseRefreshToken(refreshToken);
        String ownerType = claims.get("ownerType", String.class);
        String tokenType = claims.get("tokenType", String.class);

        if (!AuthOwnerTypes.MEMBER.equals(ownerType) || !"refresh".equals(tokenType)) {
            throw new MemberAccountExcption("refreshToken", ResponseMessages.TOKEN_INVALID);
        }

        String refreshTokenHash = jwtService.hashToken(refreshToken);
        AuthToken authToken = authTokenRepository
                .findActiveByRefreshTokenHashAndOwnerType(refreshTokenHash, AuthOwnerTypes.MEMBER)
                .orElseThrow(() -> new MemberAccountExcption("refreshToken", ResponseMessages.TOKEN_INVALID));

        if (authToken.getRefreshExpiresAt().isBefore(LocalDateTime.now())) {
            authToken.setRevoked(TOKEN_REVOKED);
            throw new MemberAccountExcption("refreshToken", ResponseMessages.TOKEN_EXPIRED);
        }

        authToken.setRevoked(TOKEN_REVOKED);
        authToken.setDeletedAt(LocalDateTime.now());

        return createAndSaveToken(AuthOwnerTypes.MEMBER, authToken.getOwnerId());
    }

    @Transactional
    public void logout(String refreshToken) {
        // logout 採用軟撤銷，讓同一顆 refresh token 之後不能再換 token。
        String refreshTokenHash = jwtService.hashToken(refreshToken);
        authTokenRepository
                .findActiveByRefreshTokenHashAndOwnerType(refreshTokenHash, AuthOwnerTypes.MEMBER)
                .ifPresent(authToken -> {
                    authToken.setRevoked(TOKEN_REVOKED);
                    authToken.setDeletedAt(LocalDateTime.now());
                });
    }

    @Transactional
    public Member updateProfile(Long id, MemberProfileUpdateRequest request) {
        Member member = findActiveMember(id);

        // profile 採部分更新：request 有帶的欄位才更新，沒帶就保留原值。
        if (request.getName() != null) {
            if (request.getName().isBlank()) {
                throw new MemberAccountExcption("name", ResponseMessages.MEMBER_NAME_REQUIRED);
            }

            member.setName(request.getName().trim());
        }

        if (request.getGender() != null) {
            member.setGender(request.getGender());
        }

        if (request.getEmail() != null) {
            member.setEmail(normalizeEmail(request.getEmail()));
        }

        return member;
    }

    @Transactional
    public void updatePassword(Long id, MemberPasswordUpdateRequest request) {
        validateConfirmPassword(request.getPassword(), request.getConfirmPassword());
        Member member = findActiveMember(id);

        member.setPassword(passwordEncoder.encode(request.getPassword()));
    }

    @Transactional
    public void delete(Long id) {
        Member member = findActiveMember(id);
        LocalDateTime now = LocalDateTime.now();

        // 軟刪除時同步改 account，釋放原 account 給新註冊使用。
        member.setStatus(STATUS_DELETED);
        member.setDeletedAt(now);
        member.setAccount("del_" + now.format(DELETED_ACCOUNT_TIMESTAMP_FORMAT) + "_" + member.getAccount());
    }

    public long getRefreshTokenSeconds() {
        return jwtService.getRefreshTokenSeconds();
    }

    private void validateConfirmPassword(String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new MemberAccountExcption("confirmPassword", ResponseMessages.MEMBER_CONFIRM_PASSWORD_NOT_MATCH);
        }
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        return email.trim();
    }

    private Member findActiveMember(Long id) {
        return repository.findOneByIdAndStatus(id, STATUS_ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("member", ResponseMessages.MEMBER_NOT_FOUND));
    }

    private TokenResponse createAndSaveToken(String ownerType, Long ownerId) {
        // 發 token 後把 hash 與過期時間存 MySQL，供 logout / refresh rotation / token 檢查使用。
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

    private Claims parseRefreshToken(String refreshToken) {
        try {
            return jwtService.parse(refreshToken);
        } catch (ExpiredJwtException exception) {
            throw new MemberAccountExcption("refreshToken", ResponseMessages.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new MemberAccountExcption("refreshToken", ResponseMessages.TOKEN_INVALID);
        }
    }

}
