package student.ed.gtalent_spring_boot_260801.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import student.ed.gtalent_spring_boot_260801.service.MailService;

@RestController
public class MailController {
    private MailService mailService;

    public MailController(MailService mailService) {
        this.mailService = mailService;
    }

    @GetMapping("/mail/test")
    public String sendEmail() {
        mailService.sendEmail("leonardo071123@gmail.com", "Test Java Gmail", "Edward Test Message");
        return "Email sent successfully!";
    }

    // 練習1: 改成帶入 to、subject、text 參數 可用post or get

    // 練習2: 新增/修改/刪除書籍 寄送gmail通知 
}
