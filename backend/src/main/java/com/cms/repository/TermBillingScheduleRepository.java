package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.model.TermBillingSchedule;
import com.cms.model.enums.TermType;

public interface TermBillingScheduleRepository extends JpaRepository<TermBillingSchedule, Long> {

    List<TermBillingSchedule> findByAcademicYearId(Long academicYearId);

    Optional<TermBillingSchedule> findByAcademicYearIdAndTermType(Long academicYearId, TermType termType);

    boolean existsByAcademicYearId(Long academicYearId);
}
