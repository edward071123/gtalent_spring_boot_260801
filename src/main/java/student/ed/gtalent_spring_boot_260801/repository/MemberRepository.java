package student.ed.gtalent_spring_boot_260801.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import student.ed.gtalent_spring_boot_260801.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

    @Query(
            value = "SELECT * FROM members WHERE id = :id AND status = :status",
            nativeQuery = true
    )
    public Optional<Member> findOneByIdAndStatus(
            @Param("id") Long id,
            @Param("status") Byte status
    );

    @Query(
            value = "SELECT COUNT(*) FROM members WHERE account = :account",
            nativeQuery = true
    )
    public long countByAccount(@Param("account") String account);
}
