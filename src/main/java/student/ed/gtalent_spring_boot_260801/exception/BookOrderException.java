package student.ed.gtalent_spring_boot_260801.exception;

public class BookOrderException extends ApiException {

    public BookOrderException(String errorKey, String messageCode) {
        super(errorKey, messageCode);
    }
}
