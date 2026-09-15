package student.ed.gtalent_spring_boot_260801.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import student.ed.gtalent_spring_boot_260801.constant.ResponseMessages;
import student.ed.gtalent_spring_boot_260801.interceptor.AuthInterceptor;
import student.ed.gtalent_spring_boot_260801.exception.AuthException;
import student.ed.gtalent_spring_boot_260801.exception.ResourceNotFoundException;
import student.ed.gtalent_spring_boot_260801.service.BookOrderService;
import student.ed.gtalent_spring_boot_260801.service.NewebPayService;
import student.ed.gtalent_spring_boot_260801.response.BookOrderCreateResponse;
import student.ed.gtalent_spring_boot_260801.response.NewebPayPaymentFormResponse;


@RestController
@RequestMapping("/payments")
public class PaymentController {
    private final BookOrderService bookOrderService;
    private final NewebPayService newebPayService;

    public PaymentController(BookOrderService bookOrderService, NewebPayService newebPayService) {
        this.bookOrderService = bookOrderService;
        this.newebPayService = newebPayService;
    }

    // 一段式建立藍新付款表單。
    // 前端按「購買」後可以直接呼叫這支 API：
    // 1. AuthInterceptor 先從 JWT 取出 buyerMemberId 並放進 request attribute。
    // 2. 這裡建立 book_orders 與 payments。
    // 3. 再立刻用新建立的 paymentId 產生藍新 MPG 表單資料。
    @PostMapping("books/{bookId}/newebpay/form")
    @ResponseStatus(HttpStatus.OK)
    public NewebPayPaymentFormResponse createBookOrderAndNewebPayForm(
            @PathVariable Long bookId,                           
            @RequestAttribute(name = AuthInterceptor.AUTH_MEMBER_ID_ATTRIBUTE, required = false) Long buyerMemberId) {
        if (buyerMemberId == null) {
            throw new AuthException("token", ResponseMessages.TOKEN_INVALID);
        }

        if (bookId == null || bookId < 1) {
            throw new ResourceNotFoundException("book", ResponseMessages.BOOK_NOT_FOUND);
        }

        BookOrderCreateResponse order = bookOrderService.createBookOrder(bookId, buyerMemberId);

        return newebPayService.createPaymentForm(order.getPaymentId(), buyerMemberId);
    }


    // 藍新 NotifyURL：付款結果的後端背景通知。
    // 這支不能要求會員 JWT，因為呼叫方是藍新伺服器，不是前端使用者。
    // service 會先保存原始通知，再驗證 TradeSha、解密 TradeInfo，最後更新付款與訂單狀態。
    @PostMapping("/newebpay/notify")
    @ResponseStatus(HttpStatus.OK)
    public String notifyNewebPay(@RequestParam Map<String, String> formParams) {
       return newebPayService.handleNotify(formParams);
    }

    // 藍新 ReturnURL：付款完成後，使用者瀏覽器被導回的入口。
    // 這裡只負責把使用者導回書籍列表，不更新付款成功狀態；
    // 正式付款結果以 NotifyURL 或交易查詢 API 為準。
    @RequestMapping(value = "/newebpay/return", method = {RequestMethod.GET, RequestMethod.POST})
    public RedirectView returnFromNewebPay() {
        return new RedirectView("/page/books");
    }

}
