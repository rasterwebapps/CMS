package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.TermInstance;
import com.cms.model.enums.TermInstanceStatus;
import com.cms.model.enums.TermType;

public interface TermInstanceRepository extends JpaRepository<TermInstance, Long> {

    List<TermInstance> findByAcademicYearId(Long academicYearId);

    Optional<TermInstance> findByAcademicYearIdAndTermType(Long academicYearId, TermType termType);

    List<TermInstance> findByStatus(TermInstanceStatus status);

    boolean existsByAcademicYearId(Long academicYearId);
}
