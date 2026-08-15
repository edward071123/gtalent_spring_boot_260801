package student.ed.gtalent_spring_boot_260801.repository;

import java.util.Optional;

import student.ed.gtalent_spring_boot_260801.entity.Member;

public interface MemberRepository {

    public Member create(Member member);

    public Optional<Member> findActiveById(Long id);

    public Optional<Member> findActiveByAccount(String account);

    public boolean existsActiveByAccount(String account);

    public void updatePassword(Long id, String encodedPassword);

    public Member updateProfile(Long id, String name, Byte gender);

    public void softDelete(Long id);

}
