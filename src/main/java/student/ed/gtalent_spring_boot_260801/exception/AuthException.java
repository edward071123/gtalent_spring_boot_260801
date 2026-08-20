package student.ed.gtalent_spring_boot_260801.exception;

public class AuthException extends RuntimeException {

    private final String errorKey;
    private final String messageCode;

    public AuthException(String errorKey, String messageCode) {
        super(messageCode);
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