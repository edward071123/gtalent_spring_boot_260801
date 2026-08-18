package student.ed.gtalent_spring_boot_260801.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import student.ed.gtalent_spring_boot_260801.constant.ResponseMessages;
import student.ed.gtalent_spring_boot_260801.entity.Member;
import student.ed.gtalent_spring_boot_260801.exception.MemberAccountExcption;
import student.ed.gtalent_spring_boot_260801.exception.ResourceNotFoundException;
import student.ed.gtalent_spring_boot_260801.repository.MemberRepository;
import student.ed.gtalent_spring_boot_260801.request.MemberPasswordUpdateRequest;
import student.ed.gtalent_spring_boot_260801.request.MemberProfileUpdateRequest;
import student.ed.gtalent_spring_boot_260801.request.MemberRegisterRequest;

@Service
public class MemberService {

    private static final Byte STATUS_ACTIVE = 1;
    private static final Byte STATUS_DELETED = 0;
    private static final DateTimeFormatter DELETED_ACCOUNT_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final MemberRepository repository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
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
    public Member updateProfile(Long id, MemberProfileUpdateRequest request) {
        Member member = findActiveMember(id);

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

        member.setStatus(STATUS_DELETED);
        member.setDeletedAt(now);
        member.setAccount("del_" + now.format(DELETED_ACCOUNT_TIMESTAMP_FORMAT) + "_" + member.getAccount());
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
}
