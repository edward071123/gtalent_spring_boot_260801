package student.ed.gtalent_spring_boot_260801.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import student.ed.gtalent_spring_boot_260801.request.MemberPasswordUpdateRequest;
import student.ed.gtalent_spring_boot_260801.request.MemberProfileUpdateRequest;
import student.ed.gtalent_spring_boot_260801.request.MemberRegisterRequest;
import student.ed.gtalent_spring_boot_260801.response.ApiResponse;
import student.ed.gtalent_spring_boot_260801.response.MemberResponse;
import student.ed.gtalent_spring_boot_260801.service.MemberService;

@RestController
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public MemberResponse getOneById(@PathVariable Long id) {
        return new MemberResponse(memberService.findOneById(id));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse register(@Valid @RequestBody MemberRegisterRequest request) {
        memberService.register(request);
        return new ApiResponse("會員註冊成功");
    }

    @PutMapping("/{id}/profile")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody MemberProfileUpdateRequest request) {
        memberService.updateProfile(id, request);
        return new ApiResponse("會員基本資料修改成功");
    }

    @PutMapping("/{id}/password")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse updatePassword(
            @PathVariable Long id,
            @Valid @RequestBody MemberPasswordUpdateRequest request) {
        memberService.updatePassword(id, request);
        return new ApiResponse("會員密碼修改成功");
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse delete(@PathVariable Long id) {
        memberService.delete(id);
        return new ApiResponse("會員帳號刪除成功");
    }
}
