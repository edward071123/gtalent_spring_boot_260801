package student.ed.gtalent_spring_boot_260801.service;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.crypto.password.PasswordEncoder;

import student.ed.gtalent_spring_boot_260801.constant.AuthOwnerTypes;
import student.ed.gtalent_spring_boot_260801.constant.ResponseMessages;
import student.ed.gtalent_spring_boot_260801.request.MemberLoginRequest;
import student.ed.gtalent_spring_boot_260801.request.MemberForgotPasswordRequest;
import student.ed.gtalent_spring_boot_260801.request.MemberPasswordUpdateRequest;
import student.ed.gtalent_spring_boot_260801.request.MemberPasswordResetRequest;
import student.ed.gtalent_spring_boot_260801.request.MemberProfileUpdateRequest;
import student.ed.gtalent_spring_boot_260801.request.MemberRegisterRequest;
import student.ed.gtalent_spring_boot_260801.response.TokenResponse;
import student.ed.gtalent_spring_boot_260801.entity.AuthToken;
import student.ed.gtalent_spring_boot_260801.entity.Member;
import student.ed.gtalent_spring_boot_260801.entity.PasswordResetToken;
import student.ed.gtalent_spring_boot_260801.repository.AuthTokenRepository;
import student.ed.gtalent_spring_boot_260801.repository.MemberRepository;
import student.ed.gtalent_spring_boot_260801.repository.PasswordResetTokenRepository;
import student.ed.gtalent_spring_boot_260801.exception.MemberAccountExcption;
import student.ed.gtalent_spring_boot_260801.exception.ResourceNotFoundException;

@Service
public class MemberService {

    private MemberRepository repository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthTokenRepository authTokenRepository;
    private PasswordResetTokenRepository passwordResetTokenRepository;
    private MailService mailService;
    private String appBaseUrl;
    private long passwordResetTokenMinutes;
    private Byte TOKEN_REVOKED = 1;
    private Byte PASSWORD_RESET_TOKEN_USED = 1;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public MemberService(
            MemberRepository repository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthTokenRepository authTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            MailService mailService,
            @Value("${app.base-url}") String appBaseUrl,
            @Value("${member.password-reset-token-minutes}") long passwordResetTokenMinutes) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authTokenRepository = authTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.mailService = mailService;
        this.appBaseUrl = appBaseUrl;
        this.passwordResetTokenMinutes = passwordResetTokenMinutes;
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

        // 發 token 後把 hash 與過期時間存 MySQL，供 logout / rotation / token 檢查使用。
        String ownerType = AuthOwnerTypes.MEMBER;
        Long ownerId = member.getId();
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

    @Transactional
    public void logout(String refreshToken) {
        // logout 採用軟撤銷，讓同一顆 refresh token 之後不能再換 token。
        String refreshTokenHash = jwtService.hashToken(refreshToken);
        AuthToken authToken = authTokenRepository
                .findActiveByRefreshTokenHashAndOwnerType(refreshTokenHash, AuthOwnerTypes.MEMBER)
                .orElseThrow(() -> new MemberAccountExcption("refreshToken", ResponseMessages.TOKEN_INVALID));

        authToken.setRevoked(TOKEN_REVOKED);
        authToken.setDeletedAt(LocalDateTime.now());
    }

