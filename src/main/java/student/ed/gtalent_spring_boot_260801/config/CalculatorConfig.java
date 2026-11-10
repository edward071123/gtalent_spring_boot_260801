package student.ed.gtalent_spring_boot_260801.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import student.ed.gtalent_spring_boot_260801.service.Calculator;

@Configuration
public class CalculatorConfig {

    // Spring 會管理這個方法回傳的 Calculator 物件。
    @Bean
    public Calculator calculator() {
        return new Calculator();
    }
}
