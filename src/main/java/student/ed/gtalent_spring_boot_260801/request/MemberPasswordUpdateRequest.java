package student.ed.gtalent_spring_boot_260801.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import student.ed.gtalent_spring_boot_260801.constant.ResponseMessages;

public class MemberPasswordUpdateRequest {

    @NotBlank(message = ResponseMessages.MEMBER_OLD_PASSWORD_REQUIRED)
    @Size(max = 128, message = ResponseMessages.MEMBER_PASSWORD_MAX)
    private String oldPassword;

    @NotBlank(message = ResponseMessages.MEMBER_NEW_PASSWORD_REQUIRED)
    @Size(max = 128, message = ResponseMessages.MEMBER_PASSWORD_MAX)
    private String newPassword;

    public String getOldPassword() {
        return this.oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return this.newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

}
