package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.ExamResult;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {
    List<ExamResult> findByExaminationId(Long examinationId);
    List<ExamResult> findByStudentId(Long studentId);
}
