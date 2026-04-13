package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.Syllabus;

public interface SyllabusRepository extends JpaRepository<Syllabus, Long> {

    List<Syllabus> findByCourseId(Long courseId);

    Optional<Syllabus> findByCourseIdAndIsActiveTrue(Long courseId);

    Optional<Syllabus> findByCourseIdAndVersion(Long courseId, Integer version);

    boolean existsByCourseIdAndVersion(Long courseId, Integer version);

    List<Syllabus> findByIsActiveTrue();
}
