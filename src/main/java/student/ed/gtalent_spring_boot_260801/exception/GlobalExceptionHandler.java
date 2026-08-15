package student.ed.gtalent_spring_boot_260801.exception;

import student.ed.gtalent_spring_boot_260801.constant.ResponseMessages;
import student.ed.gtalent_spring_boot_260801.response.ApiResponse;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.TreeMap;

// 統一處理 controller 層拋出的例外，讓 API 錯誤回應格式一致。
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 處理 @Valid 驗證失敗，把欄位錯誤訊息合併成統一的 message。
    // 例如 BookCreateRequest 的 name 或 price 不合法時，Spring 會丟 MethodArgumentNotValidException。
    @ExceptionHandler(MethodArgumentNotValidException.class)
    // request 格式正確，但是欄位內容不符合規則，所以回 422。
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ApiResponse handleValidationException(MethodArgumentNotValidException exception) {
        return buildValidationFailedResponse(exception.getBindingResult().getFieldErrors());
    }

    // 處理 @Valid @ModelAttribute 驗證失敗。
    // Excel multipart 上傳是表單資料，不是 JSON body，所以會走 ModelAttribute binding。
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ApiResponse handleBindException(BindException exception) {
        return buildValidationFailedResponse(exception.getBindingResult().getFieldErrors());
    }

    private ApiResponse buildValidationFailedResponse(Iterable<FieldError> fieldErrors) {
        Map<String, String> errors = new TreeMap<>();

        for (FieldError error : fieldErrors) {
            String fieldName = error.getField();
            String errorMessage = ResponseMessages.getMessage(error.getDefaultMessage());

            errors.put(fieldName, errorMessage);
        }

        String message = ResponseMessages.getMessage(ResponseMessages.VALIDATION_FAILED);
        return new ApiResponse(message, errors);
    }

    // 處理 Excel 匯入書籍時的資料錯誤，例如欄位名稱錯誤、價格不是數字、書名空白。
    @ExceptionHandler(BookImportException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ApiResponse handleBookImportException(BookImportException exception) {
        String message = ResponseMessages.getMessage(ResponseMessages.BOOK_IMPORT_FAILED);
        return new ApiResponse(message, exception.getErrors());
    }

    // 處理資料庫 constraint 或寫入失敗，這類已進到資料層的錯誤回傳 400。
    // 例如 repository.save(book) 時違反資料庫限制，Spring 可能會丟 DataIntegrityViolationException。
    @ExceptionHandler(DataIntegrityViolationException.class)
    // 這類錯誤代表資料無法寫入資料庫，所以回 400。
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse handleDataIntegrityViolationException(DataIntegrityViolationException exception) {
        // DB 錯誤訊息統一從 ResponseMessages 取得。
        return new ApiResponse(ResponseMessages.getMessage(ResponseMessages.DATABASE_WRITE_FAILED));
    }

    // 處理 API 路徑不存在，例如使用者呼叫 /bookssss。
    @ExceptionHandler({
            NoHandlerFoundException.class,
            NoResourceFoundException.class
    })
    // 找不到對應 API，所以回 404。
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse handleNotFoundException(Exception exception) {
        return new ApiResponse(ResponseMessages.getMessage(ResponseMessages.NOT_FOUND));
    }

    // 處理 HTTP method 不支援，例如 API 只支援 GET / POST，但使用者送 PUT / DELETE。
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    // API 路徑存在，但是 HTTP method 用錯，所以回 405。
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResponse handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException exception) {
        return new ApiResponse(ResponseMessages.getMessage(ResponseMessages.METHOD_NOT_ALLOWED));
    }

    // 其他 HTTP 相關之錯誤
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse handleException(Exception exception) {
        return new ApiResponse(ResponseMessages.getMessage(ResponseMessages.HTTP_REQUEST_FAILED));
    }

    // 處理查不到指定資料，例如 GET /books/999 或 PUT /books/999。
    @ExceptionHandler(ResourceNotFoundException.class)
    // API 路徑存在，但是 request 指定的資料不存在，所以這裡依專案規則回 400。
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse handleResourceNotFoundException(ResourceNotFoundException exception) {
        Map<String, String> errors = new TreeMap<>();
        errors.put(exception.getErrorKey(), ResponseMessages.getMessage(exception.getMessageCode()));

        String message = ResponseMessages.getMessage(ResponseMessages.RESOURCE_NOT_FOUND);
        return new ApiResponse(message, errors);
    }


    // 處理寄信失敗，例如 SMTP 設定錯誤、帳密錯誤或 mail server 連線失敗。
    @ExceptionHandler(MailException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse handleMailException(MailException exception) {
        return new ApiResponse(ResponseMessages.getMessage(ResponseMessages.MAIL_SEND_FAILED));
    }

}
