package student.ed.gtalent_spring_boot_260801.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import student.ed.gtalent_spring_boot_260801.constant.ResponseMessages;

public class MemberProfileUpdateRequest {

    @NotBlank(message = ResponseMessages.MEMBER_NAME_REQUIRED)
    @Size(max = 30, message = ResponseMessages.MEMBER_NAME_MAX)
    private String name;

    @NotNull(message = ResponseMessages.MEMBER_GENDER_INVALID)
    @Min(value = 0, message = ResponseMessages.MEMBER_GENDER_INVALID)
    @Max(value = 2, message = ResponseMessages.MEMBER_GENDER_INVALID)
    private Byte gender;

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
