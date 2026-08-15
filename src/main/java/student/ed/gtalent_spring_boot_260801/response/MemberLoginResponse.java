package student.ed.gtalent_spring_boot_260801.response;

import java.time.LocalDateTime;

import student.ed.gtalent_spring_boot_260801.entity.Member;

public class MemberLoginResponse {

    private String message;

    private String token;

    private LocalDateTime expiresAt;

    private MemberResponse member;

    public MemberLoginResponse(String message, String token, LocalDateTime expiresAt, Member member) {
        this.message = message;
        this.token = token;
        this.expiresAt = expiresAt;
        this.member = new MemberResponse(member);
    }

    public String getMessage() {
        return this.message;
    }

    public String getToken() {
        return this.token;
    }

    public LocalDateTime getExpiresAt() {
        return this.expiresAt;
    }

    public MemberResponse getMember() {
        return this.member;
    }

}
