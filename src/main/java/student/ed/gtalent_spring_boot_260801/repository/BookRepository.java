package student.ed.gtalent_spring_boot_260801.repository;

import student.ed.gtalent_spring_boot_260801.entity.Book;

import java.util.List;

public interface BookRepository {

    // 取得所有書籍
    public List<Book> findAll();

    // 新增一本書籍
    public Book create(Book book);

    // 修改一本書籍
    public Book update(Long id,Book book);

}