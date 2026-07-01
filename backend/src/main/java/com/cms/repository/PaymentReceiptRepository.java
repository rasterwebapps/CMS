package com.cms.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.PaymentReceipt;

public interface PaymentReceiptRepository extends JpaRepository<PaymentReceipt, Long>, JpaSpecificationExecutor<PaymentReceipt> {

    List<PaymentReceipt> findAllByOrderByCreatedAtDescIdDesc();

    Optional<PaymentReceipt> findByReceiptNumber(String receiptNumber);

    List<PaymentReceipt> findByPayerTypeAndPayerIdOrderByCreatedAtDesc(String payerType, Long payerId);

    List<PaymentReceipt> findByPaymentDateBetween(LocalDate start, LocalDate end);

    @Query(value = "SELECT EXTRACT(YEAR FROM payment_date)::int, EXTRACT(MONTH FROM payment_date)::int, COALESCE(SUM(amount_paid), 0) FROM payment_receipts WHERE payment_date BETWEEN :from AND :to GROUP BY 1, 2", nativeQuery = true)
    List<Object[]> sumAmountByYearMonth(@Param("from") LocalDate from, @Param("to") LocalDate to);
}

