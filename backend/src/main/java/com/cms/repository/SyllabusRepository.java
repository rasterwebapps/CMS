package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.Syllabus;

public interface SyllabusRepository extends JpaRepository<Syllabus, Long> {

    List<Syllabus> findBySubjectId(Long subjectId);

    Optional<Syllabus> findBySubjectIdAndIsActiveTrue(Long subjectId);

    Optional<Syllabus> findBySubjectIdAndVersion(Long subjectId, Integer version);

    boolean existsBySubjectIdAndVersion(Long subjectId, Integer version);

    List<Syllabus> findByIsActiveTrue();
}
