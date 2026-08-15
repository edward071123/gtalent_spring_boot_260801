package student.ed.gtalent_spring_boot_260801.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import student.ed.gtalent_spring_boot_260801.constant.ResponseMessages;
import student.ed.gtalent_spring_boot_260801.entity.Member;

@Repository
public class MemberRepositoryImpl implements MemberRepository {

    private static final Byte ACTIVE_STATUS = 1;

    private static final Byte DELETED_STATUS = 0;

    private final JdbcTemplate jdbcTemplate;

    public MemberRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Member create(Member member) {
        try {
            LocalDateTime now = LocalDateTime.now();

            jdbcTemplate.update(
                    """
                    INSERT INTO members (account, password, name, gender, status, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    member.getAccount(),
                    member.getPassword(),
                    member.getName(),
                    member.getGender(),
                    ACTIVE_STATUS,
                    now,
                    now
            );

            return findActiveByAccount(member.getAccount()).orElseThrow();
        } catch (RuntimeException exception) {
            throw new DataIntegrityViolationException(
                    ResponseMessages.getMessage(ResponseMessages.DATABASE_WRITE_FAILED),
                    exception
            );
        }
    }

    @Override
    public Optional<Member> findActiveById(Long id) {
        try {
            Member member = jdbcTemplate.queryForObject(
                    "SELECT * FROM members WHERE id = ? AND status = ?",
                    new MemberRowMapper(),
                    id,
                    ACTIVE_STATUS
            );

            return Optional.of(member);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Member> findActiveByAccount(String account) {
        try {
            Member member = jdbcTemplate.queryForObject(
                    "SELECT * FROM members WHERE account = ? AND status = ?",
                    new MemberRowMapper(),
                    account,
                    ACTIVE_STATUS
            );

            return Optional.of(member);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override
    public boolean existsActiveByAccount(String account) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM members WHERE account = ? AND status = ?",
                Integer.class,
                account,
                ACTIVE_STATUS
        );

        return count != null && count > 0;
    }

    @Override
    public void updatePassword(Long id, String encodedPassword) {
        jdbcTemplate.update(
                "UPDATE members SET password = ?, updated_at = ? WHERE id = ? AND status = ?",
                encodedPassword,
                LocalDateTime.now(),
                id,
                ACTIVE_STATUS
        );
    }

    @Override
    public Member updateProfile(Long id, String name, Byte gender) {
        jdbcTemplate.update(
                "UPDATE members SET name = ?, gender = ?, updated_at = ? WHERE id = ? AND status = ?",
                name,
                gender,
                LocalDateTime.now(),
                id,
                ACTIVE_STATUS
        );

        return findActiveById(id).orElseThrow();
    }

    @Override
    public void softDelete(Long id) {
        Member member = findActiveById(id).orElseThrow();
        LocalDateTime now = LocalDateTime.now();
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String deletedAccount = "del_" + timestamp + "_" + member.getAccount();

        jdbcTemplate.update(
                """
                UPDATE members
                SET account = ?, status = ?, deleted_at = ?, updated_at = ?
                WHERE id = ? AND status = ?
                """,
                deletedAccount,
                DELETED_STATUS,
                now,
                now,
                id,
                ACTIVE_STATUS
        );
    }

    private static class MemberRowMapper implements RowMapper<Member> {

        @Override
        public Member mapRow(ResultSet resultSet, int rowNum) throws SQLException {
            Member member = new Member(
                    resultSet.getString("account"),
                    resultSet.getString("password"),
                    resultSet.getString("name"),
                    resultSet.getByte("gender")
            );

            member.setStatus(resultSet.getByte("status"));
            member.setDeletedAt(toLocalDateTime(resultSet.getTimestamp("deleted_at")));
            member.setCreatedAt(toLocalDateTime(resultSet.getTimestamp("created_at")));
            member.setUpdatedAt(toLocalDateTime(resultSet.getTimestamp("updated_at")));

            // JdbcTemplate 建出來的物件不會自動塞 @Id，所以這裡手動設定。
            member.setIdForJdbc(resultSet.getLong("id"));
            return member;
        }

        private LocalDateTime toLocalDateTime(Timestamp timestamp) {
            if (timestamp == null) {
                return null;
            }

            return timestamp.toLocalDateTime();
        }
    }

}
