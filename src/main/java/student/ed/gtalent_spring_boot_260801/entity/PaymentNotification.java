package student.ed.gtalent_spring_boot_260801.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import student.ed.gtalent_spring_boot_260801.constant.NotifyStatus;
import student.ed.gtalent_spring_boot_260801.constant.PaymentProviders;

@Getter
@Setter
@Entity
@Table(name = "payment_notifications")
public class PaymentNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "merchant_order_no", length = 64)
    private String merchantOrderNo;

    @Column(nullable = false, length = 32)
    private String provider = PaymentProviders.NEWEBPAY;

    @Column(name = "provider_trade_no", length = 64)
    private String providerTradeNo;

    @Column(name = "notify_status", nullable = false, length = 32)
    private String notifyStatus = NotifyStatus.RECEIVED;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "TEXT")
    private String rawPayload;

    @Column(nullable = false)
    private Byte verified = 0;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    protected PaymentNotification() {
    }

    public PaymentNotification(String rawPayload) {
        this.rawPayload = rawPayload;
    }

    @PrePersist
    public void prePersist() {
        this.receivedAt = LocalDateTime.now();
    }
}
