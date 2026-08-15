package student.ed.gtalent_spring_boot_260801.controller;

import student.ed.gtalent_spring_boot_260801.entity.Book;
import student.ed.gtalent_spring_boot_260801.repository.BookRepository;

import student.ed.gtalent_spring_boot_260801.request.BookCreateRequest;
import student.ed.gtalent_spring_boot_260801.request.BookExcelImportRequest;

import student.ed.gtalent_spring_boot_260801.response.ApiResponse;
import student.ed.gtalent_spring_boot_260801.response.BookImportResponse;
import student.ed.gtalent_spring_boot_260801.response.BookResponse;
import student.ed.gtalent_spring_boot_260801.response.PageResponse;
import student.ed.gtalent_spring_boot_260801.service.BookExcelImportService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/books")
public class BookController {

    private final BookRepository repository;

    private final BookExcelImportService bookExcelImportService;

    // 注入式
    public BookController(BookRepository repository, BookExcelImportService bookExcelImportService) {
        this.repository = repository;
        this.bookExcelImportService = bookExcelImportService;
    }

    // 取得所有的書籍
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PageResponse<BookResponse> getAll(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size) {
        // 預設頁碼從1開始
        if(page < 1) {
            page = 1;
        }

        // 每頁最少數量不能為0
        // 如果帶0進來, 自動呈現1頁10組
        if(size < 1) {
            size = 10;
        }

        // 每頁最大不能超過50組
        if (size > 50) {
            size = 50;
        }
        
        List<Book> books = repository.findAll(page, size);

        // API 不直接回傳 Book Entity，避免把 status、deletedAt 暴露給前端。
        // books.stream()：把 List<Book> 轉成串流，準備逐筆處理。
        // map(BookResponse::new)：每一筆 Book 都執行 new BookResponse(book)，轉成只包含id、name、price  的 DTO。
        // toList()：把轉換後的 BookResponse 收集回 List<BookResponse>。
        List<BookResponse> bookResponses = books.stream()
                .map(BookResponse::new)
                .toList();

        long totalElements = repository.countAll();

        return new PageResponse<>(bookResponses, page, size, totalElements);

    }

    // 取得單一書籍By Id
    @GetMapping("/search-id/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Book getOneById(@PathVariable Long id) {
        Book book = repository.findOneById(id);
        return book;
    }

    // 取得單一書籍By Name
    @GetMapping("search-name/{name}")
    @ResponseStatus(HttpStatus.OK)
    public List<Book> getOneByName(@PathVariable String name) {
        return repository.findOneByName(name);
    }


    // 新增一本書籍
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse create(@Valid @RequestBody BookCreateRequest request) {
        Book book = new Book(request.getName(), request.getPrice());
        repository.create(book);
        return new ApiResponse("新增書籍成功");
    }

    // 用 Excel 大量匯入書籍。
    //
    // 呼叫方式：
    // POST /books/import/excel
    // Content-Type: multipart/form-data
    // file: books.xlsx
    //
    // Excel 格式：
    // 第一列必須是標題列：name | price
    // 第二列開始才是資料列，例如：Java 入門 | 500
    //
    // 為了適合 10 萬筆匯入，真正解析和寫入邏輯放在 BookExcelImportService：
    // 1. Controller 只負責接收 HTTP 上傳檔案。
    // 2. Service 負責串流讀 Excel、驗證欄位、分批送 repository。
    // 3. Repository 用 JDBC batch insert 寫入資料庫。
    @PostMapping("/import/excel")
    @ResponseStatus(HttpStatus.CREATED)
    public BookImportResponse importExcel(@Valid @ModelAttribute BookExcelImportRequest request) {
        int importedCount = bookExcelImportService.importBooks(request.getFile());
        return new BookImportResponse("匯入書籍成功", importedCount);
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
