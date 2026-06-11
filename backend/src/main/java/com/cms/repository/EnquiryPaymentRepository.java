package com.cms.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.EnquiryPayment;

public interface EnquiryPaymentRepository extends JpaRepository<EnquiryPayment, Long> {

    List<EnquiryPayment> findByEnquiryIdOrderByPaymentDateDesc(Long enquiryId);

    @Query("SELECT COALESCE(SUM(p.amountPaid), 0) FROM EnquiryPayment p WHERE p.enquiry.id = :enquiryId AND p.refundedAt IS NULL")
    BigDecimal sumAmountPaidByEnquiryId(@Param("enquiryId") Long enquiryId);

    @Query("SELECT p.enquiry.id, COALESCE(SUM(p.amountPaid), 0) FROM EnquiryPayment p WHERE p.enquiry.id IN :ids AND p.refundedAt IS NULL GROUP BY p.enquiry.id")
    List<Object[]> sumAmountPaidGroupedByEnquiryIds(@Param("ids") List<Long> ids);

    default Map<Long, BigDecimal> paidTotalsForIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        return sumAmountPaidGroupedByEnquiryIds(ids).stream()
            .collect(Collectors.toMap(r -> (Long) r[0], r -> (BigDecimal) r[1]));
    }

    List<EnquiryPayment> findByPaymentDate(LocalDate paymentDate);

    Optional<EnquiryPayment> findByReceiptNumber(String receiptNumber);
}
