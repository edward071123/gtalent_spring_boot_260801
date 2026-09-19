package student.ed.gtalent_spring_boot_260801.service;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

import javax.crypto.Cipher;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.NoResultException;
import student.ed.gtalent_spring_boot_260801.constant.NotifyStatus;
import student.ed.gtalent_spring_boot_260801.constant.OrderStatus;
import student.ed.gtalent_spring_boot_260801.constant.PaymentStatus;
import student.ed.gtalent_spring_boot_260801.constant.ResponseMessages;
import student.ed.gtalent_spring_boot_260801.entity.Book;
import student.ed.gtalent_spring_boot_260801.entity.BookOrder;
import student.ed.gtalent_spring_boot_260801.entity.Payment;
import student.ed.gtalent_spring_boot_260801.entity.PaymentNotification;
import student.ed.gtalent_spring_boot_260801.exception.ResourceNotFoundException;
import student.ed.gtalent_spring_boot_260801.response.NewebPayPaymentFormResponse;
import student.ed.gtalent_spring_boot_260801.repository.BookOrderRepository;
import student.ed.gtalent_spring_boot_260801.repository.BookRepository;
import student.ed.gtalent_spring_boot_260801.repository.PaymentRepository;
import student.ed.gtalent_spring_boot_260801.repository.PaymentNotificationRepository;



@Service
public class NewebPayService {

    private final PaymentRepository paymentRepository;
    private final BookOrderRepository bookOrderRepository;
    private final BookRepository bookRepository;
    private final PaymentNotificationRepository paymentNotificationRepository;
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
            PaymentNotificationRepository paymentNotificationRepository,
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
        this.paymentNotificationRepository = paymentNotificationRepository;
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

