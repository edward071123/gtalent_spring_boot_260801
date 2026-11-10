package student.ed.gtalent_spring_boot_260801.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import student.ed.gtalent_spring_boot_260801.service.CalculatorService;


@Configuration 
public class CalculatorConfig {
    

    @Bean 
    public CalculatorService calculator() {
        return new CalculatorService();
    }
}
