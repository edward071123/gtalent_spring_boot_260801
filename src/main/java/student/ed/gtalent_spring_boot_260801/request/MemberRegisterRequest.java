package student.ed.gtalent_spring_boot_260801.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import student.ed.gtalent_spring_boot_260801.constant.ResponseMessages;

// 會員註冊 request。
// account / password / name 都由使用者輸入，所以在 request 層先做基本格式驗證。
public class MemberRegisterRequest {

    @NotBlank(message = ResponseMessages.MEMBER_ACCOUNT_REQUIRED)
    @Size(max = 30, message = ResponseMessages.MEMBER_ACCOUNT_MAX)
    private String account;

    @NotBlank(message = ResponseMessages.MEMBER_PASSWORD_REQUIRED)
    @Size(max = 128, message = ResponseMessages.MEMBER_PASSWORD_MAX)
    private String password;

    @NotBlank(message = ResponseMessages.MEMBER_NAME_REQUIRED)
    @Size(max = 30, message = ResponseMessages.MEMBER_NAME_MAX)
    private String name;

    @NotNull(message = ResponseMessages.MEMBER_GENDER_INVALID)
    @Min(value = 0, message = ResponseMessages.MEMBER_GENDER_INVALID)
    @Max(value = 2, message = ResponseMessages.MEMBER_GENDER_INVALID)
    private Byte gender = 0;

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

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Byte getGender() {
        return this.gender;
    }

    public void setGender(Byte gender) {
        this.gender = gender;
    }

}
