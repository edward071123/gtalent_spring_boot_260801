package student.ed.gtalent_spring_boot_260801.service;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.NoResultException;
import student.ed.gtalent_spring_boot_260801.constant.PaymentStatus;
import student.ed.gtalent_spring_boot_260801.constant.ResponseMessages;
import student.ed.gtalent_spring_boot_260801.entity.Book;
import student.ed.gtalent_spring_boot_260801.entity.BookOrder;
import student.ed.gtalent_spring_boot_260801.entity.Payment;
import student.ed.gtalent_spring_boot_260801.exception.ResourceNotFoundException;
import student.ed.gtalent_spring_boot_260801.response.NewebPayPaymentFormResponse;
import student.ed.gtalent_spring_boot_260801.repository.BookOrderRepository;
import student.ed.gtalent_spring_boot_260801.repository.BookRepository;
import student.ed.gtalent_spring_boot_260801.repository.PaymentRepository;



@Service
public class NewebPayService {

    private final PaymentRepository paymentRepository;
    private final BookOrderRepository bookOrderRepository;
    private final BookRepository bookRepository;
    private final String merchantId;
    private final String hashKey;
    private final String hashIv;
    private final String version;
    private final String gatewayUrl;
    private final String notifyUrl;
    private final String returnUrl;

    // 抓取application.properties裡的藍新金流設定
    public NewebPayService(
            PaymentRepository paymentRepository,
            BookOrderRepository bookOrderRepository,
            BookRepository bookRepository,
            @Value("${newebpay.merchant-id}") String merchantId,
            @Value("${newebpay.hash-key}") String hashKey,
            @Value("${newebpay.hash-iv}") String hashIv,
            @Value("${newebpay.version}") String version,
            @Value("${newebpay.gateway-url}") String gatewayUrl,
            @Value("${newebpay.notify-url}") String notifyUrl,
            @Value("${newebpay.return-url}") String returnUrl) {
        this.paymentRepository = paymentRepository;
        this.bookOrderRepository = bookOrderRepository;
        this.bookRepository = bookRepository;
        this.merchantId = merchantId;
        this.hashKey = hashKey;
        this.hashIv = hashIv;
        this.version = version;
        this.gatewayUrl = gatewayUrl;
        this.notifyUrl = notifyUrl;
        this.returnUrl = returnUrl;
    }

    @Transactional
    public NewebPayPaymentFormResponse createPaymentForm(Long paymentId) {
        // 產生付款表單前先確認必要設定都有填。
        validateConfig();

        Payment payment = paymentRepository.findById(paymentId)
                            .orElseThrow(() -> new ResourceNotFoundException("payment", ResponseMessages.RESOURCE_NOT_FOUND));

        BookOrder order = bookOrderRepository.findById(payment.getOrderId())
                            .orElseThrow(() -> new ResourceNotFoundException("order", ResponseMessages.RESOURCE_NOT_FOUND));
        
        Book book = findActiveBook(order.getBookId());

        // 組合字串為url
        String url = buildTradeInfo(payment, book);

        // url執行 AES-256-CBC (使用 PKCS7 填充)，並將結果轉換至十六進制
        String tradeInfo = encryptTradeInfo(url);
        
        // 組合字串為hashs
        String hashs = "HashKey=" + hashKey + "&" + tradeInfo + "&HashIV=" + hashIv;

        // 轉成大寫且加密sha256
        String tradeSha = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            tradeSha =  toHex(digest.digest(hashs.getBytes(StandardCharsets.UTF_8))).toUpperCase();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }

        // 表單資料已產生後，付款進入等待使用者完成付款與等待藍新回呼的階段。
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setTradeSha(tradeSha);
        paymentRepository.save(payment);

        return new NewebPayPaymentFormResponse(gatewayUrl, merchantId, version, tradeInfo, tradeSha);
    }

    public String handleNotify(Map<String, String> formParams) {
        // --- IGNORE ---
        return "xxx";
    }

    // 檢查藍新金流商店ID
    // 檢查藍新金流 HashKey
    // 檢查藍新金流 HashIV
    private void validateConfig() {
        if (merchantId.isBlank() || hashKey.isBlank() || hashIv.isBlank()
                || version.isBlank() || gatewayUrl.isBlank()) {
            throw new IllegalStateException("NewebPay config is incomplete");
        }

        if(hashKey.getBytes(StandardCharsets.UTF_8).length != 32) {
            throw new IllegalStateException("NewebPay HashKey must be 32 bytes");
        }

        if(hashIv.getBytes(StandardCharsets.UTF_8).length != 16) {
            throw new IllegalStateException("NewebPay HashIV must be 16 bytes");
        }
    }

    // 組合成url
    private String buildTradeInfo(Payment payment, Book book) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("MerchantID", merchantId);
        params.put("RespondType", "String");
        params.put("TimeStamp", String.valueOf(System.currentTimeMillis() / 1000));
        params.put("Version", version);
        params.put("MerchantOrderNo", payment.getMerchantOrderNo());
        params.put("Amt", String.valueOf(payment.getAmount()));
        params.put("ItemDesc", book.getName());
        params.put("NotifyURL", notifyUrl);

        String toQueryString = params.entrySet().stream()
                                .map(entry -> entry.getKey() + "=" + entry.getValue())
                                .reduce((a, b) -> a + "&" + b)
                                .orElse("");

        return toQueryString;
    }

    // 將 url 進行 AES 加密，並產生 TradeInfo
    private String encryptTradeInfo(String url) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(hashKey.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(hashIv.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            // 藍新 MPG 需要的是 hex 字串，不是 Base64。
            return toHex(cipher.doFinal(pkcs7Padding(url.getBytes(StandardCharsets.UTF_8))));
        } catch (Exception exception) {
            throw new IllegalStateException("NewebPay TradeInfo encryption failed", exception);
        }
    }

    private String toHex(byte[] bytes) {
        // 將 byte array 轉成小寫 hex；TradeSha 會在呼叫端再轉大寫。
        return HexFormat.of().formatHex(bytes);
    }

    private byte[] pkcs7Padding(byte[] source) {
        int paddingSize = 32 - source.length % 32;
        byte[] padded = new byte[source.length + paddingSize];
        System.arraycopy(source, 0, padded, 0, source.length);

        for (int index = source.length; index < padded.length; index++) {
            padded[index] = (byte) paddingSize;
        }

        return padded;
    }

    // 先確認書籍存在且未被軟刪除；不存在就不要建立任何訂單或付款資料。
    private Book findActiveBook(Long bookId) {
        try {
            return bookRepository.findOneById(bookId);
        } catch (NoResultException exception) {
            throw new ResourceNotFoundException("book", ResponseMessages.BOOK_NOT_FOUND);
        }
    }


}
