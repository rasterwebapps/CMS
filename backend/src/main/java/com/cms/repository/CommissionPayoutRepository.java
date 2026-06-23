package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.CommissionPayout;

public interface CommissionPayoutRepository extends JpaRepository<CommissionPayout, Long> {

    List<CommissionPayout> findByEnquiryIdOrderByPayoutDateDesc(Long enquiryId);

    boolean existsByAgentId(Long agentId);

    boolean existsByStaffReferrerId(Long staffReferrerId);
}
