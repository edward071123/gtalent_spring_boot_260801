package student.ed.gtalent_spring_boot_260801.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import student.ed.gtalent_spring_boot_260801.constant.ResponseMessages;

@Getter
@Setter
public class MemberRegisterRequest {

    @NotBlank(message = ResponseMessages.MEMBER_NAME_REQUIRED)
    @Size(max = 30, message = ResponseMessages.MEMBER_NAME_MAX)
    private String name;

    @NotNull(message = ResponseMessages.MEMBER_GENDER_REQUIRED)
    @Min(value = 0, message = ResponseMessages.MEMBER_GENDER_INVALID)
    @Max(value = 2, message = ResponseMessages.MEMBER_GENDER_INVALID)
    private Byte gender;

    @NotBlank(message = ResponseMessages.MEMBER_ACCOUNT_REQUIRED)
    @Size(max = 30, message = ResponseMessages.MEMBER_ACCOUNT_MAX)
    private String account;

    @Email(message = ResponseMessages.MEMBER_EMAIL_INVALID)
    @Size(max = 128, message = ResponseMessages.MEMBER_EMAIL_MAX)
    private String email;

    @NotBlank(message = ResponseMessages.MEMBER_PASSWORD_REQUIRED)
    @Size(min = 6, max = 12, message = ResponseMessages.MEMBER_PASSWORD_SIZE)
    private String password;

    @NotBlank(message = ResponseMessages.MEMBER_CONFIRM_PASSWORD_REQUIRED)
    private String confirmPassword;
}
