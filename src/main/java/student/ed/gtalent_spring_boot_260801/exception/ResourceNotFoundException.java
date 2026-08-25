package student.ed.gtalent_spring_boot_260801.exception;

// 查不到指定資料時使用的通用例外。
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String errorKey, String messageCode) {
        super(errorKey, messageCode);
    }

}
