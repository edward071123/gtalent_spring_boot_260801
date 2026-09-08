package student.ed.gtalent_spring_boot_260801.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.NoResultException;
import student.ed.gtalent_spring_boot_260801.constant.OrderStatus;
import student.ed.gtalent_spring_boot_260801.constant.ResponseMessages;
import student.ed.gtalent_spring_boot_260801.entity.Book;
import student.ed.gtalent_spring_boot_260801.entity.BookOrder;
import student.ed.gtalent_spring_boot_260801.entity.Payment;
import student.ed.gtalent_spring_boot_260801.exception.BookOrderException;
import student.ed.gtalent_spring_boot_260801.exception.ResourceNotFoundException;
import student.ed.gtalent_spring_boot_260801.repository.BookOrderRepository;
import student.ed.gtalent_spring_boot_260801.repository.BookRepository;
import student.ed.gtalent_spring_boot_260801.repository.PaymentRepository;
import student.ed.gtalent_spring_boot_260801.response.BookOrderCreateResponse;

@Service
public class BookOrderService {

    private final BookRepository bookRepository;
    private final BookOrderRepository bookOrderRepository;
    private final PaymentRepository paymentRepository;
    private final String newebpayMerchantId;

    public BookOrderService(
            BookRepository bookRepository,
            BookOrderRepository bookOrderRepository,
            PaymentRepository paymentRepository,
            @Value("${newebpay.merchant-id:}") String newebpayMerchantId) {
        this.bookRepository = bookRepository;
        this.bookOrderRepository = bookOrderRepository;
        this.paymentRepository = paymentRepository;
        this.newebpayMerchantId = newebpayMerchantId;
    }

    @Transactional
    public BookOrderCreateResponse createBookOrder(Long bookId, Long buyerMemberId) {
        // 先確認書籍存在且未被軟刪除；不存在就不要建立任何訂單或付款資料。
        Book book = findActiveBook(bookId);
        if (isBookSold(book.getId())) {
            throw new BookOrderException("book", ResponseMessages.BOOK_ALREADY_SOLD);
        }

        // orderNo 會同時作為系統訂單編號與藍新的 MerchantOrderNo，方便後續回呼對帳。
        String orderNo = generateOrderNo();

        // amount 使用下單當下的書籍價格快照，避免日後 books.price 調整影響歷史訂單金額。
        BookOrder order = new BookOrder(orderNo, book.getId(), buyerMemberId, book.getPrice());
        bookOrderRepository.save(order);

        // 建立藍新付款紀錄；此時尚未送出付款表單，所以 paymentStatus 預設為 INIT。
        Payment payment = new Payment(order.getId(), orderNo, newebpayMerchantId, order.getAmount());
        paymentRepository.save(payment);

        // @Transactional 確保訂單與付款紀錄要嘛一起成功，要嘛一起回滾，避免只有訂單沒有付款資料。
        return new BookOrderCreateResponse(order, payment);
    }

    public boolean isBookSold(Long bookId) {
        return bookOrderRepository.existsByBookIdAndOrderStatus(bookId, OrderStatus.PAID);
    }

    private Book findActiveBook(Long bookId) {
        try {
            return bookRepository.findOneById(bookId);
        } catch (NoResultException exception) {
            throw new ResourceNotFoundException("book", ResponseMessages.BOOK_NOT_FOUND);
        }
    }

    private String generateOrderNo() {
        DateTimeFormatter orderNoTimeFormat =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String orderNo = "";
        do {
            // 訂單編號格式：B + 年月日時分秒毫秒 + 4 碼亂數。
            // 例如 B202609081645301231234。
            // B 代表 Book order，方便從藍新後台或系統 log 快速辨識來源。
            String random = String.valueOf(ThreadLocalRandom.current().nextInt(1000, 10000));
            orderNo = "B" + LocalDateTime.now().format(orderNoTimeFormat) + random;

            // 時間戳加亂數已經能大幅降低重複機率，但高併發下仍不是絕對不會碰撞。
            // 因此每次產生後都查一次 DB，確認 order_no 尚未存在；若已存在就重新產生。
        } while (bookOrderRepository.existsByOrderNo(orderNo));

        return orderNo;
    }
}
