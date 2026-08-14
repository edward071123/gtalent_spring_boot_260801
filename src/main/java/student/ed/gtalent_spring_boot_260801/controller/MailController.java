package student.ed.gtalent_spring_boot_260801.controller;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import student.ed.gtalent_spring_boot_260801.request.MailSendRequest;
import student.ed.gtalent_spring_boot_260801.response.ApiResponse;
import student.ed.gtalent_spring_boot_260801.service.MailService;

@RestController
public class MailController {
    private MailService mailService;

    public MailController(MailService mailService) {
        this.mailService = mailService;
    }

    @PostMapping("/send/gmail")
    public ApiResponse sendEmail(@Valid @RequestBody MailSendRequest request) {
        mailService.sendEmail(request.getToMailAddress(), request.getSubject(), request.getContent());
        return new ApiResponse("寄送Gmail成功");
    }

    // 練習1: 改成帶入 to、subject、text 參數 可用post or get

    // 練習2: 新增/修改/刪除書籍 寄送gmail通知 
}
