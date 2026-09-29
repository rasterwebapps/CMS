package com.cms.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.cms.model.FacultyAbsence;

public interface FacultyAbsenceRepository extends JpaRepository<FacultyAbsence, Long>,
    JpaSpecificationExecutor<FacultyAbsence> {

    Optional<FacultyAbsence> findByFacultyIdAndAbsenceDate(Long facultyId, LocalDate absenceDate);

    boolean existsByFacultyIdAndAbsenceDate(Long facultyId, LocalDate absenceDate);
}