    @Transactional
    public String handleNotify(String rowBody) {
        // NotifyURL 第一件事是保存原始資料。
        // 即使後續解密、驗章、更新狀態失敗，也要留下藍新實際送來的內容，方便查帳。
        PaymentNotification notification = new PaymentNotification(rowBody);
        paymentNotificationRepository.save(notification);

        try {
            // 用私有的方法解析藍新送來的原始表單內容，將每個欄位名稱與值存成 Map。
            // Map<String, String> formParams = { 
            //     "Status": "SUCCESS",
            //     "MerchantID": "MS127874575", 
            //     "Version":2.0, 
            //     "TradeInfo":"ee11d1501e6
            //         dc8433c75988258f2343d11f4d0a423be672e8e02aaf373c53c2363aeffdb4992579693
            //         277359b3e449ebe644d2075fdfbc10150b1c40e7d24cb215febefdb85b16a5cde449f6b
            //         06c58a5510d31e8d34c95284d459ae4b52afc1509c2800976a5c0b99ef24cfd28a2dfc8
            //         004215a0c98a1d3c77707773c2f2132f9a9a4ce3475cb888c2ad372485971876f8e2fec
            //         0589927544c3463d30c785c2d3bd947c06c8c33cf43e131f57939e1f7e3b3d8c3f08a84
            //         f34ef1a67a08efe177f1e663ecc6bedc7f82640a1ced807b548633cfa72d060864271ec
            //         79854ee2f5a170aa902000e7c61d1269165de330fce7d10663d1668c711571776365bfd
            //         cd7ddc915dcb90d31a9f27af9b79a443ca8302e508b0dbaac817d44cfc44247ae613075
            //         dde4ac960f1bdff4173b915e4344bc4567bd32e86be7d796e6d9b9cf20476e4996e98cc
            //         c315f1ed03a34139f936797d971f2a3f90bc18f8a155a290bcbcf04f4277171c305bf55
            //         4f5cba243154b30082748a81f2e5aa432ef9950cc9668cd4330ef7c37537a6dcb5e6ef0
            //         1b4eca9705e4b097cf6913ee96e81d0389e5f775",
            //     "TradeSha":"C80876AEBAC0036268C0E240E5BFF69C0470DE9606EEE083C5C8DD64FDB3347A"
            // };
            Map<String, String> formParams = parseFormBody(rowBody);

            // 檢查藍新回傳的 Status 是否為 SUCCESS，若不是就直接回傳 ERROR。
            if(!formParams.get("Status").equals("SUCCESS")){
                notification.setNotifyStatus("FAILED");
                notification.setErrorMessage("藍新回傳的 Status 不是 SUCCESS");
                paymentNotificationRepository.save(notification);
                return "ERROR";
            }

            // 檢查藍新金流商店ID
            // 檢查藍新金流 HashKey
            // 檢查藍新金流 HashIV
            validateConfig();   

            // 先檢查藍新送來的 MerchantID 是否正確，避免被人偽造。
            if (!formParams.get("MerchantID").equals(merchantId)) {
                notification.setNotifyStatus("FAILED");
                notification.setErrorMessage("藍新回傳的 MerchantID 不正確");
                paymentNotificationRepository.save(notification);
                return "ERROR";
            }

            // 先檢查藍新送來的 TradeSha 是否正確，避免被人偽造。
            String hostTradeSha = generateTradeSha(formParams.get("TradeInfo"));
            if (!hostTradeSha.equals(formParams.get("TradeSha"))) {
                notification.setNotifyStatus("FAILED");
                notification.setErrorMessage("藍新回傳的 TradeSha 不正確");
                paymentNotificationRepository.save(notification);
                return "ERROR";
            }

            // 上面已經驗證成功 所以做一個標記，表示這筆通知已經驗證過了。
            notification.setVerified((byte) 1);

            // 解密藍新送來的 TradeInfo，取得裡面的付款資訊。
            // "ee11d1501e6dc8433c75988258f2343d11f4d0a423be672e8e02aaf373c53c2
            // 363aeffdb4992579693277359b3e449ebe644d2075fdfbc10150b1c40e7d24cb215febe
            // fdb85b16a5cde449f6b06c58a5510d31e8d34c95284d459ae4b52afc1509c2800976a5c
            // 0b99ef24cfd28a2dfc8004215a0c98a1d3c77707773c2f2132f9a9a4ce3475cb888c2ad
            // 372485971876f8e2fec0589927544c3463d30c785c2d3bd947c06c8c33cf43e131f5793
            // 9e1f7e3b3d8c3f08a84f34ef1a67a08efe177f1e663ecc6bedc7f82640a1ced807b5486
            // 33cfa72d060864271ec79854ee2f5a170aa902000e7c61d1269165de330fce7d10663d1
            // 668c711571776365bfdcd7ddc915dcb90d31a9f27af9b79a443ca8302e508b0dbaac817
            // d44cfc44247ae613075dde4ac960f1bdff4173b915e4344bc4567bd32e86be7d796e6d9
            // b9cf20476e4996e98ccc315f1ed03a34139f936797d971f2a3f90bc18f8a155a290bcbc
            // f04f4277171c305bf554f5cba243154b30082748a81f2e5aa432ef9950cc9668cd4330e
            // f7c37537a6dcb5e6ef01b4eca9705e4b097cf6913ee96e81d0389e5f775"
            // 解密出來範例:
            // "Status=SUCCESS&Message=%E6%8E%88%E6%AC%8A%E6%88%90%E5%8A%9F&MerchantID=
            // MS127874575&Amt=30&TradeNo=23092714215835071&MerchantOrderNo=Vanespl_ec
            // _1695795668&RespondType=String&IP=123.51.237.115&EscrowBank=HNCB&Paymen
            // tType=CREDIT&RespondCode=00&Auth=115468&Card6No=400022&Card4No=1111&Exp
            // =2609&AuthBank=KGI&TokenUseStatus=0&InstFirst=0&InstEach=0&Inst=0&ECI=&
            // PayTime=2023-09-27+14%3A21%3A59&PaymentMethod=CREDIT"

            String decryptedTradeInfo = decryptTradeInfo(formParams.get("TradeInfo"));
            // 將解密後的 TradeInfo 解析成 Map，方便後續依欄位名稱取值。
            Map<String, String> decryptedTradeInfoParams = parseFormBody(decryptedTradeInfo);
            String tradeInfoStatus = decryptedTradeInfoParams.get("Status");
            String tradeInfoMessage = decryptedTradeInfoParams.get("Message");
            String merchantOrderNo = decryptedTradeInfoParams.get("MerchantOrderNo");
            String tradeNo = decryptedTradeInfoParams.get("TradeNo");
            String escrowBank = decryptedTradeInfoParams.get("EscrowBank");
            String tradeInfoRespondCode = decryptedTradeInfoParams.get("RespondCode");
            String paymentMethod = decryptedTradeInfoParams.get("PaymentMethod");

            notification.setMerchantOrderNo(merchantOrderNo);
            notification.setProviderTradeNo(tradeNo);

            // 資料庫找出付款單號
            Payment payment = paymentRepository.findByMerchantOrderNo(merchantOrderNo)
                    .orElseThrow(() -> new ResourceNotFoundException("payment", ResponseMessages.RESOURCE_NOT_FOUND));

            // 檢查付款單狀態是否為 PENDING，若不是就直接回傳 ERROR。
            if(!PaymentStatus.PENDING.equals(payment.getPaymentStatus())) {
                notification.setNotifyStatus("FAILED");
                notification.setErrorMessage("付款單狀態不是 PENDING，無法更新");
                paymentNotificationRepository.save(notification);
                return "ERROR";
            }

            // 回寫付款單ID到通知紀錄，方便查帳。
            notification.setPaymentId(payment.getId());

            BookOrder order = bookOrderRepository.findById(payment.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("order", ResponseMessages.RESOURCE_NOT_FOUND));

            // 回寫藍新回傳的 TradeNo、付款方式、回傳代碼、回傳訊息到付款單，方便查帳。
            payment.setProviderTradeNo(tradeNo);
            payment.setPaymentMethod(paymentMethod);
            payment.setReturnCode(tradeInfoRespondCode);
            payment.setReturnMessage(tradeInfoMessage);

            // 更改付款單與訂單狀態，若藍新回傳的 Status 是 SUCCESS 就改成 PAID，否則改成 FAILED。
            if ("SUCCESS".equalsIgnoreCase(tradeInfoStatus)) {
                LocalDateTime paidAt = LocalDateTime.now();
                payment.setPaymentStatus(PaymentStatus.PAID);
                payment.setPaidAt(paidAt);
                order.setOrderStatus(OrderStatus.PAID);
                order.setPaidAt(paidAt);
            } else {
                payment.setPaymentStatus(PaymentStatus.FAILED);
                order.setOrderStatus(OrderStatus.FAILED);
            }

            // 訂單跟付款單儲存
            paymentRepository.save(payment);
            bookOrderRepository.save(order);

            // 更新通知紀錄的狀態為 PROCESSED，並記錄處理時間。
            notification.setNotifyStatus(NotifyStatus.PROCESSED);
            notification.setProcessedAt(LocalDateTime.now());
            paymentNotificationRepository.save(notification);

            return "OK";
        } catch (Exception exception) {
            return "ERROR";
        }
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
        params.put("ReturnURL", returnUrl);
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


    // 將藍新送來的原始表單內容解析成 Map，方便後續依欄位名稱取值。
    // 例如輸入：Status=SUCCESS&MerchantID=MS123&Version=2.0
    // 解析結果：Status -> SUCCESS、MerchantID -> MS123、Version -> 2.0
    private Map<String, String> parseFormBody(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            throw new IllegalArgumentException("NewebPay notify body is empty");
        }

        Map<String, String> formParams = new LinkedHashMap<>();
        
        // 先用 & 分開每一組參數；此時尚未 URL decode，參數值中的 %26 不會被誤切。
        // "Status=SUCCESS", "MerchantID=MS123", "Version=2.0"
        for(String pair : rawBody.split("&")) {
            // 如果是空的就換下一組
            if(pair.isEmpty()) {
                continue;
            }
            // 將參數名稱和值進行 URL decode
            // String[] keyValue = {"Status", "SUCCESS"}
            // ItemDesc=Java+%E5%85%A5%E9%96%80" => {ItemDesc, Java+%E5%85%A5%E9%96%80}
            String[] keyValue = pair.split("=", 2);
            // URLDecoder.decode() 將 URL 編碼的字串轉回原本的字串，
            // 例如 "Java+%E5%85%A5%E9%96%80" 會變成 "Java 入門"
            String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
            // if(keyValue.length == 2) {
            //   String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
            // } else {
            //   String value = "";
            // }
            String value = keyValue.length == 2 ? URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8) : "";
            // 存到map裡，方便後續依欄位名稱取值
            formParams.put(key, value);
        }

