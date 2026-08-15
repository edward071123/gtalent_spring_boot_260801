package student.ed.gtalent_spring_boot_260801.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import student.ed.gtalent_spring_boot_260801.constant.ResponseMessages;
import student.ed.gtalent_spring_boot_260801.entity.Member;
import student.ed.gtalent_spring_boot_260801.exception.BusinessException;
import student.ed.gtalent_spring_boot_260801.exception.ResourceNotFoundException;
import student.ed.gtalent_spring_boot_260801.repository.MemberRepository;
import student.ed.gtalent_spring_boot_260801.request.MemberLoginRequest;
import student.ed.gtalent_spring_boot_260801.request.MemberPasswordUpdateRequest;
import student.ed.gtalent_spring_boot_260801.request.MemberProfileUpdateRequest;
import student.ed.gtalent_spring_boot_260801.request.MemberRegisterRequest;
import student.ed.gtalent_spring_boot_260801.response.MemberLoginResponse;

@Service
public class MemberService {

    private final MemberRepository memberRepository;

    private final JwtService jwtService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public MemberService(MemberRepository memberRepository, JwtService jwtService) {
        this.memberRepository = memberRepository;
        this.jwtService = jwtService;
    }

    public Member register(MemberRegisterRequest request) {
        String account = request.getAccount().trim();

        if (memberRepository.existsActiveByAccount(account)) {
            throw new BusinessException(ResponseMessages.ACCOUNT_ALREADY_EXISTS);
        }

        // 使用者輸入的是明文密碼，不能直接寫進資料庫。
        // BCrypt 會把密碼轉成不可逆雜湊字串，登入時再用 matches() 比對。
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        Member member = new Member(account, encodedPassword, request.getName().trim(), request.getGender());
        return memberRepository.create(member);
    }

    public MemberLoginResponse login(MemberLoginRequest request) {
        Member member = memberRepository.findActiveByAccount(request.getAccount().trim())
                .orElseThrow(() -> new BusinessException(ResponseMessages.ACCOUNT_OR_PASSWORD_ERROR));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new BusinessException(ResponseMessages.ACCOUNT_OR_PASSWORD_ERROR);
        }

        JwtService.JwtToken jwtToken = jwtService.createToken(member.getId());
        return new MemberLoginResponse("登入成功", jwtToken.getToken(), jwtToken.getExpiresAt(), member);
    }

    public void logout(String authorizationHeader) {
        jwtService.revoke(authorizationHeader);
    }

    public Member getCurrentMember(String authorizationHeader) {
        Long memberId = jwtService.getMemberId(authorizationHeader);
        return memberRepository.findActiveById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("member", ResponseMessages.MEMBER_NOT_FOUND));
    }

    public void updatePassword(String authorizationHeader, MemberPasswordUpdateRequest request) {
        Member member = getCurrentMember(authorizationHeader);

        if (!passwordEncoder.matches(request.getOldPassword(), member.getPassword())) {
            throw new BusinessException(ResponseMessages.OLD_PASSWORD_ERROR);
        }

        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        memberRepository.updatePassword(member.getId(), encodedPassword);
    }

    public Member updateProfile(String authorizationHeader, MemberProfileUpdateRequest request) {
        Member member = getCurrentMember(authorizationHeader);
        return memberRepository.updateProfile(member.getId(), request.getName().trim(), request.getGender());
    }

    public void deleteCurrentMember(String authorizationHeader) {
        Member member = getCurrentMember(authorizationHeader);
        memberRepository.softDelete(member.getId());

        // 刪除帳號後，也把目前 token 登出，避免 client 繼續拿同一顆 token 呼叫 API。
        jwtService.revoke(authorizationHeader);
    }

}
