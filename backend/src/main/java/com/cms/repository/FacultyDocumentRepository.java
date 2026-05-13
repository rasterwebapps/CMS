package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.FacultyDocument;
import com.cms.model.enums.DocumentVerificationStatus;

public interface FacultyDocumentRepository extends JpaRepository<FacultyDocument, Long> {

    List<FacultyDocument> findByFacultyId(Long facultyId);

    List<FacultyDocument> findByStatus(DocumentVerificationStatus status);
}
