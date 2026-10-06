package student.ed.gtalent_spring_boot_260801.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import student.ed.gtalent_spring_boot_260801.service.MemberService;

@Controller
public class PageController {

    private final MemberService memberService;

    @Value("${line.liff.id}")
    private String liffId;

    public PageController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/page/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/page/books")
    public String booksPage() {
        return "books";
    }

    @GetMapping("/page/liff")
    public String liffPage(Model model) {
        model.addAttribute("liffId", liffId);
        return "liff-profile";
    }

    @GetMapping("/page/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @GetMapping("/page/reset-password")
    public String resetPasswordPage(
            @RequestParam(required = false) String token,
            Model model) {
        model.addAttribute("tokenValid", memberService.isPasswordResetTokenValid(token));
        return "reset-password";
    }
    
}
