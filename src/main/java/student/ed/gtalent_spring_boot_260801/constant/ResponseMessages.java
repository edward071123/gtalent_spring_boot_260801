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

    // 20000 區間：資料庫寫入相關錯誤。
    public static final String DATABASE_WRITE_FAILED    = "20000";

    // 30000 區間：HTTP request 相關錯誤。
    public static final String HTTP_REQUEST_FAILED      = "30000";
    public static final String METHOD_NOT_ALLOWED       = "30001"; // HTTP method 不支援。
    public static final String NOT_FOUND                = "30002"; // API 路徑不存在。
    public static final String RESOURCE_NOT_FOUND       = "30003"; // 資料不存在。

    public static final String BOOK_NOT_FOUND           = "40001"; // 找不到書籍。
    

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
        messages.put(DATABASE_WRITE_FAILED, "資料寫入失敗");
        messages.put(HTTP_REQUEST_FAILED,   "HTTP 其他相關的錯誤");
        messages.put(METHOD_NOT_ALLOWED,    "HTTP 方法不支援");
        messages.put(NOT_FOUND,             "找不到 API");
        messages.put(BOOK_NOT_FOUND,        "找不到書籍");
        return messages;
    }

    public static String getMessage(String code) {
        return ZH_TW_MESSAGES.get(code);
    }

}