        return formParams;
    }

    // 依藍新規格產生 TradeSha：HashKey、TradeInfo、HashIV 串接後做 SHA-256 並轉成大寫。
    private String generateTradeSha(String tradeInfo) {
        String source = "HashKey=" + hashKey + "&" + tradeInfo + "&HashIV=" + hashIv;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 將 source 轉成 byte array 後做 SHA-256，最後再轉成大寫 hex 字串。
            return toHex(digest.digest(source.getBytes(StandardCharsets.UTF_8))).toUpperCase();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    // 將十六進位 TradeInfo 轉回 bytes，再以和加密端相同的 AES-256-CBC 設定解密。
    private String decryptTradeInfo(String tradeInfo) {
        try {
            byte[] tradeInfoBytes = HexFormat.of().parseHex(tradeInfo);
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(hashKey.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(hashIv.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            return new String(removePkcs7Padding(cipher.doFinal(tradeInfoBytes)), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("NewebPay TradeInfo decryption failed", exception);
        }
    }

    // 驗證並移除加密前補上的 padding；本專案加密端以 32 bytes 為 padding 區塊。
    private byte[] removePkcs7Padding(byte[] source) {
        if (source.length == 0) {
            throw new IllegalArgumentException("Decrypted TradeInfo is empty");
        }

        int paddingSize = source[source.length - 1] & 0xff;
        if (paddingSize < 1 || paddingSize > 32 || paddingSize > source.length) {
            throw new IllegalArgumentException("Invalid TradeInfo padding");
        }

        for (int index = source.length - paddingSize; index < source.length; index++) {
            if ((source[index] & 0xff) != paddingSize) {
                throw new IllegalArgumentException("Invalid TradeInfo padding");
            }
        }

        byte[] unpadded = new byte[source.length - paddingSize];
        System.arraycopy(source, 0, unpadded, 0, unpadded.length);
        return unpadded;
    }


}
