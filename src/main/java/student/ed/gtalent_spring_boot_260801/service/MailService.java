package student.ed.gtalent_spring_boot_260801.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {
    
    private JavaMailSender mailSender;

    // 注入 JavaMailSender
    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // 發送電子郵件的方法
    public void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
