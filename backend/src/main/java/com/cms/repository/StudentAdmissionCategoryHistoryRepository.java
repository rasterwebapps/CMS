package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.StudentAdmissionCategoryHistory;

public interface StudentAdmissionCategoryHistoryRepository
        extends JpaRepository<StudentAdmissionCategoryHistory, Long> {

    List<StudentAdmissionCategoryHistory> findByStudentIdOrderByChangedAtDesc(Long studentId);
}
