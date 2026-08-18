package student.ed.gtalent_spring_boot_260801.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import student.ed.gtalent_spring_boot_260801.constant.ResponseMessages;

@Getter
@Setter
public class MemberPasswordUpdateRequest {

    @NotBlank(message = ResponseMessages.MEMBER_PASSWORD_REQUIRED)
    @Size(min = 6, max = 12, message = ResponseMessages.MEMBER_PASSWORD_SIZE)
    private String password;

    @NotBlank(message = ResponseMessages.MEMBER_CONFIRM_PASSWORD_REQUIRED)
    private String confirmPassword;
}
