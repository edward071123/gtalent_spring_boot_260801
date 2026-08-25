package student.ed.gtalent_spring_boot_260801.exception;

public class AuthException extends ApiException {

    public AuthException(String errorKey, String messageCode) {
        super(errorKey, messageCode);
    }
}
