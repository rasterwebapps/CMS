package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.model.EnquiryDocumentHistory;

@Repository
public interface EnquiryDocumentHistoryRepository extends JpaRepository<EnquiryDocumentHistory, Long> {

    List<EnquiryDocumentHistory> findByEnquiryDocumentIdOrderByChangedAtDesc(Long enquiryDocumentId);

    List<EnquiryDocumentHistory> findByEnquiryIdOrderByChangedAtDesc(Long enquiryId);

    List<EnquiryDocumentHistory> findByAdmissionIdOrderByChangedAtDesc(Long admissionId);
}
