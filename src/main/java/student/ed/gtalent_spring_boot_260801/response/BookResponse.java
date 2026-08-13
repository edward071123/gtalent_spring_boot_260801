package student.ed.gtalent_spring_boot_260801.response;

import student.ed.gtalent_spring_boot_260801.entity.Book;

public class BookResponse {

    private Long id;

    private String name;

    private Integer price;

    public BookResponse(Book book) {
        this.id = book.getId();
        this.name = book.getName();
        this.price = book.getPrice();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getPrice() {
        return price;
    }
}
