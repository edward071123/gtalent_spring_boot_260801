package student.ed.gtalent_spring_boot_260801.exception;

// 業務規則不通過時使用的通用例外。
// 例如：帳號已存在、帳號密碼錯誤、舊密碼錯誤、JWT token 無效。
public class BusinessException extends RuntimeException {

    private final String messageCode;

    public BusinessException(String messageCode) {
        this.messageCode = messageCode;
    }

    public String getMessageCode() {
        return this.messageCode;
    }

}
