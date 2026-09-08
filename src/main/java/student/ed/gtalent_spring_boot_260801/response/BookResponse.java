package student.ed.gtalent_spring_boot_260801.response;

import lombok.Getter;
import student.ed.gtalent_spring_boot_260801.entity.Book;

@Getter
public class BookResponse {
    private Long id;

    private String name;

    private Integer price;

    private Boolean sold;

    public BookResponse(Book book) {
        this(book, false);
    }

    public BookResponse(Book book, Boolean sold) {
        this.id = book.getId();
        this.name = book.getName();
        this.price = book.getPrice();
        this.sold = sold;
    }

}
