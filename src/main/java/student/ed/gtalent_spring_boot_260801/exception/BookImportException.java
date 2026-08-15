package student.ed.gtalent_spring_boot_260801.exception;

import java.util.Map;

// Excel 匯入書籍時，如果檔案格式或某些列資料不正確，就丟這個例外。
// 這個例外會把錯誤明細保存起來，交給 GlobalExceptionHandler 回傳給前端。
public class BookImportException extends RuntimeException {

    // key 會放 row2.name、row5.price 這種位置；value 放使用者看得懂的錯誤訊息。
    private final Map<String, String> errors;

    public BookImportException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return this.errors;
    }

}
