package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.EnquiryDocument;

public interface EnquiryDocumentRepository extends JpaRepository<EnquiryDocument, Long> {

    List<EnquiryDocument> findByEnquiryId(Long enquiryId);
}
