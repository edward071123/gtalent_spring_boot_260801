package student.ed.gtalent_spring_boot_260801.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import student.ed.gtalent_spring_boot_260801.entity.BookOrder;

public interface BookOrderRepository extends JpaRepository<BookOrder, Long> {

    public Optional<BookOrder> findByOrderNo(String orderNo);

    public boolean existsByOrderNo(String orderNo);
}
