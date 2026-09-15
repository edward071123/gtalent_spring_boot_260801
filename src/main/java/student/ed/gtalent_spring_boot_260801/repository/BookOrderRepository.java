package student.ed.gtalent_spring_boot_260801.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import student.ed.gtalent_spring_boot_260801.entity.BookOrder;

public interface BookOrderRepository extends JpaRepository<BookOrder, Long> {
    
    @Query(
        value = "SELECT COUNT(*) FROM book_orders WHERE book_id = :bookId AND order_status = :orderStatus",
        nativeQuery = true
    )
    public boolean existsByBookIdAndOrderStatus(Long bookId, String orderStatus);

    @Query(
        value = "SELECT COUNT(*) FROM book_orders WHERE order_no = :orderNo",
        nativeQuery = true
    )
    public boolean existsByOrderNo(String orderNo);
}
