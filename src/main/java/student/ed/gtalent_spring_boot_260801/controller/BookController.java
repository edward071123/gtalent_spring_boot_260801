package student.ed.gtalent_spring_boot_260801.controller;

import student.ed.gtalent_spring_boot_260801.entity.Book;
import student.ed.gtalent_spring_boot_260801.repository.BookRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/books")
public class BookController {

    private final BookRepository repository;

    // 注入式
    public BookController(BookRepository repository) {
        this.repository = repository;
    }

    // 取得所有的書籍
    @GetMapping
    public List<Book> getAll() {
        return repository.findAll();
    }

    // 依書名查詢書籍。
    // 範例：GET /books/search?name=Java 入門
    @GetMapping("/search")
    public List<Book> getByName(@RequestParam String name) {
        return repository.findByName(name);
    }

}

