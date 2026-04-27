package com.cms.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.TermFeePayment;

public interface TermFeePaymentRepository extends JpaRepository<TermFeePayment, Long> {

    List<TermFeePayment> findByFeeDemandId(Long demandId);

    Optional<TermFeePayment> findByReceiptNumber(String receiptNumber);

    List<TermFeePayment> findByPaymentDateBetween(LocalDate from, LocalDate to);

    long countByPaymentDate(LocalDate date);
}
