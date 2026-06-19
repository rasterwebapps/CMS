package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.model.CommissionPayout;

public interface CommissionPayoutRepository extends JpaRepository<CommissionPayout, Long> {

    List<CommissionPayout> findByEnquiryIdOrderByPayoutDateDesc(Long enquiryId);

    boolean existsByAgentId(Long agentId);

    boolean existsByStaffReferrerId(Long staffReferrerId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM CommissionPayout p WHERE p.enquiry.id = :enquiryId")
    java.math.BigDecimal sumAmountByEnquiryId(@Param("enquiryId") Long enquiryId);
}
