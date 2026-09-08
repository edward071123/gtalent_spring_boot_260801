package student.ed.gtalent_spring_boot_260801.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
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
import student.ed.gtalent_spring_boot_260801.repository.BookOrderRepository;
import student.ed.gtalent_spring_boot_260801.repository.BookRepository;
import student.ed.gtalent_spring_boot_260801.repository.PaymentRepository;
import student.ed.gtalent_spring_boot_260801.response.NewebPayPaymentFormResponse;

@Service
public class NewebPayService {

    // 藍新 MPG TradeInfo 使用 AES-256-CBC 加密。
    // Java 的 PKCS5Padding 對 AES block cipher 來說可對應藍新文件常見的 PKCS7 padding 實作。
    private static final String AES_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String AES_ALGORITHM = "AES";

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
        // HashKey / HashIV / MerchantID 是藍新後台提供的機敏資訊，不應寫死在程式碼。
        validateConfig();

        // 付款表單必須從系統內已建立的 payment 產生，不能讓前端自己組金額或訂單編號。
        // paymentId 由前一步建立訂單 API 回傳，這裡再回資料庫查一次，確保使用的是後端可信資料。
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("payment", ResponseMessages.RESOURCE_NOT_FOUND));

        // payment 只保存付款資料，實際購買哪本書要從對應的 book_order 取得。
        BookOrder order = bookOrderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("order", ResponseMessages.RESOURCE_NOT_FOUND));

        // 重新查書籍資料是為了取得 ItemDesc。
        // 金額不從 books.price 重新取，而是使用 payment.amount，避免書價異動影響已建立訂單。
        Book book = findActiveBook(order.getBookId());

        // TradeInfo 是藍新實際讀取的交易資料；先組成 query string，再用 HashKey/HashIV 做 AES 加密。
        String tradeInfo = encryptTradeInfo(buildTradeInfoParams(payment, book));

        // TradeSha 是 TradeInfo 的 SHA-256 簽章，用來讓藍新驗證交易資料沒有被竄改。
        String tradeSha = generateTradeSha(tradeInfo);

        // 表單資料已產生後，付款進入等待使用者完成付款與等待藍新回呼的階段。
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setTradeSha(tradeSha);
        paymentRepository.save(payment);

        return new NewebPayPaymentFormResponse(gatewayUrl, merchantId, version, tradeInfo, tradeSha);
    }

    private Map<String, String> buildTradeInfoParams(Payment payment, Book book) {
        // 使用 LinkedHashMap 固定參數輸出順序，方便除錯與比對加密前明文。
        // 藍新驗證重點是加密後的 TradeInfo 與 TradeSha，順序固定能降低排查成本。
        Map<String, String> params = new LinkedHashMap<>();

        // MerchantID：藍新商店代號。
        params.put("MerchantID", merchantId);

        // RespondType：指定藍新付款結果回傳格式，JSON 較適合後端解析。
        params.put("RespondType", "JSON");

        // TimeStamp：交易建立時間，藍新使用 Unix timestamp 秒數。
        params.put("TimeStamp", String.valueOf(Instant.now().getEpochSecond()));

        // Version：MPG 串接版本，放在 .env，方便依藍新文件調整。
        params.put("Version", version);

        // MerchantOrderNo：商店訂單編號；這裡使用系統 orderNo，方便 NotifyURL 回來時對帳。
        params.put("MerchantOrderNo", payment.getMerchantOrderNo());

        // Amt：付款金額，只能使用資料庫 payment.amount，不能使用前端傳入金額。
        params.put("Amt", String.valueOf(payment.getAmount()));

        // ItemDesc：商品說明，藍新付款頁會顯示給使用者看。
        params.put("ItemDesc", book.getName());

        // LoginType=0：不要求付款人登入藍新會員。
        params.put("LoginType", "0");

        // NotifyURL 是藍新背景通知後端的 webhook，正式判斷付款成功應以 NotifyURL 為主。
        if (!notifyUrl.isBlank()) {
            params.put("NotifyURL", notifyUrl);
        }

        // ReturnURL 是付款完成後瀏覽器導回的頁面，只適合顯示結果，不適合當作付款成功依據。
        if (!returnUrl.isBlank()) {
            params.put("ReturnURL", returnUrl);
        }

        return params;
    }

    private String encryptTradeInfo(Map<String, String> params) {
        // TradeInfo 加密前是 URL query string，例如：
        // MerchantID=xxx&RespondType=JSON&TimeStamp=...&MerchantOrderNo=...
        String plainText = toQueryString(params);

        try {
            // HashKey 必須是 32 bytes，HashIV 必須是 16 bytes，才符合 AES-256-CBC。
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            SecretKeySpec keySpec = new SecretKeySpec(hashKey.getBytes(StandardCharsets.UTF_8), AES_ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(hashIv.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            // 藍新 MPG 需要的是 hex 字串，不是 Base64。
            return toHex(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("NewebPay TradeInfo encryption failed", exception);
        }
    }

    private String generateTradeSha(String tradeInfo) {
        // TradeSha 原文格式固定：
        // HashKey={HashKey}&{TradeInfo}&HashIV={HashIV}
        // 做 SHA-256 後轉成大寫 hex，送給藍新驗證 TradeInfo 沒有被竄改。
        String plainText = "HashKey=" + hashKey + "&" + tradeInfo + "&HashIV=" + hashIv;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(plainText.getBytes(StandardCharsets.UTF_8))).toUpperCase();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String toQueryString(Map<String, String> params) {
        // 每個 key/value 都做 URL encode，避免書名或網址有空白、中文、特殊符號造成藍新解析錯誤。
        return params.entrySet()
                .stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String toHex(byte[] bytes) {
        // 將 byte array 轉成小寫 hex；TradeSha 會在呼叫端再轉大寫。
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            hex.append(String.format("%02x", value));
        }
        return hex.toString();
    }

    private Book findActiveBook(Long bookId) {
        // 沿用既有 BookRepository 規則，只查 status = 1 的書籍。
        try {
            return bookRepository.findOneById(bookId);
        } catch (NoResultException exception) {
            throw new ResourceNotFoundException("book", ResponseMessages.BOOK_NOT_FOUND);
        }
    }

    private void validateConfig() {
        // 這些設定缺少任一項都無法產生有效的藍新付款資料。
        // notifyUrl / returnUrl 可依環境決定是否填，所以不在這裡強制檢查。
        if (merchantId.isBlank() || hashKey.isBlank() || hashIv.isBlank()
                || version.isBlank() || gatewayUrl.isBlank()) {
            throw new IllegalStateException("NewebPay config is incomplete");
        }

        if (hashKey.getBytes(StandardCharsets.UTF_8).length != 32
                || hashIv.getBytes(StandardCharsets.UTF_8).length != 16) {
            throw new IllegalStateException("NewebPay HashKey must be 32 bytes and HashIV must be 16 bytes");
        }
    }
}
