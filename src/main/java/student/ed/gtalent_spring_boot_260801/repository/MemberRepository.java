package student.ed.gtalent_spring_boot_260801.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import student.ed.gtalent_spring_boot_260801.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // 只查未軟刪除的會員。
    @Query(
            value = "SELECT * FROM members WHERE id = :id AND status = :status",
            nativeQuery = true
    )
    public Optional<Member> findOneByIdAndStatus(
            @Param("id") Long id,
            @Param("status") Byte status
    );

    // 會員登入用 account 查詢，status = 1 才能登入。
    @Query(
            value = "SELECT * FROM members WHERE account = :account AND status = :status",
            nativeQuery = true
    )
    public Optional<Member> findOneByAccountAndStatus(
            @Param("account") String account,
            @Param("status") Byte status
    );

    // 註冊前先檢查 account 是否已存在；DB unique key 是最後一層保護。
    @Query(
            value = "SELECT COUNT(*) FROM members WHERE account = :account",
            nativeQuery = true
    )
    public long countByAccount(@Param("account") String account);
}
