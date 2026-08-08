package student.ed.gtalent_spring_boot_260801.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import student.ed.gtalent_spring_boot_260801.constant.ResponseMessages;

// 新增書籍時的 request body；validation message 放錯誤碼，再由 GlobalExceptionHandler 轉成語系訊息。
public class BookCreateRequest {

    // 書名不可為 null、空字串或只有空白。
    @NotBlank(message = ResponseMessages.BOOK_NAME_REQUIRED)
    private String name;

    // 價格必填，且最小值為 1。
    @NotNull(message = ResponseMessages.BOOK_PRICE_REQUIRED)
    @Min(value = 1, message = ResponseMessages.BOOK_PRICE_MIN)
    private Integer price;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPrice() {
        return this.price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

}

