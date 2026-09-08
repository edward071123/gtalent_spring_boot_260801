package student.ed.gtalent_spring_boot_260801.response;

import java.time.LocalDateTime;

import lombok.Getter;
import student.ed.gtalent_spring_boot_260801.entity.BookOrder;
import student.ed.gtalent_spring_boot_260801.entity.Payment;

@Getter
public class BookOrderCreateResponse {

    private Long orderId;
    private String orderNo;
    private Long bookId;
    private Long buyerMemberId;
    private Integer amount;
    private String orderStatus;
    private Long paymentId;
    private String merchantOrderNo;
    private String paymentStatus;
    private LocalDateTime createdAt;

    public BookOrderCreateResponse(BookOrder order, Payment payment) {
        this.orderId = order.getId();
        this.orderNo = order.getOrderNo();
        this.bookId = order.getBookId();
        this.buyerMemberId = order.getBuyerMemberId();
        this.amount = order.getAmount();
        this.orderStatus = order.getOrderStatus();
        this.paymentId = payment.getId();
        this.merchantOrderNo = payment.getMerchantOrderNo();
        this.paymentStatus = payment.getPaymentStatus();
        this.createdAt = order.getCreatedAt();
    }
}
