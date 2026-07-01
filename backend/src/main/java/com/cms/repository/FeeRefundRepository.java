package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.cms.model.FeeRefund;

public interface FeeRefundRepository extends JpaRepository<FeeRefund, Long>, JpaSpecificationExecutor<FeeRefund> {

    List<FeeRefund> findByStatusOrderByRequestedAtDescIdDesc(String status);

    List<FeeRefund> findByStatusOrderByCreatedAtDescIdDesc(String status);

    /** True if a PENDING or APPROVED request already exists for the receipt (excludes REJECTED). */
    boolean existsByOriginalReceiptNumberAndStatusNot(String originalReceiptNumber, String status);

    /** Used by unified receipts to mark payment rows with active refund workflow state. */
    List<FeeRefund> findByStatusIn(List<String> statuses);

    /** Approved refund vouchers shown in student fee payment history. */
    List<FeeRefund> findByStudentIdAndStatusOrderByPaymentDateDescIdDesc(Long studentId, String status);

    /** Approved refund vouchers shown in enquiry payment history. */
    List<FeeRefund> findByEnquiryIdAndStatusOrderByPaymentDateDescIdDesc(Long enquiryId, String status);

    /** All refunds — PENDING first, then by requestedAt desc. */
    @Query("SELECT r FROM FeeRefund r ORDER BY CASE r.status WHEN 'PENDING' THEN 0 ELSE 1 END ASC, r.requestedAt DESC, r.id DESC")
    List<FeeRefund> findAllOrderedByStatusAndDate();
}
