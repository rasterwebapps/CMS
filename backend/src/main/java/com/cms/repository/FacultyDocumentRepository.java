package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.FacultyDocument;

public interface FacultyDocumentRepository extends JpaRepository<FacultyDocument, Long> {

    List<FacultyDocument> findByFacultyId(Long facultyId);
}
