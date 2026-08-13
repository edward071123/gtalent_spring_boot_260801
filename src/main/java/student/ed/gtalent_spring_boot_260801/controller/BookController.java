package student.ed.gtalent_spring_boot_260801.controller;

import student.ed.gtalent_spring_boot_260801.entity.Book;
import student.ed.gtalent_spring_boot_260801.repository.BookRepository;

import student.ed.gtalent_spring_boot_260801.request.BookCreateRequest;
import student.ed.gtalent_spring_boot_260801.response.ApiResponse;
import student.ed.gtalent_spring_boot_260801.response.BookResponse;
import student.ed.gtalent_spring_boot_260801.response.PageResponse;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

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
    @ResponseStatus(HttpStatus.OK)
    public PageResponse<BookResponse> getAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (page < 1) {
            page = 1;
        }

        if (size < 1) {
            size = 10;
        }

        if (size > 100) {
            size = 100;
        }

        // 先依照 page、size 從資料庫查出這一頁的書籍資料。
        // 這裡拿到的是 Book Entity，代表資料庫中的完整資料，
        // 所以 Book 裡面仍然會有 status、deletedAt 這類只給後端使用的欄位。
        List<Book> books = repository.findAll(page, size);

        // API 不直接回傳 Book Entity，避免把 status、deletedAt 暴露給前端。
        // books.stream()：把 List<Book> 轉成串流，準備逐筆處理。
        // map(BookResponse::new)：每一筆 Book 都執行 new BookResponse(book)，轉成只包含 id、name、price 的 DTO。
        // toList()：把轉換後的 BookResponse 收集回 List<BookResponse>。
        List<BookResponse> bookResponses = books.stream()
                .map(BookResponse::new)
                .toList();
        long totalElements = repository.countAll();

        return new PageResponse<>(bookResponses, page, size, totalElements);
    }

    // 取得單一書籍By Id
    @GetMapping("/searchid/{id}")
    @ResponseStatus(HttpStatus.OK)
    public BookResponse getOneById(@PathVariable Long id) {
        Book book = repository.findOneById(id);
        return new BookResponse(book);
    }

     // 取得單一書籍By Id
    @GetMapping("/searchname/{name}")
    @ResponseStatus(HttpStatus.OK)
    public BookResponse getOneById(@PathVariable String name) {
        Book book = repository.findOneByName(name);
        return new BookResponse(book);
    }


    // 新增一本書籍
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse create(@Valid @RequestBody BookCreateRequest request) {
        Book book = new Book(request.getName(), request.getPrice());
        repository.create(book);
        return new ApiResponse("新增書籍成功");
    }

    // 修改一本書籍
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse update(@PathVariable Long id, @Valid @RequestBody BookCreateRequest request) {
        Book book = new Book(request.getName(), request.getPrice());
        repository.update(id, book);
        return new ApiResponse("修改書籍成功");
    }

    // 軟刪除一本書籍
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse delete(@PathVariable Long id) {
        repository.delete(id);
        return new ApiResponse("刪除書籍成功");
    }
}
