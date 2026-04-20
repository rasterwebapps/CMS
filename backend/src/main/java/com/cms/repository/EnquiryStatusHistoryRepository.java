package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.EnquiryStatusHistory;

public interface EnquiryStatusHistoryRepository extends JpaRepository<EnquiryStatusHistory, Long> {

    List<EnquiryStatusHistory> findByEnquiryIdOrderByChangedAtAsc(Long enquiryId);
}
