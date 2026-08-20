package student.ed.gtalent_spring_boot_260801.service;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

import org.springframework.security.crypto.password.PasswordEncoder;

import student.ed.gtalent_spring_boot_260801.constant.AuthOwnerTypes;
import student.ed.gtalent_spring_boot_260801.constant.ResponseMessages;
import student.ed.gtalent_spring_boot_260801.request.MemberLoginRequest;
import student.ed.gtalent_spring_boot_260801.request.MemberPasswordUpdateRequest;
import student.ed.gtalent_spring_boot_260801.request.MemberProfileUpdateRequest;
import student.ed.gtalent_spring_boot_260801.request.MemberRegisterRequest;
import student.ed.gtalent_spring_boot_260801.response.TokenResponse;
import student.ed.gtalent_spring_boot_260801.entity.AuthToken;
import student.ed.gtalent_spring_boot_260801.entity.Member;
import student.ed.gtalent_spring_boot_260801.repository.AuthTokenRepository;
import student.ed.gtalent_spring_boot_260801.repository.MemberRepository;
import student.ed.gtalent_spring_boot_260801.exception.MemberAccountExcption;
import student.ed.gtalent_spring_boot_260801.exception.ResourceNotFoundException;

@Service
public class MemberService {

    private MemberRepository repository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthTokenRepository authTokenRepository;
    private Byte TOKEN_REVOKED = 1;

    public MemberService(
            MemberRepository repository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthTokenRepository authTokenRepository) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authTokenRepository = authTokenRepository;
    }

    public Member findOneById(Long id) {
        // 1. 給repository找
        Optional<Member> member = this.repository.findOneById(id);

        // 2. 如果找不到
        if (member.isEmpty()) {
            throw new ResourceNotFoundException(
                    "member",
                    ResponseMessages.MEMBER_NOT_FOUND);
        }

        // 3. 有找到，就把 Member 拿出來
        return member.get();

    }

    @Transactional
    public Member register(MemberRegisterRequest request) {
        // 比對傳入的密碼跟確認密碼
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new MemberAccountExcption("confirmPassword", ResponseMessages.MEMBER_CONFIRM_PASSWORD_NOT_MATCH);
        }

        // 驗證輸入的帳號是否已存在系統
        // 比對帳戶存在系統的話就要跳出例外
        String account = request.getAccount();
        if (this.repository.countByAccount(account) > 0) {
            throw new MemberAccountExcption("account", ResponseMessages.MEMBER_ACCOUNT_EXISTS);
        }

        Member member = new Member(
                request.getName(),
                request.getGender(),
                request.getAccount(),
                request.getEmail(),
                this.passwordEncoder.encode(request.getPassword()) // 密碼加密
        );

        // 開始新增資料到資料庫
        try {
            this.repository.save(member);
            return member;
        } catch (RuntimeException exception) {
            // 統一丟資料寫入失敗，讓 GlobalExceptionHandler 判斷資料庫細項錯誤。
            throw new DataIntegrityViolationException(
                    ResponseMessages.getMessage(ResponseMessages.DATABASE_WRITE_FAILED),
                    exception);
        }

    }

    @Transactional
    public void updatePassword(Long id, MemberPasswordUpdateRequest request) {
        // 比對傳入的密碼跟確認密碼
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new MemberAccountExcption("confirmPassword", ResponseMessages.MEMBER_CONFIRM_PASSWORD_NOT_MATCH);
        }

        // 1. 給repository找
        Optional<Member> member = this.repository.findOneById(id);

        // 2. 如果找不到
        if (member.isEmpty()) {
            throw new ResourceNotFoundException(
                    "member",
                    ResponseMessages.MEMBER_NOT_FOUND);
        }

        // 3. 有找到，就把 Member 拿出來
        Member targetMember = member.get();
        targetMember.setPassword(this.passwordEncoder.encode(request.getPassword()));
    }

    @Transactional
    public void updateProfile(Long id, MemberProfileUpdateRequest request)
    {
        // 1. 給repository找
        Optional<Member> member = this.repository.findOneById(id);

        // 2. 如果找不到
        if (member.isEmpty()) {
            throw new ResourceNotFoundException(
                    "member",
                    ResponseMessages.MEMBER_NOT_FOUND);
        }

        Member targetMember = member.get();

        if (request.getName() != null) {
            if (request.getName().isBlank()) {
                throw new MemberAccountExcption("name", ResponseMessages.MEMBER_NAME_REQUIRED);
            }

            targetMember.setName(request.getName().trim());
        }

        if (request.getGender() != null) {
            targetMember.setGender(request.getGender());
        }

        if (request.getEmail() != null) {
            targetMember.setEmail(normalizeEmail(request.getEmail()));
        }

    }

    @Transactional
    public void delete(long id) {
        // 1. 給repository找
        Optional<Member> member = this.repository.findOneById(id);

        // 2. 如果找不到
        if (member.isEmpty()) {
            throw new ResourceNotFoundException(
                    "member",
                    ResponseMessages.MEMBER_NOT_FOUND);
        }

        Member targetMember = member.get();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter DELETED_ACCOUNT_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        byte deleteStatus = 0;
        // 軟刪除時同步改 account，釋放原 account 給新註冊使用。
        targetMember.setStatus(deleteStatus);
        targetMember.setDeletedAt(now);
        targetMember
                .setAccount("del_" + now.format(DELETED_ACCOUNT_TIMESTAMP_FORMAT) + "_" + targetMember.getAccount());
    }
    
    @Transactional
    public TokenResponse login(MemberLoginRequest request) {
        String account = request.getAccount().trim();
        Member member = repository.findOneByAccountAndStatus(account)
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
    

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        return email.trim();
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
