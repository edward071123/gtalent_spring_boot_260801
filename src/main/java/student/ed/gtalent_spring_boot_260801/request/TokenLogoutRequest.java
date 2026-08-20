package student.ed.gtalent_spring_boot_260801.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import student.ed.gtalent_spring_boot_260801.constant.ResponseMessages;

@Getter
@Setter
public class TokenLogoutRequest {

    @NotBlank(message = ResponseMessages.REFRESH_TOKEN_REQUIRED)
    private String refreshToken;
}