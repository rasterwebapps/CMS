package com.cms.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.ComplianceDocument;

public interface ComplianceDocumentRepository extends JpaRepository<ComplianceDocument, Long> {
}

