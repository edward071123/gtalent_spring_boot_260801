package student.ed.gtalent_spring_boot_260801.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/page/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/page/books")
    public String booksPage() {
        return "books";
    }
    
}
