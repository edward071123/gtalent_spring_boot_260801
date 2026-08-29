package student.ed.gtalent_spring_boot_260801.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import student.ed.gtalent_spring_boot_260801.entity.PasswordResetToken;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    @Modifying
    @Query(
            value = """
                    UPDATE password_reset_tokens
                    SET used = 1,
                        deleted_at = :now,
                        updated_at = :now
                    WHERE member_id = :memberId
                      AND used = 0
                      AND deleted_at IS NULL
                    """,
            nativeQuery = true
    )
    public void revokeActiveByMemberId(
            @Param("memberId") Long memberId,
            @Param("now") LocalDateTime now
    );
}
