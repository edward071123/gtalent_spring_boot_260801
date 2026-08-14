package student.ed.gtalent_spring_boot_260801.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import student.ed.gtalent_spring_boot_260801.constant.ResponseMessages;

// 寄送 email 時的 request body；validation message 放錯誤碼，再由 GlobalExceptionHandler 轉成語系訊息。
public class MailSendRequest {

    @NotBlank(message = ResponseMessages.MAIL_ADDRESS_REQUIRED)
    @Email(message = ResponseMessages.MAIL_ADDRESS_INVALID)
    private String toMailAddress;

    @NotBlank(message = ResponseMessages.MAIL_SUBJECT_REQUIRED)
    @Size(max = 60, message = ResponseMessages.MAIL_SUBJECT_MAX)
    private String subject;

    @NotBlank(message = ResponseMessages.MAIL_CONTENT_REQUIRED)
    @Size(max = 1000, message = ResponseMessages.MAIL_CONTENT_MAX)
    private String content;

    public String getToMailAddress() {
        return this.toMailAddress;
    }

    public void setToMailAddress(String toMailAddress) {
        this.toMailAddress = toMailAddress;
    }

    public String getSubject() {
        return this.subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}
