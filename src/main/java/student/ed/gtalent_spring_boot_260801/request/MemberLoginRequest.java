package student.ed.gtalent_spring_boot_260801.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import student.ed.gtalent_spring_boot_260801.constant.ResponseMessages;

public class MemberLoginRequest {

    @NotBlank(message = ResponseMessages.MEMBER_ACCOUNT_REQUIRED)
    @Size(max = 30, message = ResponseMessages.MEMBER_ACCOUNT_MAX)
    private String account;

    @NotBlank(message = ResponseMessages.MEMBER_PASSWORD_REQUIRED)
    @Size(max = 128, message = ResponseMessages.MEMBER_PASSWORD_MAX)
    private String password;

    public String getAccount() {
        return this.account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
