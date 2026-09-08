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
import student.ed.gtalent_spring_boot_260801.constant.PaymentProviders;
import student.ed.gtalent_spring_boot_260801.constant.PaymentStatus;

@Getter
@Setter
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "merchant_order_no", nullable = false, length = 64, unique = true)
    private String merchantOrderNo;

    @Column(name = "merchant_id", nullable = false, length = 32)
    private String merchantId;

    @Column(nullable = false, length = 32)
    private String provider = PaymentProviders.NEWEBPAY;

    @Column(name = "provider_trade_no", length = 64)
    private String providerTradeNo;

    @Column(name = "payment_method", length = 32)
    private String paymentMethod;

    @Column(nullable = false)
    private Integer amount;

    @Column(name = "payment_status", nullable = false, length = 32)
    private String paymentStatus = PaymentStatus.INIT;

    @Column(name = "trade_sha", length = 128)
    private String tradeSha;

    @Column(name = "return_code", length = 32)
    private String returnCode;

    @Column(name = "return_message")
    private String returnMessage;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Payment() {
    }

    public Payment(Long orderId, String merchantOrderNo, String merchantId, Integer amount) {
        this.orderId = orderId;
        this.merchantOrderNo = merchantOrderNo;
        this.merchantId = merchantId;
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