    @Transactional
    public void forgotPassword(MemberForgotPasswordRequest request) {
        // 忘記密碼允許使用者輸入「帳號」或「email」。
        // trim() 是為了避免前後空白造成查不到會員，例如 "edward "。
        String accountOrEmail = request.getAccountOrEmail().trim();

        // 只查 status = 1 的啟用會員；被軟刪除或停用的會員不能再透過忘記密碼恢復登入。
        Optional<Member> member = repository.findOneByAccountOrEmailAndStatus(accountOrEmail);

        // 安全考量：
        // 不論會員是否存在，controller 都會回「若帳號存在，將寄送重設密碼信」。
        // 這樣外部使用者無法靠 API 回應差異大量測試哪些帳號或 email 已註冊。
        if (member.isEmpty()) {
            return;
        }

        Member targetMember = member.get();

        // 若會員沒有 email，就無法寄送重設密碼連結。
        // 這裡仍然直接 return，維持和「會員不存在」相同的對外回應。
        if (targetMember.getEmail() == null || targetMember.getEmail().isBlank()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // rawToken 是真正寄給使用者的 token，只會出現在 email 連結中。
        // DB 不存 rawToken，只存 SHA-256 hash，降低資料庫外洩時 token 被直接拿去使用的風險。
        String rawToken = generatePasswordResetToken();
        String tokenHash = jwtService.hashToken(rawToken);

        // token 有效時間由 application.properties 的 member.password-reset-token-minutes 控制。
        // 預設是 30 分鐘，避免很久以前的信件連結還能重設密碼。
        LocalDateTime expiresAt = now.plusMinutes(passwordResetTokenMinutes);

        // 同一位會員重新申請忘記密碼時，先把舊的未使用 token 作廢。
        // 這樣信箱裡只有最新那封信的連結有效，減少多個有效連結並存的風險。
        passwordResetTokenRepository.revokeActiveByMemberId(targetMember.getId(), now);

        // 儲存 token hash、會員 id、過期時間與未使用狀態。
        // save 成功後，後續 resetPassword 才能用 token hash 查回這筆紀錄。
        passwordResetTokenRepository.save(new PasswordResetToken(targetMember.getId(), tokenHash, expiresAt));

        // appBaseUrl 是網站對外網址，例如 http://localhost:8080 或正式站 https://example.com。
        // email 必須放完整 URL，使用者點擊後才會回到本系統的重設密碼頁。
        String resetLink = appBaseUrl + "/page/reset-password?token=" + rawToken;

        // 信件內容只放重設連結與有效時間。
        // 不把密碼、會員 id 或 token hash 放進信中。
        String content = """
                您好，

                請點擊下方連結重設密碼：
                %s

                此連結將在 %d 分鐘後失效，若您沒有申請重設密碼，請忽略此信。
                """.formatted(resetLink, passwordResetTokenMinutes);

        // 寄信失敗時 MailService 會丟 MailException，GlobalExceptionHandler 會轉成統一 API 錯誤回應。
        mailService.sendEmail(targetMember.getEmail(), "重設會員密碼", content);
    }

    @Transactional
    public void resetPassword(MemberPasswordResetRequest request) {
        // 重設密碼時也要重新比對 password / confirmPassword。
        // 前端檢查只能改善使用體驗，真正的資料驗證必須放在後端。
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new MemberAccountExcption("confirmPassword", ResponseMessages.MEMBER_CONFIRM_PASSWORD_NOT_MATCH);
        }

        // 使用者送回來的是 email 裡的 raw token。
        // 因為 DB 只存 token hash，所以這裡先把 raw token 做同樣的 SHA-256 hash，再去 DB 比對。
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findValidByTokenHash(jwtService.hashToken(request.getToken().trim()), LocalDateTime.now())
                .orElseThrow(() -> new MemberAccountExcption("token", ResponseMessages.PASSWORD_RESET_TOKEN_INVALID));

        // token 只代表「有一筆有效的重設密碼申請」，還要再用 token 裡存的 memberId 找會員。
        // 若會員已停用或刪除，findOneById 會查不到，這時回覆 token 無效，避免透露會員狀態。
        Member member = repository.findOneById(resetToken.getMemberId())
                .orElseThrow(() -> new MemberAccountExcption("token", ResponseMessages.PASSWORD_RESET_TOKEN_INVALID));

        // BCrypt 加密後才寫回 DB，不能存明碼密碼。
        member.setPassword(passwordEncoder.encode(request.getPassword()));

        // token 使用後立刻標記已使用並 soft delete。
        // 這可以防止同一封 email 連結被重複點擊後再次修改密碼。
        resetToken.setUsed(PASSWORD_RESET_TOKEN_USED);
        resetToken.setDeletedAt(LocalDateTime.now());
    }

    public boolean isPasswordResetTokenValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        return passwordResetTokenRepository
                .findValidByTokenHash(jwtService.hashToken(token.trim()), LocalDateTime.now())
                .isPresent();
    }
    

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        return email.trim();
    }

    private String generatePasswordResetToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

}
