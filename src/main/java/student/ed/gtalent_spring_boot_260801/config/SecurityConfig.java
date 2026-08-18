package student.ed.gtalent_spring_boot_260801.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {

    // 建立 Spring Security 官方 PasswordEncoder。
    // MemberService 會注入 PasswordEncoder 介面，實際使用 BCryptPasswordEncoder 做密碼加密與比對。
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
