package student.ed.gtalent_spring_boot_260801.response;

// Excel 匯入完成後回傳給前端的結果。
// 目前採用「整份 Excel 驗證通過才寫入」的策略，所以只需要回傳成功匯入筆數。
public class BookImportResponse {

    private String message;

    private int importedCount;

    public BookImportResponse(String message, int importedCount) {
        this.message = message;
        this.importedCount = importedCount;
    }

    public String getMessage() {
        return this.message;
    }

    public int getImportedCount() {
        return this.importedCount;
    }

}
