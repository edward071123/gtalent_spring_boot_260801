package student.ed.gtalent_spring_boot_260801.request;



import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import student.ed.gtalent_spring_boot_260801.constant.ResponseMessages;

@Getter
@Setter
public class MemberProfileUpdateRequest {

    @Size(max = 30, message = ResponseMessages.MEMBER_NAME_MAX)
    private String name;
    
    @Min(value = 0, message = ResponseMessages.MEMBER_GENDER_INVALID)
    @Max(value = 2, message = ResponseMessages.MEMBER_GENDER_INVALID)
    private Byte gender;

    @Email(message = ResponseMessages.MEMBER_EMAIL_INVALID)
    @Size(max = 128, message = ResponseMessages.MEMBER_EMAIL_MAX)
    private String email;
}
