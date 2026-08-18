package student.ed.gtalent_spring_boot_260801.response;

import java.time.LocalDateTime;

import lombok.Getter;
import student.ed.gtalent_spring_boot_260801.entity.Member;

@Getter
public class MemberResponse {

    private Long id;
    private String name;
    private Byte gender;
    private String account;
    private String email;
    private Byte status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MemberResponse(Member member) {
        this.id = member.getId();
        this.name = member.getName();
        this.gender = member.getGender();
        this.account = member.getAccount();
        this.email = member.getEmail();
        this.status = member.getStatus();
        this.createdAt = member.getCreatedAt();
        this.updatedAt = member.getUpdatedAt();
    }
}