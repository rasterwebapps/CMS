package com.cms.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.EnquiryPayment;

public interface EnquiryPaymentRepository extends JpaRepository<EnquiryPayment, Long> {

    List<EnquiryPayment> findByEnquiryIdOrderByPaymentDateDesc(Long enquiryId);

    @Query("SELECT COALESCE(SUM(p.amountPaid), 0) FROM EnquiryPayment p WHERE p.enquiry.id = :enquiryId")
    BigDecimal sumAmountPaidByEnquiryId(@Param("enquiryId") Long enquiryId);

    List<EnquiryPayment> findByPaymentDate(LocalDate paymentDate);
}
