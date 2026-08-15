package student.ed.gtalent_spring_boot_260801.request;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import student.ed.gtalent_spring_boot_260801.constant.ResponseMessages;

// Excel 匯入書籍的 request。
//
// 為什麼要把檔案驗證放在 request？
// Controller 的責任是接收 HTTP request。
// Service 的責任是處理商業邏輯，例如解析 Excel、驗證每一列資料、批次寫入資料庫。
//
// 「有沒有上傳 file」、「file 是不是 .xlsx」這種屬於 request 格式檢查，
// 所以放在 request class 裡，和 BookCreateRequest 的 name / price 驗證概念一致。
public class BookExcelImportRequest {

    // multipart/form-data 裡的欄位名稱要叫 file。
    //
    // @NotNull 只能檢查有沒有帶 file 欄位。
    // 如果有帶 file 但檔案是空的，會交給下面的 isFileNotEmpty() 檢查。
    @NotNull(message = ResponseMessages.BOOK_EXCEL_FILE_REQUIRED)
    private MultipartFile file;

    public MultipartFile getFile() {
        return this.file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    // @AssertTrue 代表這個方法必須回傳 true，驗證才算通過。
    //
    // 方法名稱是 isFileNotEmpty，所以如果驗證失敗，
    // GlobalExceptionHandler 看到的欄位名稱會是 fileNotEmpty。
    //
    // 這裡檢查的是：file 欄位有帶，而且內容不是空檔。
    @AssertTrue(message = ResponseMessages.BOOK_EXCEL_FILE_REQUIRED)
    public boolean isFileNotEmpty() {
        return this.file != null && !this.file.isEmpty();
    }

    // 檢查副檔名是不是 .xlsx。
    //
    // 如果 file 是 null 或空檔，這裡先回 true。
    // 原因是：
    // - null / 空檔錯誤已經由 @NotNull 和 isFileNotEmpty() 負責
    // - 這裡只負責「副檔名」錯誤
    // 這樣 response 裡的錯誤訊息比較精準，不會同時出現多個重複錯誤。
    @AssertTrue(message = ResponseMessages.BOOK_EXCEL_FILE_INVALID)
    public boolean isXlsxFile() {
        if (this.file == null || this.file.isEmpty()) {
            return true;
        }

        String filename = this.file.getOriginalFilename();
        return filename != null && filename.toLowerCase().endsWith(".xlsx");
    }

}
