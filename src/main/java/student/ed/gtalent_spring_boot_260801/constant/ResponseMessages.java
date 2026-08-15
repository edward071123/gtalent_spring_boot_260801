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
    public static final String BOOK_EXCEL_FILE_REQUIRED = "10010"; // Excel 檔案未上傳。
    public static final String BOOK_EXCEL_FILE_INVALID  = "10011"; // Excel 檔案格式錯誤。
    public static final String BOOK_EXCEL_NO_DATA       = "10012"; // Excel 沒有可匯入資料。
    public static final String BOOK_EXCEL_HEADER_INVALID = "10013"; // Excel 標題列錯誤。
    public static final String BOOK_IMPORT_FAILED       = "10014"; // 書籍匯入驗證失敗。
    public static final String MEMBER_ACCOUNT_REQUIRED  = "10015"; // 會員帳號未填。
    public static final String MEMBER_ACCOUNT_MAX       = "10016"; // 會員帳號超過長度限制。
    public static final String MEMBER_PASSWORD_REQUIRED = "10017"; // 會員密碼未填。
    public static final String MEMBER_PASSWORD_MAX      = "10018"; // 會員密碼超過長度限制。
    public static final String MEMBER_NAME_REQUIRED     = "10019"; // 會員姓名未填。
    public static final String MEMBER_NAME_MAX          = "10020"; // 會員姓名超過長度限制。
    public static final String MEMBER_GENDER_INVALID    = "10021"; // 會員性別格式錯誤。
    public static final String MEMBER_OLD_PASSWORD_REQUIRED = "10022"; // 舊密碼未填。
    public static final String MEMBER_NEW_PASSWORD_REQUIRED = "10023"; // 新密碼未填。

    // 20000 區間：資料庫寫入相關錯誤。
    public static final String DATABASE_WRITE_FAILED    = "20000";

    // 30000 區間：HTTP request 相關錯誤。
    public static final String HTTP_REQUEST_FAILED      = "30000";
    public static final String METHOD_NOT_ALLOWED       = "30001"; // HTTP method 不支援。
    public static final String NOT_FOUND                = "30002"; // API 路徑不存在。
    public static final String RESOURCE_NOT_FOUND       = "30003"; // 資料不存在。
    public static final String UNAUTHORIZED             = "30004"; // 未登入或 token 無效。
    public static final String ACCOUNT_OR_PASSWORD_ERROR = "30005"; // 帳號或密碼錯誤。
    public static final String ACCOUNT_ALREADY_EXISTS   = "30006"; // 帳號已存在。
    public static final String OLD_PASSWORD_ERROR       = "30007"; // 舊密碼錯誤。

    public static final String BOOK_NOT_FOUND           = "40001"; // 找不到書籍。
    public static final String MEMBER_NOT_FOUND         = "40002"; // 找不到會員。
    
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
        messages.put(BOOK_EXCEL_FILE_REQUIRED, "請上傳 Excel 檔案");
        messages.put(BOOK_EXCEL_FILE_INVALID,  "Excel 檔案格式錯誤，請上傳 .xlsx 檔案");
        messages.put(BOOK_EXCEL_NO_DATA,       "Excel 沒有可匯入的書籍資料");
        messages.put(BOOK_EXCEL_HEADER_INVALID, "Excel 第一列欄位必須是 name、price");
        messages.put(BOOK_IMPORT_FAILED,       "書籍匯入驗證失敗");
        messages.put(MEMBER_ACCOUNT_REQUIRED,  "會員帳號必填");
        messages.put(MEMBER_ACCOUNT_MAX,       "會員帳號不可超過 30 個字");
        messages.put(MEMBER_PASSWORD_REQUIRED, "會員密碼必填");
        messages.put(MEMBER_PASSWORD_MAX,      "會員密碼不可超過 128 個字");
        messages.put(MEMBER_NAME_REQUIRED,     "會員姓名必填");
        messages.put(MEMBER_NAME_MAX,          "會員姓名不可超過 30 個字");
        messages.put(MEMBER_GENDER_INVALID,    "會員性別只能是 0、1、2");
        messages.put(MEMBER_OLD_PASSWORD_REQUIRED, "舊密碼必填");
        messages.put(MEMBER_NEW_PASSWORD_REQUIRED, "新密碼必填");
        messages.put(DATABASE_WRITE_FAILED, "資料寫入失敗");
        messages.put(HTTP_REQUEST_FAILED,   "HTTP 其他相關的錯誤");
        messages.put(METHOD_NOT_ALLOWED,    "HTTP 方法不支援");
        messages.put(NOT_FOUND,             "找不到 API");
        messages.put(UNAUTHORIZED,          "請先登入或重新登入");
        messages.put(ACCOUNT_OR_PASSWORD_ERROR, "帳號或密碼錯誤");
        messages.put(ACCOUNT_ALREADY_EXISTS, "帳號已存在");
        messages.put(OLD_PASSWORD_ERROR,    "舊密碼錯誤");
        messages.put(BOOK_NOT_FOUND,        "找不到書籍");
        messages.put(MEMBER_NOT_FOUND,      "找不到會員");
        messages.put(RESOURCE_NOT_FOUND,    "資料不存在");
        messages.put(MAIL_SEND_FAILED,      "電子郵件寄送失敗");
        return messages;
    }

    public static String getMessage(String code) {
        return ZH_TW_MESSAGES.get(code);
    }

}
