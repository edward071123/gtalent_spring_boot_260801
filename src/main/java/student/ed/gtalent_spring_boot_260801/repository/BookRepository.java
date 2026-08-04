package student.ed.gtalent_spring_boot_260801.repository;

import student.ed.gtalent_spring_boot_260801.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {

    // 依書名查詢。
    // Spring Data JPA 看到 findByName，會自動產生類似 where name = ? 的查詢。
    List<Book> findByName(String name);

}
