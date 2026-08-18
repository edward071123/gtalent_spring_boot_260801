package student.ed.gtalent_spring_boot_260801.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import student.ed.gtalent_spring_boot_260801.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
    
    @Query(
        value="SELECT COUNT(*) FROM members WHERE account = :account",
                nativeQuery = true
    )
    // 去資料庫內搜尋這個帳號的數量
    public long countByAccount(@Param("account") String account);
}
