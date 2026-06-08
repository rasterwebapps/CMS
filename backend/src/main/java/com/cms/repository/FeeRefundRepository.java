package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.cms.model.FeeRefund;

public interface FeeRefundRepository extends JpaRepository<FeeRefund, Long> {

    List<FeeRefund> findByStatusOrderByRequestedAtDescIdDesc(String status);

    List<FeeRefund> findByStatusOrderByCreatedAtDescIdDesc(String status);

    /** True if a PENDING or APPROVED request already exists for the receipt (excludes REJECTED). */
    boolean existsByOriginalReceiptNumberAndStatusNot(String originalReceiptNumber, String status);

    /** All refunds — PENDING first, then by requestedAt desc. */
    @Query("SELECT r FROM FeeRefund r ORDER BY CASE r.status WHEN 'PENDING' THEN 0 ELSE 1 END ASC, r.requestedAt DESC, r.id DESC")
    List<FeeRefund> findAllOrderedByStatusAndDate();
}
