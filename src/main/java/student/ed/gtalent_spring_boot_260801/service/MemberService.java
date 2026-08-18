package student.ed.gtalent_spring_boot_260801.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import student.ed.gtalent_spring_boot_260801.constant.ResponseMessages;
import student.ed.gtalent_spring_boot_260801.request.MemberRegisterRequest;
import student.ed.gtalent_spring_boot_260801.entity.Member;
import student.ed.gtalent_spring_boot_260801.repository.MemberRepository;
import student.ed.gtalent_spring_boot_260801.exception.MemberAccountExcption;

@Service
public class MemberService {

    private MemberRepository repository;

    public MemberService(MemberRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Member register(MemberRegisterRequest request) {
        // 比對傳入的密碼跟確認密碼
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new MemberAccountExcption("confirmPassword", ResponseMessages.MEMBER_CONFIRM_PASSWORD_NOT_MATCH);
        }

        // 驗證輸入的帳號是否已存在系統
        String account = request.getAccount();
        if (this.repository.countByAccount(account) > 0) {
            throw new MemberAccountExcption("account", ResponseMessages.MEMBER_ACCOUNT_EXISTS);
        }
        // 比對帳戶存在系統的話就要跳出例外

        Member member = new Member(
            request.getName(),
            request.getGender(),
            request.getAccount(),
            request.getEmail(),
            request.getPassword()
        );


        // 開始新增資料到資料庫
        try {
            this.repository.save(member);
            return member;
        } catch (RuntimeException exception) {
            // 統一丟資料寫入失敗，讓 GlobalExceptionHandler 判斷資料庫細項錯誤。
            throw new DataIntegrityViolationException(
                    ResponseMessages.getMessage(ResponseMessages.DATABASE_WRITE_FAILED),
                    exception
            );
        }

    }
     
}
