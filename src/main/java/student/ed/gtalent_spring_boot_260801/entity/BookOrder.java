package student.ed.gtalent_spring_boot_260801.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import student.ed.gtalent_spring_boot_260801.constant.OrderStatus;

@Getter
@Setter
@Entity
@Table(name = "book_orders")
public class BookOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 64, unique = true)
    private String orderNo;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "buyer_member_id", nullable = false)
    private Long buyerMemberId;

    @Column(nullable = false)
    private Integer amount;

    @Column(name = "order_status", nullable = false, length = 32)
    private String orderStatus = OrderStatus.PENDING_PAYMENT;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    protected BookOrder() {
    }

    public BookOrder(String orderNo, Long bookId, Long buyerMemberId, Integer amount) {
        this.orderNo = orderNo;
        this.bookId = bookId;
        this.buyerMemberId = buyerMemberId;
        this.amount = amount;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
