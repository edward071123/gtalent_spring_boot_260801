package student.ed.gtalent_spring_boot_260801.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import student.ed.gtalent_spring_boot_260801.service.Calculator;

@RestController
@RequestMapping("/api/demo")
public class CalculatorController {

    private final Calculator calculator;

    // Spring 會將 Calculator Bean 傳入建構子。
    public CalculatorController(Calculator calculator) {
        this.calculator = calculator;
    }

    @GetMapping("/add")
    public int add(@RequestParam int a, @RequestParam int b) {
        return calculator.add(a, b);
    }

    @GetMapping("/multiply")
    public int multiply(@RequestParam int a, @RequestParam int b) {
        return calculator.multiply(a, b);
    }
}
