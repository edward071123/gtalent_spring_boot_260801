package student.ed.gtalent_spring_boot_260801.controller;

import student.ed.gtalent_spring_boot_260801.entity.Book;
import student.ed.gtalent_spring_boot_260801.repository.BookRepository;

import student.ed.gtalent_spring_boot_260801.request.BookCreateRequest;

import student.ed.gtalent_spring_boot_260801.response.ApiResponse;
import student.ed.gtalent_spring_boot_260801.response.BookOrderCreateResponse;
import student.ed.gtalent_spring_boot_260801.response.BookResponse;
import student.ed.gtalent_spring_boot_260801.response.PageResponse;
import student.ed.gtalent_spring_boot_260801.constant.ResponseMessages;
import student.ed.gtalent_spring_boot_260801.exception.AuthException;
import student.ed.gtalent_spring_boot_260801.exception.ResourceNotFoundException;
import student.ed.gtalent_spring_boot_260801.interceptor.AuthInterceptor;
import student.ed.gtalent_spring_boot_260801.service.BookOrderService;
import student.ed.gtalent_spring_boot_260801.service.MailService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/books")
public class BookController {

    private final BookRepository repository;
    private final BookOrderService bookOrderService;
    private MailService mailService;
    private String toMailAddress = "leonardo071123@gmail.com";
    // 注入式
    public BookController(
            BookRepository repository,
            BookOrderService bookOrderService,
            MailService mailService) {
        this.repository = repository;
        this.bookOrderService = bookOrderService;
        this.mailService = mailService;
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
        mailService.sendEmail(this.toMailAddress, "新增書籍通知", "新增書籍成功，書名：" + request.getName() + "，價格：" + request.getPrice());
        return new ApiResponse("新增書籍成功");
    }

    // 修改一本書籍
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse update(@PathVariable Long id, @Valid @RequestBody BookCreateRequest request) {
        Book book = new Book(request.getName(), request.getPrice());
        repository.update(id, book);
        mailService.sendEmail(this.toMailAddress, "修改書籍通知", "修改書籍成功，書名：" + request.getName() + "，價格：" + request.getPrice());
        return new ApiResponse("修改書籍成功");
    }

    // 軟刪除一本書籍
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse delete(@PathVariable Long id) {
        repository.delete(id);
        mailService.sendEmail(this.toMailAddress, "刪除書籍通知", "刪除書籍成功，書id：" + id);
        return new ApiResponse("刪除書籍成功");
    }

    // 建立書籍購買訂單，後續會用這筆 payment 產生藍新付款表單。
    // 目前不需要 BookCreateOrderRequest，因為建立訂單不信任前端傳入的會員與金額資料：
    // 1. bookId 從 URL path 取得，例如 POST /books/1/orders。
    // 2. buyerMemberId 從 JWT 驗證後的 request attribute 取得，避免前端假冒其他會員下單。
    // 3. amount 由 service 依 bookId 查詢 books.price 後建立價格快照，避免前端竄改付款金額。
    // 如果之後建立訂單時要讓使用者選付款方式，再新增 request class 承接 paymentMethod。
    @PostMapping("/{bookId}/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public BookOrderCreateResponse createOrder(
            @PathVariable Long bookId,
            // buyerMemberId 業務上必填，來源是 AuthInterceptor 驗完 JWT 後放入的 request attribute。
            // 這裡 required = false 是刻意的：如果 attribute 不存在，讓下面自己丟 AuthException，
            // 才會走專案統一的 token 錯誤格式，而不是先被 Spring MVC 轉成通用 HTTP 錯誤。
            @RequestAttribute(name = AuthInterceptor.AUTH_MEMBER_ID_ATTRIBUTE, required = false) Long buyerMemberId) {
        if (bookId == null || bookId < 1) {
            throw new ResourceNotFoundException("book", ResponseMessages.BOOK_NOT_FOUND);
        }

        if (buyerMemberId == null) {
            throw new AuthException("token", ResponseMessages.TOKEN_INVALID);
        }

        return bookOrderService.createBookOrder(bookId, buyerMemberId);
    }
}
