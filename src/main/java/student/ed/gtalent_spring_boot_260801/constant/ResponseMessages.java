package student.ed.gtalent_spring_boot_260801.constant;

import java.util.HashMap;
import java.util.Map;

// 集中管理 API 訊息；程式用錯誤碼找 message，之後要支援多語系時可以依語系換不同 Map。
public final class ResponseMessages {

    // 00000 區間：成功訊息。
    public static final String CREATE_SUCCESS           = "00000";
    public static final String UPDATE_SUCCESS           = "00001";
    public static final String DELETE_SUCCESS           = "00002";

    // 10000 區間：request validation 相關錯誤。錯誤碼用數字字串，因為 validation annotation 的 message 只能放 String。
    public static final String VALIDATION_FAILED        = "10000"; // validation 大項目錯誤。
    public static final String BOOK_NAME_REQUIRED       = "10001"; // 書名未填。
    public static final String BOOK_PRICE_REQUIRED      = "10002"; // 價格未填。
    public static final String BOOK_PRICE_MIN           = "10003"; // 價格小於允許的最小值。
    public static final String MAIL_ADDRESS_REQUIRED    = "10004"; // email 未填。
    public static final String MAIL_ADDRESS_INVALID     = "10005"; // email 格式錯誤。
    public static final String MAIL_SUBJECT_REQUIRED    = "10006"; // 信件標題未填。
    public static final String MAIL_SUBJECT_MAX         = "10007"; // 信件標題超過長度限制。
    public static final String MAIL_CONTENT_REQUIRED    = "10008"; // 信件內容未填。
    public static final String MAIL_CONTENT_MAX         = "10009"; // 信件內容超過長度限制。

    // 20000 區間：資料庫寫入相關錯誤。
    public static final String DATABASE_WRITE_FAILED    = "20000";

    // 30000 區間：HTTP request 相關錯誤。
    public static final String HTTP_REQUEST_FAILED      = "30000";
    public static final String METHOD_NOT_ALLOWED       = "30001"; // HTTP method 不支援。
    public static final String NOT_FOUND                = "30002"; // API 路徑不存在。
    public static final String RESOURCE_NOT_FOUND       = "30003"; // 資料不存在。

    public static final String BOOK_NOT_FOUND           = "40001"; // 找不到書籍。
    
    // 50000 區間：外接第三方服務通知相關錯誤。
    public static final String MAIL_SEND_FAILED         = "50000"; // 電子郵件寄送失敗。

    private static final Map<String, String> ZH_TW_MESSAGES = createZhTwMessages();

    private static Map<String, String> createZhTwMessages() {
        Map<String, String> messages = new HashMap<>();
        messages.put(CREATE_SUCCESS,        "新增成功");
        messages.put(UPDATE_SUCCESS,        "修改成功");
        messages.put(DELETE_SUCCESS,        "刪除成功");
        messages.put(VALIDATION_FAILED,     "資料驗證失敗");
        messages.put(BOOK_NAME_REQUIRED,    "書名必填");
        messages.put(BOOK_PRICE_REQUIRED,   "價格必填");
        messages.put(BOOK_PRICE_MIN,        "價格必須大於等於 1");
        messages.put(MAIL_ADDRESS_REQUIRED, "email 必填");
        messages.put(MAIL_ADDRESS_INVALID,  "email 格式錯誤");
        messages.put(MAIL_SUBJECT_REQUIRED, "信件標題必填");
        messages.put(MAIL_SUBJECT_MAX,      "信件標題不可超過 60 個字");
        messages.put(MAIL_CONTENT_REQUIRED, "信件內容必填");
        messages.put(MAIL_CONTENT_MAX,      "信件內容不可超過 1000 個字");
        messages.put(DATABASE_WRITE_FAILED, "資料寫入失敗");
        messages.put(HTTP_REQUEST_FAILED,   "HTTP 其他相關的錯誤");
        messages.put(METHOD_NOT_ALLOWED,    "HTTP 方法不支援");
        messages.put(NOT_FOUND,             "找不到 API");
        messages.put(BOOK_NOT_FOUND,        "找不到書籍");
        messages.put(RESOURCE_NOT_FOUND,    "資料不存在");
        messages.put(MAIL_SEND_FAILED,      "電子郵件寄送失敗");
        return messages;
    }

    public static String getMessage(String code) {
        return ZH_TW_MESSAGES.get(code);
    }

}
