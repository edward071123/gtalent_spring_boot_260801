package student.ed.gtalent_spring_boot_260801.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import student.ed.gtalent_spring_boot_260801.entity.Payment;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // jpaRepository 會自動實作這個方法，透過 merchantOrderNo 查詢 Payment。
    Optional<Payment> findByMerchantOrderNo(String merchantOrderNo);
}
