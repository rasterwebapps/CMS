package com.cms.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.PaymentReceipt;

public interface PaymentReceiptRepository extends JpaRepository<PaymentReceipt, Long> {

    List<PaymentReceipt> findAllByOrderByCreatedAtDescIdDesc();

    Optional<PaymentReceipt> findByReceiptNumber(String receiptNumber);

    List<PaymentReceipt> findByPayerTypeAndPayerIdOrderByCreatedAtDesc(String payerType, Long payerId);

    List<PaymentReceipt> findByPaymentDateBetween(LocalDate start, LocalDate end);
}

