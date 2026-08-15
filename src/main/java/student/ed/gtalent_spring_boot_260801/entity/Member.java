package student.ed.gtalent_spring_boot_260801.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 使用者註冊時 account 最多 30 字。
    // 刪除帳號時會改成 del_ + 10 位 timestamp + _ + 原本 account，最多 45 字，不會超過 64。
    @Column(nullable = false, length = 64, unique = true)
    private String account;

    // 這裡存的是 BCrypt 雜湊後的密碼，不是使用者輸入的明文密碼。
    @Column(nullable = false, length = 128)
    private String password;

    @Column(nullable = false, length = 30)
    private String name;

    // 0: 其他，1: 男，2: 女。
    @Column(nullable = false)
    private Byte gender = 0;

    // 1: 存在，0: 已刪除。
    @Column(nullable = false)
    private Byte status = 1;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    protected Member() {
    }

    public Member(String account, String password, String name, Byte gender) {
        this.account = account;
        this.password = password;
        this.name = name;
        this.gender = gender;
    }

    public Long getId() {
        return this.id;
    }

    public void setIdForJdbc(Long id) {
        this.id = id;
    }

    public String getAccount() {
        return this.account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Byte getGender() {
        return this.gender;
    }

    public void setGender(Byte gender) {
        this.gender = gender;
    }

    public Byte getStatus() {
        return this.status;
    }

    public void setStatus(Byte status) {
        this.status = status;
    }

    public LocalDateTime getDeletedAt() {
        return this.deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

}
