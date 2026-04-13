package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.Examination;

public interface ExaminationRepository extends JpaRepository<Examination, Long> {
    List<Examination> findByCourseId(Long courseId);
    List<Examination> findBySemesterId(Long semesterId);
}
