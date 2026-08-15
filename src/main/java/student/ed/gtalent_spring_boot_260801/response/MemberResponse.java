package student.ed.gtalent_spring_boot_260801.response;

import student.ed.gtalent_spring_boot_260801.entity.Member;

public class MemberResponse {

    private Long id;

    private String account;

    private String name;

    private Byte gender;

    public MemberResponse(Member member) {
        this.id = member.getId();
        this.account = member.getAccount();
        this.name = member.getName();
        this.gender = member.getGender();
    }

    public Long getId() {
        return this.id;
    }

    public String getAccount() {
        return this.account;
    }

    public String getName() {
        return this.name;
    }

    public Byte getGender() {
        return this.gender;
    }

}
