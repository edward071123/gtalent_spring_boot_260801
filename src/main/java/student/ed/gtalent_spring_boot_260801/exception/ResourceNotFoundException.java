package student.ed.gtalent_spring_boot_260801.exception;

// 查不到指定資料時使用的通用例外。
public class ResourceNotFoundException extends RuntimeException {

    private final String errorKey;

    private final String messageCode;

    public ResourceNotFoundException(String errorKey, String messageCode) {
        this.errorKey = errorKey;
        this.messageCode = messageCode;
    }

    public String getErrorKey() {
        return errorKey;
    }

    public String getMessageCode() {
        return messageCode;
    }

}
