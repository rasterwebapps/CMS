package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.model.FacultyDocumentHistory;

@Repository
public interface FacultyDocumentHistoryRepository extends JpaRepository<FacultyDocumentHistory, Long> {

    List<FacultyDocumentHistory> findByFacultyDocumentIdOrderByChangedAtDesc(Long facultyDocumentId);

    List<FacultyDocumentHistory> findByFacultyIdOrderByChangedAtDesc(Long facultyId);
}
