package student.ed.gtalent_spring_boot_260801.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import student.ed.gtalent_spring_boot_260801.entity.Member;
import student.ed.gtalent_spring_boot_260801.request.MemberLoginRequest;
import student.ed.gtalent_spring_boot_260801.request.MemberPasswordUpdateRequest;
import student.ed.gtalent_spring_boot_260801.request.MemberProfileUpdateRequest;
import student.ed.gtalent_spring_boot_260801.request.MemberRegisterRequest;
import student.ed.gtalent_spring_boot_260801.response.ApiResponse;
import student.ed.gtalent_spring_boot_260801.response.MemberLoginResponse;
import student.ed.gtalent_spring_boot_260801.response.MemberResponse;
import student.ed.gtalent_spring_boot_260801.service.MemberService;

@RestController
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse register(@Valid @RequestBody MemberRegisterRequest request) {
        Member member = memberService.register(request);
        return new MemberResponse(member);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public MemberLoginResponse login(@Valid @RequestBody MemberLoginRequest request) {
        return memberService.login(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse logout(@RequestHeader("Authorization") String authorizationHeader) {
        memberService.logout(authorizationHeader);
        return new ApiResponse("登出成功");
    }

    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public MemberResponse getCurrentMember(@RequestHeader("Authorization") String authorizationHeader) {
        Member member = memberService.getCurrentMember(authorizationHeader);
        return new MemberResponse(member);
    }

    @PatchMapping("/password")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse updatePassword(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody MemberPasswordUpdateRequest request) {
        memberService.updatePassword(authorizationHeader, request);
        return new ApiResponse("修改密碼成功");
    }

    @PutMapping("/profile")
    @ResponseStatus(HttpStatus.OK)
    public MemberResponse updateProfile(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody MemberProfileUpdateRequest request) {
        Member member = memberService.updateProfile(authorizationHeader, request);
        return new MemberResponse(member);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse deleteCurrentMember(@RequestHeader("Authorization") String authorizationHeader) {
        memberService.deleteCurrentMember(authorizationHeader);
        return new ApiResponse("刪除會員成功");
    }

}
