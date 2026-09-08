package student.ed.gtalent_spring_boot_260801.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import student.ed.gtalent_spring_boot_260801.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    public Optional<Payment> findByMerchantOrderNo(String merchantOrderNo);
}
